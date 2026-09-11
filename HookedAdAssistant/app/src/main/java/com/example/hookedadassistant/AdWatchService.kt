package com.example.hookedadassistant
import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale
import java.util.concurrent.Executors

class AdWatchService : AccessibilityService() {
    private val handler=Handler(Looper.getMainLooper())
    private val exec=Executors.newSingleThreadExecutor()
    private var foregroundPackage=""
    private var previousPackage=""
    private var lastAlertAt=0L
    private var possibleAd=false
    private var adStart=0L
    private var xHits=0
    private val keywords=listOf("close","skip","continue","next","done","finish","schließen","überspringen","weiter","fertig","reward","belohnung")

    override fun onServiceConnected() { super.onServiceConnected(); handler.post(screenLoop) }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg=event?.packageName?.toString().orEmpty()
        if(pkg.isNotBlank()) updatePackage(pkg)
        if(Prefs.tree(this)) {
            val root=rootInActiveWindow
            if(root!=null && containsActionElement(root)) alert("Bedienelement der Werbung erkannt")
        }
    }

    private fun updatePackage(pkg:String){
        previousPackage=foregroundPackage; foregroundPackage=pkg
        if(previousPackage==Prefs.GAME_PACKAGE && pkg!=Prefs.GAME_PACKAGE && pkg!=packageName){ startAd() }
        if(possibleAd && pkg==Prefs.GAME_PACKAGE && previousPackage!=Prefs.GAME_PACKAGE){ finishAd() }
    }
    private fun startAd(){
        if(possibleAd) return
        possibleAd=true; adStart=System.currentTimeMillis(); StatsRepository(this).startAd(adStart)
        if(Prefs.timer(this)) {
            handler.postDelayed({ if(possibleAd) alert("35 s erreicht – Werbung könnte eine Eingabe erwarten") },35000)
            handler.postDelayed({ if(possibleAd) alert("60 s erreicht – bitte Werbung prüfen") },60000)
        }
    }
    private fun finishAd(){ possibleAd=false; adStart=0L; xHits=0; StatsRepository(this).finishAd(System.currentTimeMillis()) }

    private val screenLoop=object:Runnable{
        override fun run(){
            if(Prefs.screen(this@AdWatchService) && foregroundPackage!=packageName){ captureAndAnalyze() }
            handler.postDelayed(this,2500)
        }
    }
    private fun captureAndAnalyze(){
        takeScreenshot(Display.DEFAULT_DISPLAY,exec,object:TakeScreenshotCallback{
            override fun onSuccess(result: ScreenshotResult) {
                val hb=result.hardwareBuffer
                try {
                    val hw=Bitmap.wrapHardwareBuffer(hb,result.colorSpace) ?: return
                    val b=hw.copy(Bitmap.Config.ARGB_8888,false)
                    val hit=XDetector.likelyCloseX(b)
                    b.recycle()
                    xHits=if(hit) xHits+1 else 0
                    if(xHits>=2){
                        if(!possibleAd) startAd()
                        alert("Mögliches X/Schließen-Symbol erkannt")
                        xHits=0
                    }
                } finally { hb.close() }
            }
            override fun onFailure(errorCode:Int){ }
        })
    }
    private fun containsActionElement(node:AccessibilityNodeInfo?):Boolean{
        if(node==null) return false
        val text=buildString{
            node.text?.let{append(it).append(' ')}
            node.contentDescription?.let{append(it).append(' ')}
            node.viewIdResourceName?.let{append(it)}
        }.lowercase(Locale.getDefault())
        if(keywords.any{text.contains(it)}) return true
        for(i in 0 until node.childCount) if(containsActionElement(node.getChild(i))) return true
        return false
    }
    private fun alert(msg:String){
        val now=System.currentTimeMillis(); if(now-lastAlertAt<7000) return
        lastAlertAt=now; Notifier.notifyActionNeeded(this,msg)
    }
    override fun onInterrupt()=Unit
    override fun onDestroy(){ handler.removeCallbacksAndMessages(null); exec.shutdownNow(); super.onDestroy() }
}
