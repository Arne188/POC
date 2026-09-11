package com.example.hookedadassistant

import android.graphics.Bitmap
import kotlin.math.abs

object XDetector {
    fun likelyCloseControl(bitmap: Bitmap): Boolean {
        val w = bitmap.width
        val h = bitmap.height
        if (w < 200 || h < 300) return false

        val top = (h * 0.01).toInt()
        val bottom = (h * 0.30).toInt()
        val leftWidth = (w * 0.34).toInt()
        val rightWidth = (w * 0.34).toInt()

        if (scanForX(bitmap, 0, top, leftWidth, bottom)) return true
        if (scanForX(bitmap, w - rightWidth, top, w, bottom)) return true
        if (likelyTopLeftNextPill(bitmap)) return true
        return false
    }

    private fun scanForX(b: Bitmap, l: Int, t: Int, r: Int, bot: Int): Boolean {
        val sizes = intArrayOf(28, 32, 36, 40, 48, 56, 64, 72)
        for (s in sizes) {
            val step = (s / 6).coerceAtLeast(5)
            var y = t
            while (y + s < bot) {
                var x = l
                while (x + s < r) {
                    if (xScore(b, x, y, s) >= 0.57) return true
                    x += step
                }
                y += step
            }
        }
        return false
    }

    private fun xScore(b: Bitmap, x0: Int, y0: Int, s: Int): Double {
        val margin = (s * 0.18).toInt().coerceAtLeast(3)
        val thickness = (s * 0.055).toInt().coerceIn(1, 4)
        val sample = (s / 14).coerceAtLeast(2)

        var mean = 0.0
        var count = 0
        for (y in y0 until y0 + s step sample) {
            for (x in x0 until x0 + s step sample) {
                mean += lum(b.getPixel(x, y))
                count++
            }
        }
        if (count == 0) return 0.0
        mean /= count

        fun c(x: Int, y: Int) = abs(lum(b.getPixel(x, y)) - mean)

        var diag = 0.0
        var diagN = 0
        var background = 0.0
        var bgN = 0

        for (i in margin until s - margin) {
            for (d in -thickness..thickness) {
                val yy = (y0 + i).coerceIn(y0, y0 + s - 1)
                val xa = (x0 + i + d).coerceIn(x0, x0 + s - 1)
                val xb = (x0 + (s - 1 - i) + d).coerceIn(x0, x0 + s - 1)
                diag += c(xa, yy) + c(xb, yy)
                diagN += 2
            }

            val q1 = x0 + s / 5
            val q2 = x0 + 4 * s / 5
            background += c(q1, y0 + i) + c(q2, y0 + i)
            bgN += 2
        }

        if (diagN == 0 || bgN == 0) return 0.0
        val d = diag / diagN
        val bg = background / bgN
        if (d < 20.0) return 0.0

        val dominance = d / (d + bg + 1.0)
        val center = c(x0 + s / 2, y0 + s / 2) / 255.0
        return (dominance + center * 0.10).coerceIn(0.0, 1.0)
    }

    private fun likelyTopLeftNextPill(b: Bitmap): Boolean {
        val w = b.width
        val h = b.height
        val x1 = (w * 0.02).toInt()
        val x2 = (w * 0.32).toInt()
        val y1 = (h * 0.03).toInt()
        val y2 = (h * 0.16).toInt()
        if (x2 <= x1 || y2 <= y1) return false

        var bright = 0
        var dark = 0
        var total = 0
        val step = 4
        for (y in y1 until y2 step step) {
            for (x in x1 until x2 step step) {
                val l = lum(b.getPixel(x, y))
                if (l > 205) bright++
                if (l < 70) dark++
                total++
            }
        }
        if (total == 0) return false

        val brightRatio = bright.toDouble() / total
        val darkRatio = dark.toDouble() / total
        return brightRatio in 0.015..0.20 && darkRatio > 0.35
    }

    private fun lum(c: Int): Double {
        val r = (c shr 16) and 255
        val g = (c shr 8) and 255
        val bl = c and 255
        return 0.2126 * r + 0.7152 * g + 0.0722 * bl
    }
}
