package com.example.hookedadassistant

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale
import java.util.concurrent.Executors

class AdWatchService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private val exec = Executors.newSingleThreadExecutor()
    private lateinit var spotlight: SpotlightOverlay
    private var foregroundPackage = ""
    private var previousPackage = ""
    private var lastAlertAt = 0L
    private var possibleAd = false
    private var xHits = 0

    private val keywords = listOf("close", "skip", "continue", "next", "done", "finish", "schließen", "überspringen", "weiter", "fertig", "claim", "collect")
    private val adTokens = listOf("rewarded", "interstitial", "advert", "unityads", "applovin", "ironsource", "vungle", "mintegral", "admob", "googleads", "facebookads")

    override fun onServiceConnected() {
        super.onServiceConnected()
        spotlight = SpotlightOverlay(this)
        handler.post(screenLoop)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!Prefs.assistantOn(this)) { if (::spotlight.isInitialized) spotlight.hide(); return }
        event ?: return
        val pkg = event.packageName?.toString().orEmpty()
        if (pkg.isNotBlank()) updatePackage(pkg)
        val eventClass = event.className?.toString().orEmpty().lowercase(Locale.getDefault())
        val eventText = event.text?.joinToString(" ").orEmpty().lowercase(Locale.getDefault())
        if (looksAdRelated(pkg.lowercase(Locale.getDefault())) || looksAdRelated(eventClass) || looksAdRelated(eventText)) startAd()
        if (!Prefs.tree(this)) return
        val root = rootInActiveWindow ?: return
        val action = findActionElement(root)
        when {
            action != null -> {
                startAd()
                if (Prefs.overlay(this)) showNode(action, "Hier tippen")
                alert("Werbe-Bedienelement erkannt")
            }
            possibleAd -> {
                val close = findLikelyCloseButton(root)
                if (close != null) {
                    if (Prefs.overlay(this)) showNode(close, "Schliessen / Weiter")
                    alert("Mögliches Schließen-/Weiter-Element erkannt")
                }
            }
            treeLooksAdRelated(root) -> startAd()
        }
    }

    private fun showNode(node: AccessibilityNodeInfo, label: String) {
        val r = Rect(); node.getBoundsInScreen(r)
        if (!r.isEmpty && ::spotlight.isInitialized) spotlight.show(r, label)
    }

    private fun looksAdRelated(s: String) = adTokens.any { s.contains(it) }

    private fun updatePackage(pkg: String) {
        if (pkg == foregroundPackage) return
        previousPackage = foregroundPackage
        foregroundPackage = pkg
        if (previousPackage == Prefs.GAME_PACKAGE && pkg != Prefs.GAME_PACKAGE && pkg != packageName) startAd()
        if (possibleAd && pkg == Prefs.GAME_PACKAGE && previousPackage.isNotBlank() && previousPackage != Prefs.GAME_PACKAGE) finishAd()
    }

    private fun startAd() {
        if (!Prefs.assistantOn(this) || possibleAd) return
        possibleAd = true
        StatsRepository(this).startAd(System.currentTimeMillis())
        if (Prefs.timer(this)) {
            handler.postDelayed({ if (Prefs.assistantOn(this) && possibleAd) alert("35 s erreicht – bitte Werbung prüfen") }, 35_000)
            handler.postDelayed({ if (Prefs.assistantOn(this) && possibleAd) alert("55 s erreicht – Schließen/Weiter könnte verfügbar sein") }, 55_000)
        }
    }

    private fun finishAd() {
        possibleAd = false; xHits = 0
        if (::spotlight.isInitialized) spotlight.hide()
        StatsRepository(this).finishAd(System.currentTimeMillis())
    }

    private val screenLoop = object : Runnable {
        override fun run() {
            if (Prefs.assistantOn(this@AdWatchService) && Prefs.screen(this@AdWatchService) && foregroundPackage != packageName) captureAndAnalyze()
            handler.postDelayed(this, 1800)
        }
    }

    private fun captureAndAnalyze() {
        takeScreenshot(Display.DEFAULT_DISPLAY, exec, object : TakeScreenshotCallback {
            override fun onSuccess(result: ScreenshotResult) {
                if (!Prefs.assistantOn(this@AdWatchService)) { result.hardwareBuffer.close(); return }
                val hb = result.hardwareBuffer
                try {
                    val hw = Bitmap.wrapHardwareBuffer(hb, result.colorSpace) ?: return
                    val b = hw.copy(Bitmap.Config.ARGB_8888, false)
                    val hit = XDetector.likelyCloseControl(b)
                    b.recycle()
                    xHits = if (hit) xHits + 1 else 0
                    val neededHits = if (possibleAd) 2 else 3
                    if (xHits >= neededHits) {
                        startAd()
                        alert("Mögliches X/Next erkannt – oben links/rechts prüfen")
                        xHits = 0
                    }
                } finally { hb.close() }
            }
            override fun onFailure(errorCode: Int) = Unit
        })
    }

    private fun nodeLabel(node: AccessibilityNodeInfo) = buildString {
        node.text?.let { append(it).append(' ') }
        node.contentDescription?.let { append(it).append(' ') }
        node.viewIdResourceName?.let { append(it) }
    }.lowercase(Locale.getDefault()).trim()

    private fun findActionElement(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        val text = nodeLabel(node)
        if ((node.isClickable || node.isFocusable) && keywords.any { text.contains(it) }) return node
        if (node.isClickable && text in listOf("x", "×", "✕", "✖", ">", "›", "»")) return node
        for (i in 0 until node.childCount) findActionElement(node.getChild(i))?.let { return it }
        return null
    }

    private fun treeLooksAdRelated(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        val text = nodeLabel(node) + " " + (node.className ?: "").toString().lowercase(Locale.getDefault())
        if (looksAdRelated(text)) return true
        for (i in 0 until node.childCount) if (treeLooksAdRelated(node.getChild(i))) return true
        return false
    }

    private fun findLikelyCloseButton(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isClickable) {
            val r = Rect(); node.getBoundsInScreen(r)
            val w = resources.displayMetrics.widthPixels; val h = resources.displayMetrics.heightPixels
            if (w > 0 && h > 0 && r.width() in 18..(w * 0.18).toInt() && r.height() in 18..(h * 0.12).toInt()) {
                val nearTop = r.centerY() < h * 0.24
                val nearSide = r.centerX() < w * 0.22 || r.centerX() > w * 0.78
                if (nearTop && nearSide) return node
            }
        }
        for (i in 0 until node.childCount) findLikelyCloseButton(node.getChild(i))?.let { return it }
        return null
    }

    private fun alert(msg: String) {
        if (!Prefs.assistantOn(this)) return
        val now = System.currentTimeMillis()
        if (now - lastAlertAt < 25_000) return
        lastAlertAt = now
        Notifier.notifyActionNeeded(this, msg)
    }

    override fun onInterrupt() = Unit
    override fun onDestroy() {
        if (::spotlight.isInitialized) spotlight.hide()
        handler.removeCallbacksAndMessages(null); exec.shutdownNow(); super.onDestroy()
    }
}
