package app.tony.player

import android.content.Context
import android.graphics.Bitmap
import android.view.TextureView
import androidx.compose.runtime.mutableStateMapOf
import app.tony.data.Channel
import app.tony.data.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Pictures the TV takes itself. Some stations only serve viewers in their own country, or use a
 * format the hourly server job can't read, so they never get a server still. When such a channel
 * plays in a preview on this TV, Tony keeps one small frame (app-private cache, capped) and the
 * cards use it instead of the plain logo tile. Nothing leaves the TV.
 */
object LocalStills {
    private const val W = 640
    private const val H = 360
    private const val MAX_FILES = 300
    private const val REFRESH_MS = 30 * 60 * 1000L

    /** channel id -> time of the local picture (state, so cards update when a new one is taken). */
    val versions = mutableStateMapOf<String, Long>()
    private var dir: File? = null

    fun init(ctx: Context) {
        val d = File(ctx.cacheDir, "stills").apply { mkdirs() }
        dir = d
        d.listFiles()?.forEach { f -> versions[f.nameWithoutExtension] = f.lastModified() }
    }

    fun file(id: String): File? = dir?.let { File(it, "$id.jpg") }?.takeIf { versions.containsKey(id) && it.exists() }

    /** What a card should load: the server still, or this TV's own picture once the server one failed. */
    fun model(c: Channel, serverFailed: Boolean): Any {
        val local = if (serverFailed) file(c.id) else null
        return local ?: Repository.stillUrl(c)
    }

    fun wants(id: String): Boolean = (versions[id] ?: 0L) < System.currentTimeMillis() - REFRESH_MS

    /** Grab the current frame of [view] (must run on the main thread) and save it in the background. */
    suspend fun capture(id: String, view: TextureView) {
        if (!wants(id) || !view.isAvailable) return
        val bmp = runCatching { view.getBitmap(W, H) }.getOrNull() ?: return
        val d = dir ?: return
        withContext(Dispatchers.IO) {
            try {
                if (tooDark(bmp)) return@withContext   // a black frame is worse than the logo tile
                val out = trimBars(bmp)
                val f = File(d, "$id.jpg")
                val tmp = File(d, "$id.tmp")
                tmp.outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, 80, it) }
                if (out !== bmp) out.recycle()
                tmp.renameTo(f)
                val files = d.listFiles { x -> x.extension == "jpg" }.orEmpty().sortedBy { it.lastModified() }
                files.take((files.size - MAX_FILES).coerceAtLeast(0)).forEach { old -> old.delete(); versions.remove(old.nameWithoutExtension) }
                withContext(Dispatchers.Main) { versions[id] = f.lastModified() }
            } finally { bmp.recycle() }
        }
    }

    /** Cut off black letterbox/pillarbox bars, then fill 16:9 again (centre crop), like the server job. */
    private fun trimBars(b: Bitmap): Bitmap {
        fun rowDark(y: Int) = (0 until b.width step 8).all { x -> lum(b.getPixel(x, y)) < 22 }
        fun colDark(x: Int) = (0 until b.height step 8).all { y -> lum(b.getPixel(x, y)) < 22 }
        var top = 0; while (top < b.height / 3 && rowDark(top)) top++
        var bottom = b.height - 1; while (bottom > b.height * 2 / 3 && rowDark(bottom)) bottom--
        var left = 0; while (left < b.width / 3 && colDark(left)) left++
        var right = b.width - 1; while (right > b.width * 2 / 3 && colDark(right)) right--
        if (top == 0 && left == 0 && bottom == b.height - 1 && right == b.width - 1) return b
        var w = right - left + 1; var h = bottom - top + 1
        if (w * 9 > h * 16) { val nw = h * 16 / 9; left += (w - nw) / 2; w = nw } else { val nh = w * 9 / 16; top += (h - nh) / 2; h = nh }
        val cut = Bitmap.createBitmap(b, left, top, w.coerceAtLeast(16), h.coerceAtLeast(9))
        return Bitmap.createScaledBitmap(cut, W, H, true).also { if (it !== cut) cut.recycle() }
    }

    private fun lum(p: Int) = ((p shr 16 and 255) + (p shr 8 and 255) + (p and 255)) / 3

    private fun tooDark(b: Bitmap): Boolean {
        var sum = 0L; var n = 0
        for (y in 0 until b.height step 24) for (x in 0 until b.width step 24) {
            val p = b.getPixel(x, y); sum += ((p shr 16 and 255) + (p shr 8 and 255) + (p and 255)) / 3; n++
        }
        return n == 0 || sum / n < 18
    }
}
