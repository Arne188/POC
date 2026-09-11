package com.example.hookedadassistant

import android.accessibilityservice.AccessibilityService
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Region
import android.view.View
import android.view.WindowManager
import kotlin.math.abs

class SpotlightOverlay(private val service: AccessibilityService) {
    private var view: SpotlightView? = null
    private var currentTarget: Rect? = null
    private var currentLabel: String? = null
    private val wm = service.getSystemService(AccessibilityService.WINDOW_SERVICE) as WindowManager

    fun show(target: Rect, label: String) {
        val expanded=Rect(target).apply{inset(-14,-14)}
        val old=currentTarget
        if(view!=null&&old!=null&&abs(old.centerX()-expanded.centerX())<70&&abs(old.centerY()-expanded.centerY())<70&&currentLabel==label)return
        hide(); currentTarget=expanded; currentLabel=label
        val v=SpotlightView(service,expanded,label)
        val lp=WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,android.graphics.PixelFormat.TRANSLUCENT)
        wm.addView(v,lp); view=v
        v.postDelayed({if(view===v)hide()},5000)
    }

    fun hide(){view?.let{runCatching{wm.removeView(it)}};view=null;currentTarget=null;currentLabel=null}

    private class SpotlightView(service:AccessibilityService,private val target:Rect,private val label:String):View(service){
        private val dim=Paint().apply{color=0x66000000;style=Paint.Style.FILL}
        private val outer=Paint().apply{color=Color.BLACK;style=Paint.Style.STROKE;strokeWidth=13f}
        private val border=Paint().apply{color=Color.YELLOW;style=Paint.Style.STROKE;strokeWidth=7f}
        private val marker=Paint().apply{color=Color.YELLOW;style=Paint.Style.FILL}
        private val textBg=Paint().apply{color=0xDD000000.toInt();style=Paint.Style.FILL}
        private val text=Paint().apply{color=Color.YELLOW;textSize=38f;isFakeBoldText=true}
        override fun onDraw(canvas:Canvas){
            super.onDraw(canvas);val hole=RectF(target)
            canvas.save();canvas.clipRect(0f,0f,width.toFloat(),height.toFloat());canvas.clipRect(hole,Region.Op.DIFFERENCE);canvas.drawRect(0f,0f,width.toFloat(),height.toFloat(),dim);canvas.restore()
            canvas.drawRoundRect(hole,20f,20f,outer);canvas.drawRoundRect(hole,20f,20f,border);canvas.drawCircle(hole.centerX(),hole.centerY(),9f,marker)
            val labelWidth=text.measureText(label)+34f;val left=hole.left.coerceAtLeast(12f).coerceAtMost((width-labelWidth-12f).coerceAtLeast(12f));val top=if(hole.bottom+62f<height)hole.bottom+12f else (hole.top-58f).coerceAtLeast(8f);val box=RectF(left,top,left+labelWidth,top+50f)
            canvas.drawRoundRect(box,12f,12f,textBg);canvas.drawText(label,left+17f,top+38f,text)
        }
    }
}
