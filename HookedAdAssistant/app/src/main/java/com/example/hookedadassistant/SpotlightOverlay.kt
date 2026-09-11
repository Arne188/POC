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

class SpotlightOverlay(private val service: AccessibilityService) {
    private var view: SpotlightView? = null
    private val wm = service.getSystemService(AccessibilityService.WINDOW_SERVICE) as WindowManager

    fun show(target: Rect, label: String) {
        hide()
        val v = SpotlightView(service, Rect(target), label)
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            android.graphics.PixelFormat.TRANSLUCENT
        )
        wm.addView(v, lp)
        view = v
        v.postDelayed({ if (view === v) hide() }, 6500)
    }

    fun hide() {
        view?.let { runCatching { wm.removeView(it) } }
        view = null
    }

    private class SpotlightView(
        service: AccessibilityService,
        private val target: Rect,
        private val label: String
    ) : View(service) {
        private val dim = Paint().apply { color = 0x99000000.toInt(); style = Paint.Style.FILL }
        private val border = Paint().apply { color = Color.YELLOW; style = Paint.Style.STROKE; strokeWidth = 8f }
        private val text = Paint().apply { color = Color.YELLOW; textSize = 42f; isFakeBoldText = true }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val pad = 18f
            val hole = RectF(target.left - pad, target.top - pad, target.right + pad, target.bottom + pad)
            canvas.save()
            canvas.clipRect(0f, 0f, width.toFloat(), height.toFloat())
            canvas.clipRect(hole, Region.Op.DIFFERENCE)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dim)
            canvas.restore()
            canvas.drawRoundRect(hole, 22f, 22f, border)
            val tx = hole.left.coerceAtLeast(18f)
            val ty = if (hole.bottom + 58f < height) hole.bottom + 52f else (hole.top - 18f).coerceAtLeast(50f)
            canvas.drawText(label, tx, ty, text)
        }
    }
}
