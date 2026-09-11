package com.example.hookedadassistant

import android.graphics.Bitmap
import android.graphics.Rect
import kotlin.math.abs

object XDetector {
    data class Detection(val rect: Rect, val kind: String, val score: Double)

    fun findControl(bitmap: Bitmap): Detection? {
        val w = bitmap.width
        val h = bitmap.height
        if (w < 200 || h < 300) return null
        val top = (h * 0.01).toInt()
        val bottom = (h * 0.30).toInt()
        val side = (w * 0.34).toInt()

        val right = scanForX(bitmap, w - side, top, w, bottom)
        val left = scanForX(bitmap, 0, top, side, bottom)
        val next = findTopLeftNext(bitmap)
        return listOfNotNull(right, left, next).maxByOrNull { it.score }
    }

    fun likelyCloseControl(bitmap: Bitmap) = findControl(bitmap) != null

    private fun scanForX(b: Bitmap, l: Int, t: Int, r: Int, bot: Int): Detection? {
        var best: Detection? = null
        val sizes = intArrayOf(28, 32, 36, 40, 48, 56, 64, 72)
        for (s in sizes) {
            val step = (s / 6).coerceAtLeast(5)
            var y = t
            while (y + s < bot) {
                var x = l
                while (x + s < r) {
                    val score = xScore(b, x, y, s)
                    if (score >= 0.57 && (best == null || score > best.score))
                        best = Detection(Rect(x, y, x + s, y + s), "X / Schliessen", score)
                    x += step
                }
                y += step
            }
        }
        return best
    }

    private fun findTopLeftNext(b: Bitmap): Detection? {
        val w = b.width; val h = b.height
        val r = Rect((w * 0.02).toInt(), (h * 0.03).toInt(), (w * 0.32).toInt(), (h * 0.16).toInt())
        var bright = 0; var dark = 0; var total = 0
        for (y in r.top until r.bottom step 4) for (x in r.left until r.right step 4) {
            val l = lum(b.getPixel(x, y)); if (l > 205) bright++; if (l < 70) dark++; total++
        }
        if (total == 0) return null
        val br = bright.toDouble() / total; val dr = dark.toDouble() / total
        return if (br in 0.015..0.20 && dr > 0.35) Detection(r, "Next / Weiter", 0.60 + br) else null
    }

    private fun xScore(b: Bitmap, x0: Int, y0: Int, s: Int): Double {
        val margin = (s * 0.18).toInt().coerceAtLeast(3)
        val thickness = (s * 0.055).toInt().coerceIn(1, 4)
        val sample = (s / 14).coerceAtLeast(2)
        var mean = 0.0; var count = 0
        for (y in y0 until y0 + s step sample) for (x in x0 until x0 + s step sample) { mean += lum(b.getPixel(x, y)); count++ }
        if (count == 0) return 0.0
        mean /= count
        fun c(x: Int, y: Int) = abs(lum(b.getPixel(x, y)) - mean)
        var diag = 0.0; var diagN = 0; var bg = 0.0; var bgN = 0
        for (i in margin until s - margin) {
            for (d in -thickness..thickness) {
                val yy = (y0 + i).coerceIn(y0, y0 + s - 1)
                diag += c((x0 + i + d).coerceIn(x0, x0+s-1), yy)
                diag += c((x0 + s-1-i+d).coerceIn(x0, x0+s-1), yy); diagN += 2
            }
            bg += c(x0+s/5, y0+i) + c(x0+4*s/5, y0+i); bgN += 2
        }
        if (diagN == 0 || bgN == 0) return 0.0
        val d = diag/diagN; val o = bg/bgN
        if (d < 20.0) return 0.0
        return (d/(d+o+1.0) + (c(x0+s/2,y0+s/2)/255.0)*0.10).coerceIn(0.0,1.0)
    }

    private fun lum(c: Int): Double {
        val r=(c shr 16) and 255; val g=(c shr 8) and 255; val bl=c and 255
        return 0.2126*r + 0.7152*g + 0.0722*bl
    }
}
