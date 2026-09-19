package com.countdown.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import java.io.File
import kotlin.math.cos
import kotlin.random.Random

class MotivationLayer(private val context: Context) {

    companion object {
        const val CARD_INTERVAL_MS = 7000L
        const val TRANSITION_MS = 700L
        const val MAX_USER_IMAGES = 12
        const val USER_IMAGES_DIR = "motivation_images"

        fun userImagesDir(context: Context): File =
            File(context.filesDir, USER_IMAGES_DIR).apply { if (!exists()) mkdirs() }
    }

    private var cards: List<MotivationCard> = MotivationContent.defaultCards()
    private val userBitmaps = LinkedHashMap<String, Bitmap>()

    private var currentCard: MotivationCard? = null
    private var nextCard: MotivationCard? = null
    private var lastSwitchTime = 0L
    private var transitionStart = 0L
    private var lastIndex = -1

    private val bgPaint = Paint().apply { isAntiAlias = true; color = Color.argb(28, 255, 255, 255) }
    private val borderPaint = Paint().apply {
        isAntiAlias = true; style = Paint.Style.STROKE; strokeWidth = 1.5f
        color = Color.argb(60, 255, 255, 255)
    }
    private val titlePaint = Paint().apply {
        isAntiAlias = true; color = Color.WHITE; textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }
    private val subtitlePaint = Paint().apply {
        isAntiAlias = true; color = Color.LTGRAY; textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }
    private val spinePaint = Paint().apply { isAntiAlias = true }

    fun refreshUserImages() {
        val dir = userImagesDir(context)
        val files = dir.listFiles { f -> f.isFile }?.sortedByDescending { it.lastModified() }
            ?.take(MAX_USER_IMAGES) ?: emptyList()

        val validPaths = files.map { it.absolutePath }.toSet()
        val it = userBitmaps.entries.iterator()
        while (it.hasNext()) { if (it.next().key !in validPaths) it.remove() }

        for (f in files) {
            if (!userBitmaps.containsKey(f.absolutePath)) {
                decodeSmall(f.absolutePath, 320)?.let { bmp -> userBitmaps[f.absolutePath] = bmp }
            }
        }

        cards = MotivationContent.defaultCards() + files.map {
            MotivationCard(CardType.IMAGE, it.nameWithoutExtension, imagePath = it.absolutePath)
        }
    }

    fun isTransitioning(): Boolean = nextCard != null

    fun update(now: Long) {
        if (currentCard == null && cards.isNotEmpty()) {
            currentCard = cards[Random.nextInt(cards.size)]
            lastSwitchTime = now
            return
        }
        if (nextCard == null && cards.size > 1 && now - lastSwitchTime >= CARD_INTERVAL_MS) {
            var idx: Int
            do { idx = Random.nextInt(cards.size) } while (idx == lastIndex)
            lastIndex = idx
            nextCard = cards[idx]
            transitionStart = now
        }
        if (nextCard != null && now - transitionStart >= TRANSITION_MS) {
            currentCard = nextCard
            nextCard = null
            lastSwitchTime = now
        }
    }

    fun draw(canvas: Canvas, screenW: Float, screenH: Float, now: Long) {
        val card = currentCard ?: return
        val cardW = screenW * 0.74f
        val cardH = screenH * 0.095f
        val cx = screenW / 2f
        val cy = screenH * 0.80f

        titlePaint.textSize = screenW * 0.034f
        subtitlePaint.textSize = screenW * 0.024f

        if (nextCard != null) {
            val raw = ((now - transitionStart).toFloat() / TRANSITION_MS).coerceIn(0f, 1f)
            val eased = (1 - cos(raw * Math.PI)).toFloat() / 2f
            drawCard(canvas, card, cx, cy, cardW, cardH, 1f - eased, -eased * 14f)
            drawCard(canvas, nextCard!!, cx, cy, cardW, cardH, eased, (1f - eased) * 14f)
        } else {
            drawCard(canvas, card, cx, cy, cardW, cardH, 1f, 0f)
        }
    }

    private fun drawCard(
        canvas: Canvas, card: MotivationCard, cx: Float, cy: Float,
        w: Float, h: Float, alpha: Float, riseOffset: Float
    ) {
        if (alpha <= 0.01f) return
        val a = (alpha * 255).toInt().coerceIn(0, 255)
        val top = cy - h / 2f + riseOffset
        val bottom = cy + h / 2f + riseOffset
        val left = cx - w / 2f
        val right = cx + w / 2f
        val rect = RectF(left, top, right, bottom)
        val radius = h * 0.28f

        bgPaint.alpha = (28 * alpha).toInt().coerceIn(0, 255)
        borderPaint.alpha = (60 * alpha).toInt().coerceIn(0, 255)
        canvas.drawRoundRect(rect, radius, radius, bgPaint)
        canvas.drawRoundRect(rect, radius, radius, borderPaint)

        titlePaint.alpha = a
        subtitlePaint.alpha = (a * 0.8f).toInt().coerceIn(0, 255)

        when (card.type) {
            CardType.BOOK -> {
                spinePaint.color = card.accentColor ?: Color.DKGRAY
                spinePaint.alpha = a
                val spineW = h * 0.34f
                val spineRect = RectF(left + h * 0.18f, top + h * 0.16f, left + h * 0.18f + spineW, bottom - h * 0.16f)
                canvas.drawRoundRect(spineRect, 6f, 6f, spinePaint)
                val textX = spineRect.right + (right - spineRect.right) / 2f
                canvas.drawText(card.title, textX, cy + riseOffset - h * 0.03f, titlePaint)
                card.subtitle?.let { canvas.drawText(it, textX, cy + riseOffset + h * 0.24f, subtitlePaint) }
            }
            CardType.IMAGE -> {
                val bmp = userBitmaps[card.imagePath]
                if (bmp != null) {
                    val imgH = h * 0.78f
                    val imgW = imgH * (bmp.width.toFloat() / bmp.height.toFloat())
                    val imgLeft = left + h * 0.16f
                    val imgTop = cy + riseOffset - imgH / 2f
                    val srcRect = Rect(0, 0, bmp.width, bmp.height)
                    val dstRect = RectF(imgLeft, imgTop, imgLeft + imgW, imgTop + imgH)
                    val p = Paint(Paint.ANTI_ALIAS_FLAG)
                    p.alpha = a
                    canvas.drawBitmap(bmp, srcRect, dstRect, p)
                    val textX = imgLeft + imgW + (right - (imgLeft + imgW)) / 2f
                    canvas.drawText(card.title, textX, cy + riseOffset, titlePaint)
                } else {
                    canvas.drawText(card.title, cx, cy + riseOffset, titlePaint)
                }
            }
            else -> {
                canvas.drawText(card.title, cx, cy + riseOffset - (if (card.subtitle != null) h * 0.06f else 0f), titlePaint)
                card.subtitle?.let { canvas.drawText(it, cx, cy + riseOffset + h * 0.22f, subtitlePaint) }
            }
        }
    }

    private fun decodeSmall(path: String, maxDim: Int): Bitmap? {
        return try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, opts)
            var sample = 1
            while (opts.outWidth / sample > maxDim || opts.outHeight / sample > maxDim) sample *= 2
            BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
        } catch (e: Exception) { null }
    }
}
