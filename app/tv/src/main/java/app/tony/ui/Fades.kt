package app.tony.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import coil3.size.Size
import coil3.transform.Transformation

/*
 * Performance (Chromecast GPU is weak): instead of drawing see-through gradient layers over every
 * picture on every frame, the fades are painted into the picture once, when it's loaded.
 */

private fun eased(color: Int, from: Float, to: Float, steps: Int = 14): Pair<IntArray, FloatArray> {
    val colors = IntArray(steps + 1); val pos = FloatArray(steps + 1)
    for (i in 0..steps) {
        val t = i / steps.toFloat(); val e = t * t * (3 - 2 * t)
        val a = ((from + (to - from) * e) * 255).toInt().coerceIn(0, 255)
        colors[i] = (a shl 24) or (color and 0xFFFFFF); pos[i] = t
    }
    return colors to pos
}

/** Crop the top part of the frame (drops the news ticker along the bottom) to [outW]×[outH]. */
private fun cropTop(input: Bitmap, keep: Float, outW: Int, outH: Int): Bitmap {
    val srcH = (input.height * keep).toInt().coerceAtLeast(1)
    // centre-crop to the output aspect within the kept area
    val aspect = outW / outH.toFloat()
    var w = input.width; var h = srcH
    if (w / h.toFloat() > aspect) w = (h * aspect).toInt() else h = (w / aspect).toInt()
    val left = (input.width - w) / 2
    val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
    Canvas(out).drawBitmap(input, Rect(left, 0, left + w, h), Rect(0, 0, outW, outH), Paint(Paint.FILTER_BITMAP_FLAG))
    return out
}

/** Card artwork: top-anchored crop + dark fade over the lower ~62% for the title. */
class CardArt(private val w: Int, private val h: Int) : Transformation() {
    override val cacheKey = "cardArt-$w-$h-v2"
    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val out = cropTop(input, 1f / 1.28f, w, h)
        val c = Canvas(out)
        val (cols, pos) = eased(0x0E0F15, 0f, 0.97f)
        val top = h * 0.38f
        c.drawRect(0f, top, w.toFloat(), h.toFloat(), Paint().apply { shader = LinearGradient(0f, top, 0f, h.toFloat(), cols, pos, Shader.TileMode.CLAMP) })
        return out
    }
}

/** Hero picture: top-anchored crop + left fade (text side), bottom fade into the page, top fade (bar). */
class HeroArt(private val w: Int, private val h: Int) : Transformation() {
    override val cacheKey = "heroArt-$w-$h-v2"
    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val out = cropTop(input, 1f / 1.18f, w, h)
        val c = Canvas(out)
        val (l, lp) = eased(0x0E0F15, 0.95f, 0f)
        c.drawRect(0f, 0f, w * 0.8f, h.toFloat(), Paint().apply { shader = LinearGradient(0f, 0f, w * 0.8f, 0f, l, lp, Shader.TileMode.CLAMP) })
        val (b, bp) = eased(0x1B1E2B, 0f, 1f)
        val bt = h * 0.4f
        c.drawRect(0f, bt, w.toFloat(), h.toFloat(), Paint().apply { shader = LinearGradient(0f, bt, 0f, h * 0.94f, b, bp, Shader.TileMode.CLAMP) })
        val (t, tp) = eased(0x0E0F15, 0.9f, 0f)
        c.drawRect(0f, 0f, w.toFloat(), h * 0.24f, Paint().apply { shader = LinearGradient(0f, 0f, 0f, h * 0.24f, t, tp, Shader.TileMode.CLAMP) })
        return out
    }
}
