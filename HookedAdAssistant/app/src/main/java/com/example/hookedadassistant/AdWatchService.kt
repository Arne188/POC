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
    private var foregroundPackage = ""
    private var previousPackage = ""
    private var lastAlertAt = 0L
    private var possibleAd = false
    private var adStart = 0L
    private var xHits = 0

    private val keywords = listOf("close", "skip", "continue", "next", "done", "finish", "schließen", "überspringen", "weiter", "fertig", "claim", "collect")
    private val adTokens = listOf("rewarded", "interstitial", "advert", "unityads", "applovin", "ironsource", "vungle", "mintegral", "admob", "googleads", "facebookads")

    override fun onServiceConnected() { super.onServiceConnected(); handler.post(screenLoop) }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!Prefs.assistantOn(this)) return
        event ?: return
        val pkg = event.packageName?.toString().orEmpty()
        if (pkg.isNotBlank()) updatePackage(pkg)
        val eventClass = event.className?.toString().orEmpty().lowercase(Locale.getDefault())
        val eventText = event.text?.joinToString(" ").orEmpty().lowercase(Locale.getDefault())
        if (looksAdRelated(pkg.lowercase(Locale.getDefault())) || looksAdRelated(eventClass) || looksAdRelated(eventText)) startAd()
        if (!Prefs.tree(this)) return
        val root = rootInActiveWindow ?: return
        when {
            containsActionElement(root) -> { startAd(); alert("Werbe-Bedienelement erkannt") }
            possibleAd && containsLikelyCloseButton(root) -> alert("Mögliches Schließen-/Weiter-Element erkannt")
            treeLooksAdRelated(root) -> startAd()
        }
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
        adStart = System.currentTimeMillis()
        StatsRepository(this).startAd(adStart)
        if (Prefs.timer(this)) {
            handler.postDelayed({ if (Prefs.assistantOn(this) && possibleAd) alert("35 s erreicht – bitte Werbung prüfen") }, 35_000)
            handler.postDelayed({ if (Prefs.assistantOn(this) && possibleAd) alert("55 s erreicht – Schließen/Weiter könnte verfügbar sein") }, 55_000)
        }
    }

    private fun finishAd() { possibleAd = false; adStart = 0L; xHits = 0; StatsRepository(this).finishAd(System.currentTimeMillis()) }

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
                    if (xHits >= neededHits) { startAd(); alert("Mögliches X/Schließen-Symbol erkannt"); xHits = 0 }
                } finally { hb.close() }
            }
            override fun onFailure(errorCode: Int) = Unit
        })
    }

    private fun nodeLabel(node: AccessibilityNodeInfo) = buildString {
        node.text?.let { append(it).append(' ') }; node.contentDescription?.let { append(it).append(' ') }; node.viewIdResourceName?.let { append(it) }
    }.lowercase(Locale.getDefault()).trim()

    private fun containsActionElement(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        val text = nodeLabel(node)
        if (keywords.any { text.contains(it) }) return true
        if (node.isClickable && text in listOf("x", "×", "✕", "✖", ">", "›", "»")) return true
        for (i in 0 until node.childCount) if (containsActionElement(node.getChild(i))) return true
        return false
    }

    private fun treeLooksAdRelated(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        val text = nodeLabel(node) + " " + (node.className ?: "").toString().lowercase(Locale.getDefault())
        if (looksAdRelated(text)) return true
        for (i in 0 until node.childCount) if (treeLooksAdRelated(node.getChild(i))) return true
        return false
    }

    private fun containsLikelyCloseButton(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        if (node.isClickable) {
            val r = Rect(); node.getBoundsInScreen(r)
            val w = resources.displayMetrics.widthPixels; val h = resources.displayMetrics.heightPixels
            if (w > 0 && h > 0 && r.width() in 18..(w * 0.15).toInt() && r.height() in 18..(h * 0.10).toInt()) {
                val nearTop = r.centerY() < h * 0.20
                val nearSide = r.centerX() < w * 0.18 || r.centerX() > w * 0.82
                if (nearTop && nearSide && nodeLabel(node).isNotBlank()) return true
            }
        }
        for (i in 0 until node.childCount) if (containsLikelyCloseButton(node.getChild(i))) return true
        return false
    }

    private fun alert(msg: String) {
        if (!Prefs.assistantOn(this)) return
        val now = System.currentTimeMillis()
        if (now - lastAlertAt < 25_000) return
        lastAlertAt = now
        Notifier.notifyActionNeeded(this, msg)
    }

    override fun onInterrupt() = Unit
    override fun onDestroy() { handler.removeCallbacksAndMessages(null); exec.shutdownNow(); super.onDestroy() }
}
