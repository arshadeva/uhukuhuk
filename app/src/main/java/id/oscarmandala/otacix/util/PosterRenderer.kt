package id.oscarmandala.otacix.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.provider.MediaStore
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import androidx.core.graphics.PathParser
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import id.oscarmandala.otacix.R
import id.oscarmandala.otacix.data.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/** Membuat poster 1080x1620 bergaya liquid glass dari sebuah playlist (digambar langsung dengan Canvas). */
object PosterRenderer {
    private const val W = 1080f
    private const val H = 1620f
    private const val NAVY = 0xFF040B1E.toInt()
    private const val BLUE = 0xFF064A94.toInt()
    private const val BLUE_BRIGHT = 0xFF2B7BFF.toInt()
    private const val YELLOW = 0xFFFDC12B.toInt()

    private suspend fun load(ctx: Context, url: String): Bitmap? {
        val r = ctx.imageLoader.execute(ImageRequest.Builder(ctx).data(url).size(540).allowHardware(false).build())
        return ((r as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap
    }

    private suspend fun loadPhoto(ctx: Context, uri: Uri): Bitmap? {
        val r = ctx.imageLoader.execute(ImageRequest.Builder(ctx).data(uri).size(1080, 1620).allowHardware(false).build())
        return ((r as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap
    }

    /** style: 0 = kartu pemutar miring (default), 1 = grid 3x3, 2 = tumpukan miring. lite = resolusi lebih kecil (hemat RAM). */
    suspend fun render(ctx: Context, title: String, tracks: List<Track>, style: Int, lite: Boolean, bg: Uri? = null, poses: List<CardPose>? = null): Bitmap =
        withContext(Dispatchers.Default) {
            val s = if (lite) 2f / 3f else 1f
            val bmp = Bitmap.createBitmap((W * s).toInt(), (H * s).toInt(), Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            c.scale(s, s)

            val shown = tracks.take(when (style) { 0 -> 4; 1 -> 9; else -> 5 })
            val covers: List<Pair<Bitmap?, String>> = coroutineScope {
                shown.map { t -> async { load(ctx, t.cover) to t.cover } }.awaitAll()
            }

            if (style == 0) {
                val photo = if (bg != null) try { loadPhoto(ctx, bg) } catch (e: Exception) { null } else null
                drawCardsBackground(c, covers.firstOrNull(), photo)
                text(c, "$title · ${tracks.size} lagu", W / 2f, 50f, 32f, 0xDDFFFFFF.toInt(), bold = true, center = true)
                drawPlayerCards(c, shown, covers, poses)
                drawBrandPill(ctx, c)
            } else {
                drawBackground(c)
                drawHeader(ctx, c, title, tracks.size)
                if (style == 1) drawGrid(c, covers) else drawStack(c, covers)
                drawList(c, tracks, if (style == 1) 3 else 5, if (style == 1) 1400f else 1230f)
                drawFooter(c)
            }
            bmp
        }

    private fun drawBackground(c: Canvas) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(0f, 0f, 0f, H, intArrayOf(0xFF0B2F6B.toInt(), NAVY), null, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, W, H, p)
        fun blob(color: Int, cx: Float, cy: Float, r: Float) {
            p.shader = RadialGradient(cx, cy, r, color, 0x00000000, Shader.TileMode.CLAMP)
            c.drawRect(0f, 0f, W, H, p)
        }
        blob(0x66FDC12B, W * 0.95f, 140f, 700f)
        blob(0x882B7BFF.toInt(), 80f, 520f, 760f)
        blob(0x77064A94, W * 0.8f, H * 0.95f, 800f)
    }

    private fun text(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int, bold: Boolean = false, center: Boolean = false, maxW: Float = 920f, right: Boolean = false) {
        val tp = TextPaint(Paint.ANTI_ALIAS_FLAG)
        tp.textSize = size; tp.color = color
        tp.typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
        val t = TextUtils.ellipsize(s, tp, maxW, TextUtils.TruncateAt.END).toString()
        tp.textAlign = when {
            center -> Paint.Align.CENTER
            right -> Paint.Align.RIGHT
            else -> Paint.Align.LEFT
        }
        c.drawText(t, x, y, tp)
    }

    private fun drawHeader(ctx: Context, c: Canvas, title: String, count: Int) {
        // kartu logo putih (seperti di aplikasi)
        val tile = RectF(W - 60f - 230f, 70f, W - 60f, 70f + 112f)
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
        c.drawRoundRect(tile, 30f, 30f, bg)
        BitmapFactory.decodeResource(ctx.resources, R.drawable.logo)?.let { logo ->
            val dst = RectF(tile.left + 18f, tile.top + 12f, tile.right - 18f, tile.bottom - 12f)
            val sc = minOf(dst.width() / logo.width, dst.height() / logo.height)
            val w = logo.width * sc; val h = logo.height * sc
            val d = RectF(dst.centerX() - w / 2, dst.centerY() - h / 2, dst.centerX() + w / 2, dst.centerY() + h / 2)
            c.drawBitmap(logo, null, d, Paint(Paint.FILTER_BITMAP_FLAG))
        }
        text(c, "OTACIX", 60f, 120f, 56f, 0xFFFFFFFF.toInt(), bold = true)
        text(c, "by Oscar Mandala", 60f, 168f, 30f, YELLOW, bold = true)
        text(c, title, 60f, 290f, 92f, 0xFFFFFFFF.toInt(), bold = true)
        text(c, "$count lagu", 60f, 342f, 36f, 0xCCFFE08A.toInt())
    }

    private fun drawCover(c: Canvas, bmp: Bitmap?, url: String, r: RectF, radius: Float) {
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        if (bmp == null) {
            fill.shader = LinearGradient(r.left, r.top, r.right, r.bottom, BLUE_BRIGHT, BLUE, Shader.TileMode.CLAMP)
            c.drawRoundRect(r, radius, radius, fill)
        } else {
            val crop = if (url.contains("ytimg.com")) 1.34f else 1f
            val sc = maxOf(r.width() / bmp.width, r.height() / bmp.height) * crop
            val m = Matrix()
            m.setScale(sc, sc)
            m.postTranslate(r.left + (r.width() - bmp.width * sc) / 2f, r.top + (r.height() - bmp.height * sc) / 2f)
            val sh = BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            sh.setLocalMatrix(m)
            fill.shader = sh
            c.drawRoundRect(r, radius, radius, fill)
        }
        // tepi kaca
        val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 4f
            shader = LinearGradient(r.left, r.top, r.left, r.bottom, 0xAAFFFFFF.toInt(), 0x22FFFFFF, Shader.TileMode.CLAMP)
        }
        c.drawRoundRect(r, radius, radius, edge)
    }

    private fun drawGrid(c: Canvas, covers: List<Pair<Bitmap?, String>>) {
        // panel kaca di belakang grid
        val panel = RectF(40f, 380f, W - 40f, 1360f)
        c.drawRoundRect(panel, 56f, 56f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x22FFFFFF })
        c.drawRoundRect(panel, 56f, 56f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 3f; color = 0x55FFFFFF
        })
        val size = 296f; val gap = 28f
        val left = (W - (3 * size + 2 * gap)) / 2f
        for (i in 0 until 9) {
            val row = i / 3; val col = i % 3
            val x = left + col * (size + gap); val y = 410f + row * (size + gap)
            val (b, u) = covers.getOrNull(i) ?: (null to "")
            drawCover(c, b, u, RectF(x, y, x + size, y + size), 38f)
        }
    }

    private fun drawStack(c: Canvas, covers: List<Pair<Bitmap?, String>>) {
        val angles = floatArrayOf(-15f, 10f, -7f, 13f, 0f)
        val dx = floatArrayOf(-140f, 150f, -90f, 90f, 0f)
        val dy = floatArrayOf(-30f, 10f, 60f, 70f, 40f)
        val n = covers.size.coerceAtLeast(1)
        for (k in 0 until n) {
            val i = n - 1 - k                    // gambar dari belakang ke depan
            val (b, u) = covers.getOrNull(i) ?: (null to "")
            val size = if (i == 0) 600f else 480f
            c.save()
            c.translate(W / 2f + dx[i], 730f + dy[i])
            c.rotate(angles[i])
            drawCover(c, b, u, RectF(-size / 2, -size / 2, size / 2, size / 2), 52f)
            c.restore()
        }
    }

    private fun drawList(c: Canvas, tracks: List<Track>, max: Int, y0: Float) {
        tracks.take(max).forEachIndexed { i, t ->
            val y = y0 + i * 52f
            text(c, String.format("%02d", i + 1), 60f, y, 32f, YELLOW, bold = true)
            text(c, "${t.title} — ${t.artist}", 130f, y, 32f, 0xEEFFFFFF.toInt(), maxW = 880f)
        }
        if (tracks.size > max) text(c, "+ ${tracks.size - max} lagu lainnya", 130f, y0 + max * 52f, 28f, 0x99FFFFFF.toInt())
    }

    private fun drawFooter(c: Canvas) {
        text(c, "Dibuat dengan Otacix · by Oscar Mandala", W / 2f, H - 44f, 28f, 0x99FFFFFF.toInt(), center = true)
    }

    // ---- gaya "Kartu pemutar": 4 kartu kaca miring (seperti tampilan pemutar), latar sampul diburamkan ----
    private const val P_PAUSE = "M6,19h4V5H6v14zm8,-14v14h4V5h-4z"
    private const val P_FF = "M4,18l8.5,-6L4,6v12zM13,6v12l8.5,-6L13,6z"
    private const val P_RW = "M11,18V6l-8.5,6 8.5,6zM11.5,12l8.5,6V6l-8.5,6z"
    private const val P_HEART = "M16.5,3c-1.74,0 -3.41,0.81 -4.5,2.09C10.91,3.81 9.24,3 7.5,3 4.42,3 2,5.42 2,8.5c0,3.78 3.4,6.86 8.55,11.54L12,21.35l1.45,-1.32C18.6,15.36 22,12.28 22,8.5 22,5.42 19.58,3 16.5,3zM12.1,18.55l-0.1,0.1 -0.1,-0.1C7.14,14.24 4,11.39 4,8.5 4,6.5 5.5,5 7.5,5c1.54,0 3.04,0.99 3.57,2.36h1.87C13.46,5.99 14.96,5 16.5,5c2,0 3.5,1.5 3.5,3.5 0,2.89 -3.14,5.74 -7.9,10.05z"
    private const val P_CHECK = "M9,16.17L4.83,12l-1.42,1.41L9,19 21,7l-1.41,-1.41z"

    private val pausePath: Path by lazy { PathParser.createPathFromPathData(P_PAUSE) }
    private val ffPath: Path by lazy { PathParser.createPathFromPathData(P_FF) }
    private val rwPath: Path by lazy { PathParser.createPathFromPathData(P_RW) }
    private val heartPath: Path by lazy { PathParser.createPathFromPathData(P_HEART) }
    private val checkPath: Path by lazy { PathParser.createPathFromPathData(P_CHECK) }

    /** Gambar ikon path SVG 24x24 berpusat di (cx, cy) dengan ukuran sisi `size`. */
    private fun drawIcon(c: Canvas, p: Path, cx: Float, cy: Float, size: Float, argb: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = argb
        paint.style = Paint.Style.FILL
        c.save()
        c.translate(cx - size / 2f, cy - size / 2f)
        val sc = size / 24f
        c.scale(sc, sc)
        c.drawPath(p, paint)
        c.restore()
    }

    private fun fmtSec(sec: Int): String {
        val s = sec.coerceAtLeast(0)
        return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
    }

    private fun drawCardsBackground(c: Canvas, first: Pair<Bitmap?, String>?, photo: Bitmap?) {
        drawBackground(c)
        if (photo != null && photo.width > 0 && photo.height > 0) {
            // foto sendiri: dipotong tengah memenuhi poster, ditutup bayangan tipis agar kartu tetap terbaca
            val sc = maxOf(W / photo.width, H / photo.height)
            val m = Matrix()
            m.setScale(sc, sc)
            m.postTranslate((W - photo.width * sc) / 2f, (H - photo.height * sc) / 2f)
            c.drawBitmap(photo, m, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            val tint = Paint(Paint.ANTI_ALIAS_FLAG)
            tint.shader = LinearGradient(0f, 0f, 0f, H, 0x44040B1E, 0x99040B1E.toInt(), Shader.TileMode.CLAMP)
            c.drawRect(0f, 0f, W, H, tint)
            return
        }
        val src = first?.first
        if (src != null && src.width > 8 && src.height > 8) {
            // sampul dipotong persegi lalu dikecilkan ke 24px dan diperbesar lagi = latar buram lembut
            val crop = if (first?.second.orEmpty().contains("ytimg.com")) 1.34f else 1f
            val side = (minOf(src.width, src.height) / crop).toInt().coerceIn(8, minOf(src.width, src.height))
            val sq = Bitmap.createBitmap(src, (src.width - side) / 2, (src.height - side) / 2, side, side)
            val tiny = Bitmap.createScaledBitmap(sq, 24, 24, true)
            val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            c.drawBitmap(tiny, null, RectF(-W * 0.25f, -H * 0.1f, W * 1.25f, H * 1.1f), p)
        }
        val shade = Paint(Paint.ANTI_ALIAS_FLAG)
        shade.shader = LinearGradient(0f, 0f, 0f, H, 0x99040B1E.toInt(), 0xDD040B1E.toInt(), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, W, H, shade)
    }

    /** Posisi satu kartu di kanvas poster 1080x1620: pusat (x, y), sudut putar (derajat) dan skala. */
    class CardPose(val x: Float, val y: Float, val angle: Float, val scale: Float) {
        fun copy(x: Float = this.x, y: Float = this.y, angle: Float = this.angle, scale: Float = this.scale) = CardPose(x, y, angle, scale)
    }

    const val CARD_W = 450f
    const val CARD_H = 700f
    const val POSTER_W = 1080f
    const val POSTER_H = 1620f

    val templateNames = listOf("Miring", "Lurus", "Tumpuk", "Fokus")

    /** Template tata letak kartu untuk [n] kartu (1..4). Template 0 = tampilan awal. */
    fun template(id: Int, n: Int): List<CardPose> {
        if (n <= 0) return emptyList()
        val k = 0.92f
        return when (id) {
            1 -> listOf(CardPose(295f, 430f, 0f, k), CardPose(785f, 430f, 0f, k), CardPose(295f, 1050f, 0f, k), CardPose(785f, 1050f, 0f, k)).take(n)
            2 -> listOf(CardPose(330f, 430f, -8f, 0.82f), CardPose(750f, 660f, 7f, 0.82f), CardPose(330f, 980f, -6f, 0.82f), CardPose(750f, 1210f, 6f, 0.82f)).take(n)
            3 -> listOf(CardPose(540f, 700f, 0f, 1.05f), CardPose(190f, 1290f, -6f, 0.5f), CardPose(540f, 1300f, 0f, 0.5f), CardPose(890f, 1290f, 6f, 0.5f)).take(n)
            else -> when (n) {
                1 -> listOf(CardPose(540f, 740f, -4f, k))
                2 -> listOf(CardPose(295f, 424f, -7f, k), CardPose(785f, 1068f, 5f, k))
                3 -> listOf(CardPose(295f, 424f, -7f, k), CardPose(785f, 406f, 6f, k), CardPose(295f, 1040f, -5f, k))
                else -> listOf(CardPose(295f, 424f, -7f, k), CardPose(785f, 406f, 6f, k), CardPose(295f, 1040f, -5f, k), CardPose(785f, 1068f, 5f, k))
            }
        }
    }

    private fun drawPlayerCards(c: Canvas, shown: List<Track>, covers: List<Pair<Bitmap?, String>>, poses: List<CardPose>?) {
        val ps = poses?.takeIf { it.size == shown.size } ?: template(0, shown.size)
        val progress = floatArrayOf(0.62f, 0.18f, 0.08f, 0.34f)
        for (i in shown.indices) {
            val (b, u) = covers.getOrNull(i) ?: (null to "")
            val s = ps[i]
            drawPlayerCard(c, b, u, shown[i], s.x, s.y, s.angle, s.scale, progress[i % progress.size])
        }
    }

    private fun drawPlayerCard(c: Canvas, bmp: Bitmap?, url: String, t: Track, cx: Float, cy: Float, angle: Float, k: Float, progress: Float) {
        val cw = 450f
        val ch = 700f
        val pad = 28f
        c.save()
        c.translate(cx, cy)
        c.rotate(angle)
        c.scale(k, k)

        val card = RectF(-cw / 2f, -ch / 2f, cw / 2f, ch / 2f)
        val body = Paint(Paint.ANTI_ALIAS_FLAG)
        body.color = 0xCC1E2230.toInt()
        body.setShadowLayer(46f, 0f, 22f, 0x80000000.toInt())
        c.drawRoundRect(card, 52f, 52f, body)
        val edge = Paint(Paint.ANTI_ALIAS_FLAG)
        edge.style = Paint.Style.STROKE
        edge.strokeWidth = 3f
        edge.shader = LinearGradient(card.left, card.top, card.left, card.bottom, 0x77FFFFFF, 0x14FFFFFF, Shader.TileMode.CLAMP)
        c.drawRoundRect(card, 52f, 52f, edge)

        // sampul
        val side = cw - 2f * pad
        val cover = RectF(card.left + pad, card.top + pad, card.right - pad, card.top + pad + side)
        drawCover(c, bmp, url, cover, 30f)

        val x0 = card.left + pad
        val x1 = card.right - pad
        val y0 = cover.bottom

        // label kecil "✓ Otacix" (ikon centang SVG)
        drawIcon(c, checkPath, x0 + 11f, y0 + 26f, 22f, 0xAAFFFFFF.toInt())
        text(c, "Otacix", x0 + 28f, y0 + 33f, 21f, 0xAAFFFFFF.toInt())

        // judul, artis, hati
        text(c, t.title, x0, y0 + 82f, 40f, 0xFFFFFFFF.toInt(), bold = true, maxW = side - 62f)
        text(c, t.artist, x0, y0 + 120f, 30f, 0xAAFFFFFF.toInt(), maxW = side - 62f)
        drawIcon(c, heartPath, x1 - 22f, y0 + 62f, 44f, 0xDDFFFFFF.toInt())

        // bilah progres
        val by = y0 + 148f
        val track = Paint(Paint.ANTI_ALIAS_FLAG)
        track.color = 0x44FFFFFF
        track.strokeWidth = 7f
        track.strokeCap = Paint.Cap.ROUND
        c.drawLine(x0, by, x1, by, track)
        val px = x0 + (x1 - x0) * progress
        val active = Paint(Paint.ANTI_ALIAS_FLAG)
        active.color = 0xFFFFFFFF.toInt()
        active.strokeWidth = 7f
        active.strokeCap = Paint.Cap.ROUND
        c.drawLine(x0, by, px, by, active)
        c.drawCircle(px, by, 12f, active)

        // waktu
        val elapsed = (t.dur * progress).toInt()
        text(c, fmtSec(elapsed), x0, y0 + 186f, 21f, 0xAAFFFFFF.toInt())
        text(c, "-" + fmtSec(t.dur - elapsed), x1, y0 + 186f, 21f, 0xAAFFFFFF.toInt(), right = true)

        // kontrol: mundur, jeda, maju
        val cyCtl = y0 + 226f
        drawIcon(c, rwPath, 0f - 112f, cyCtl, 52f, 0xFFFFFFFF.toInt())
        drawIcon(c, pausePath, 0f, cyCtl, 56f, 0xFFFFFFFF.toInt())
        drawIcon(c, ffPath, 0f + 112f, cyCtl, 52f, 0xFFFFFFFF.toInt())

        c.restore()
    }

    private fun drawBrandPill(ctx: Context, c: Canvas) {
        val pill = RectF(W / 2f - 270f, 1452f, W / 2f + 270f, 1552f)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        fill.color = 0x33FFFFFF
        c.drawRoundRect(pill, 50f, 50f, fill)
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG)
        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = 3f
        stroke.color = 0x77FFFFFF
        c.drawRoundRect(pill, 50f, 50f, stroke)

        val tile = RectF(pill.left + 14f, pill.top + 14f, pill.left + 14f + 150f, pill.bottom - 14f)
        val white = Paint(Paint.ANTI_ALIAS_FLAG)
        white.color = 0xFFFFFFFF.toInt()
        c.drawRoundRect(tile, 26f, 26f, white)
        BitmapFactory.decodeResource(ctx.resources, R.drawable.logo)?.let { logo ->
            val dst = RectF(tile.left + 12f, tile.top + 8f, tile.right - 12f, tile.bottom - 8f)
            val sc = minOf(dst.width() / logo.width, dst.height() / logo.height)
            val w = logo.width * sc
            val h = logo.height * sc
            val d = RectF(dst.centerX() - w / 2f, dst.centerY() - h / 2f, dst.centerX() + w / 2f, dst.centerY() + h / 2f)
            c.drawBitmap(logo, null, d, Paint(Paint.FILTER_BITMAP_FLAG))
        }
        text(c, "Otacix", tile.right + 28f, pill.top + 52f, 44f, 0xFFFFFFFF.toInt(), bold = true, maxW = 300f)
        text(c, "by Oscar Mandala", tile.right + 29f, pill.top + 82f, 25f, YELLOW, bold = true, maxW = 320f)
    }

    // ---- simpan & bagikan ----
    fun saveToGallery(ctx: Context, bmp: Bitmap, name: String = "otacix-poster-${System.currentTimeMillis()}.png"): Boolean {
        if (Build.VERSION.SDK_INT < 29) return false
        val v = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Otacix")
        }
        val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v) ?: return false
        return try {
            ctx.contentResolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) } == true
        } catch (e: Exception) { false }
    }

    fun share(ctx: Context, bmp: Bitmap, chooser: String = "Bagikan poster") {
        val dir = File(ctx.cacheDir, "posters").apply { mkdirs() }
        val f = File(dir, "otacix-poster.png")
        FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", f)
        val i = Intent(Intent.ACTION_SEND).setType("image/png")
            .putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        ctx.startActivity(Intent.createChooser(i, chooser))
    }
}
