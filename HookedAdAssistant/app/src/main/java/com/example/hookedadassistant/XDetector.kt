package com.example.hookedadassistant

import android.graphics.Bitmap
import kotlin.math.abs

object XDetector {
    fun likelyCloseControl(bitmap: Bitmap): Boolean {
        val w = bitmap.width
        val h = bitmap.height
        if (w < 200 || h < 300) return false

        val top = (h * 0.015).toInt()
        val bottom = (h * 0.34).toInt()
        val side = (w * 0.38).toInt()

        return scanRegion(bitmap, 0, top, side, bottom) ||
            scanRegion(bitmap, w - side, top, w, bottom)
    }

    private fun scanRegion(b: Bitmap, l: Int, t: Int, r: Int, bot: Int): Boolean {
        val sizes = intArrayOf(32, 40, 48, 56, 64)
        for (s in sizes) {
            val step = (s / 5).coerceAtLeast(6)
            var y = t
            while (y + s < bot) {
                var x = l
                while (x + s < r) {
                    if (tileScore(b, x, y, s) >= 0.52) return true
                    x += step
                }
                y += step
            }
        }
        return false
    }

    private fun tileScore(b: Bitmap, x0: Int, y0: Int, s: Int): Double {
        var sum = 0.0
        var n = 0
        val sample = (s / 12).coerceAtLeast(3)

        for (y in y0 until y0 + s step sample) {
            for (x in x0 until x0 + s step sample) {
                sum += lum(b.getPixel(x, y))
                n++
            }
        }
        if (n == 0) return 0.0

        val mean = sum / n
        fun contrast(x: Int, y: Int) = abs(lum(b.getPixel(x, y)) - mean)

        var diag = 0.0
        var diagN = 0
        var off = 0.0
        var offN = 0
        val margin = (s * 0.14).toInt().coerceAtLeast(3)
        val thickness = (s * 0.05).toInt().coerceIn(1, 3)

        for (i in margin until s - margin) {
            for (d in -thickness..thickness) {
                val xa = (x0 + i + d).coerceIn(x0, x0 + s - 1)
                val xb = (x0 + (s - 1 - i) + d).coerceIn(x0, x0 + s - 1)
                val yy = (y0 + i).coerceIn(y0, y0 + s - 1)
                diag += contrast(xa, yy)
                diag += contrast(xb, yy)
                diagN += 2
            }

            val q1 = x0 + s / 4
            val q3 = x0 + 3 * s / 4
            off += contrast(q1, y0 + i)
            off += contrast(q3, y0 + i)
            offN += 2
        }

        val ds = if (diagN > 0) diag / diagN else 0.0
        val os = if (offN > 0) off / offN else 1.0

        if (ds < 24.0) return 0.0
        val ratio = ds / (ds + os + 1.0)

        val center = lum(b.getPixel(x0 + s / 2, y0 + s / 2))
        val cContrast = abs(center - mean)
        val centerBoost = (cContrast / 255.0) * 0.08

        return (ratio + centerBoost).coerceIn(0.0, 1.0)
    }

    private fun lum(c: Int): Double {
        val r = (c shr 16) and 255
        val g = (c shr 8) and 255
        val bl = c and 255
        return 0.2126 * r + 0.7152 * g + 0.0722 * bl
    }
}
