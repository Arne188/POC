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
import kotlin.math.abs

class AdWatchService : AccessibilityService() {
    private val handler=Handler(Looper.getMainLooper()); private val exec=Executors.newSingleThreadExecutor()
    private lateinit var spotlight: SpotlightOverlay
    private var foregroundPackage=""; private var previousPackage=""; private var lastAlertAt=0L; private var possibleAd=false
    private var imageHits=0; private var imageMisses=0; private var screenLatched=false; private var lastImageDetection:XDetector.Detection?=null
    private val keywords=listOf("close","skip","skip ad","continue","next","done","finish","schließen","überspringen","weiter","fertig","claim","collect")
    private val ctaKeywords=listOf("google play","play now","install","download","herunterladen","get it on","learn more","open","get")
    private val adTokens=listOf("rewarded","interstitial","advert","unityads","applovin","ironsource","vungle","mintegral","admob","googleads","facebookads")

    override fun onServiceConnected(){ super.onServiceConnected(); spotlight=SpotlightOverlay(this); handler.post(screenLoop) }
    override fun onAccessibilityEvent(event:AccessibilityEvent?){
        if(!Prefs.assistantOn(this)){ if(::spotlight.isInitialized) spotlight.hide(); return }; event?:return
        val pkg=event.packageName?.toString().orEmpty(); if(pkg.isNotBlank()) updatePackage(pkg)
        val cls=event.className?.toString().orEmpty().lowercase(Locale.getDefault()); val txt=event.text?.joinToString(" ").orEmpty().lowercase(Locale.getDefault())
        if(looksAdRelated(pkg.lowercase(Locale.getDefault()))||looksAdRelated(cls)||looksAdRelated(txt)) startAd()
        if(!Prefs.tree(this)) return
        val root=rootInActiveWindow?:return; val action=findActionElement(root)
        if(action!=null){ startAd(); showNode(action,"HIER TIPPEN"); alert("Werbe-Bedienelement erkannt"); return }
        if(possibleAd){ val close=findLikelyCloseButton(root); if(close!=null){ showNode(close,"HIER TIPPEN"); alert("Mögliches Schließen-/Weiter-Element erkannt"); return } }
        if(treeLooksAdRelated(root)) startAd()
    }
    private fun showNode(n:AccessibilityNodeInfo,label:String){ if(!Prefs.overlay(this))return; val r=Rect(); n.getBoundsInScreen(r); if(!r.isEmpty&&::spotlight.isInitialized) spotlight.show(r,label) }
    private fun looksAdRelated(s:String)=adTokens.any{s.contains(it)}
    private fun updatePackage(pkg:String){ if(pkg==foregroundPackage)return; previousPackage=foregroundPackage; foregroundPackage=pkg; if(previousPackage==Prefs.GAME_PACKAGE&&pkg!=Prefs.GAME_PACKAGE&&pkg!=packageName)startAd(); if(possibleAd&&pkg==Prefs.GAME_PACKAGE&&previousPackage.isNotBlank()&&previousPackage!=Prefs.GAME_PACKAGE)finishAd() }
    private fun startAd(){ if(!Prefs.assistantOn(this)||possibleAd)return; possibleAd=true; StatsRepository(this).startAd(System.currentTimeMillis()); if(Prefs.timer(this)){ handler.postDelayed({if(Prefs.assistantOn(this)&&possibleAd)alert("35 s erreicht – bitte Werbung prüfen")},35000); handler.postDelayed({if(Prefs.assistantOn(this)&&possibleAd)alert("55 s erreicht – Schließen/Weiter könnte verfügbar sein")},55000) } }
    private fun finishAd(){ possibleAd=false; imageHits=0; imageMisses=0; screenLatched=false; lastImageDetection=null; if(::spotlight.isInitialized)spotlight.hide(); StatsRepository(this).finishAd(System.currentTimeMillis()) }
    private val screenLoop=object:Runnable{ override fun run(){ if(Prefs.assistantOn(this@AdWatchService)&&Prefs.screen(this@AdWatchService)&&foregroundPackage!=packageName)captureAndAnalyze(); handler.postDelayed(this,1800) } }
    private fun captureAndAnalyze(){ takeScreenshot(Display.DEFAULT_DISPLAY,exec,object:TakeScreenshotCallback{
        override fun onSuccess(result:ScreenshotResult){ if(!Prefs.assistantOn(this@AdWatchService)){result.hardwareBuffer.close();return}; val hb=result.hardwareBuffer
            try{ val hw=Bitmap.wrapHardwareBuffer(hb,result.colorSpace)?:return; val b=hw.copy(Bitmap.Config.ARGB_8888,false); val d=XDetector.findControl(b); b.recycle()
                if(d!=null){
                    imageMisses=0
                    val same=lastImageDetection?.let{it.kind==d.kind&&abs(it.rect.centerX()-d.rect.centerX())<90&&abs(it.rect.centerY()-d.rect.centerY())<90}?:false
                    imageHits=if(same) imageHits+1 else 1
                    if(!same) screenLatched=false
                    lastImageDetection=d
                    val needed=if(possibleAd)2 else 3
                    if(imageHits>=needed&&!screenLatched){ screenLatched=true; startAd(); handler.post{ if(Prefs.overlay(this@AdWatchService)&&::spotlight.isInitialized)spotlight.show(d.rect,"HIER TIPPEN"); alert("${d.kind} erkannt") } }
                }else{
                    imageHits=0; imageMisses++
                    if(imageMisses>=3){ screenLatched=false; lastImageDetection=null; handler.post{if(::spotlight.isInitialized)spotlight.hide()} }
                }
            }finally{hb.close()} }
        override fun onFailure(errorCode:Int)=Unit }) }
    private fun nodeLabel(n:AccessibilityNodeInfo)=buildString{n.text?.let{append(it).append(' ')};n.contentDescription?.let{append(it).append(' ')};n.viewIdResourceName?.let{append(it)}}.lowercase(Locale.getDefault()).trim()
    private fun findActionElement(n:AccessibilityNodeInfo?):AccessibilityNodeInfo?{ if(n==null)return null; val t=nodeLabel(n); val interactive=n.isClickable||n.isFocusable; if(interactive&&keywords.any{t.contains(it)})return n; if(interactive&&ctaKeywords.any{t.contains(it)})return n; if(n.isClickable&&t in listOf("x","×","✕","✖",">","›","»",">>","»»"))return n; for(i in 0 until n.childCount)findActionElement(n.getChild(i))?.let{return it}; return null }
    private fun treeLooksAdRelated(n:AccessibilityNodeInfo?):Boolean{if(n==null)return false;val t=nodeLabel(n)+" "+(n.className?:"").toString().lowercase(Locale.getDefault());if(looksAdRelated(t))return true;for(i in 0 until n.childCount)if(treeLooksAdRelated(n.getChild(i)))return true;return false}
    private fun findLikelyCloseButton(n:AccessibilityNodeInfo?):AccessibilityNodeInfo?{if(n==null)return null;if(n.isClickable){val r=Rect();n.getBoundsInScreen(r);val w=resources.displayMetrics.widthPixels;val h=resources.displayMetrics.heightPixels;if(w>0&&h>0&&r.width() in 18..(w*.20).toInt()&&r.height() in 18..(h*.14).toInt()){val top=r.centerY()<h*.28;val side=r.centerX()<w*.25||r.centerX()>w*.75;if(top&&side)return n}};for(i in 0 until n.childCount)findLikelyCloseButton(n.getChild(i))?.let{return it};return null}
    private fun alert(msg:String){if(!Prefs.assistantOn(this))return;val now=System.currentTimeMillis();if(now-lastAlertAt<25000)return;lastAlertAt=now;Notifier.notifyActionNeeded(this,msg)}
    override fun onInterrupt()=Unit
    override fun onDestroy(){if(::spotlight.isInitialized)spotlight.hide();handler.removeCallbacksAndMessages(null);exec.shutdownNow();super.onDestroy()}
}
