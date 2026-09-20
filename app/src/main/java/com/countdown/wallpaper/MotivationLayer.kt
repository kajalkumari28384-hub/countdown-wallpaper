package com.countdown.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import java.io.File
import kotlin.math.cos
import kotlin.random.Random

class MotivationLayer(private val context: Context) {

    companion object {
        const val MIN_INTERVAL_MS = 6000L
        const val MAX_INTERVAL_MS = 9500L
        const val TRANSITION_MS = 650L
        const val MAX_USER_IMAGES = 12
        const val USER_IMAGES_DIR = "motivation_images"

        const val UPPER_RELX = 0.5f
        const val UPPER_RELY = 0.30f
        const val LOWER_RELX = 0.08f
        const val LOWER_RELY = 0.775f

        fun userImagesDir(context: Context): File =
            File(context.filesDir, USER_IMAGES_DIR).apply { if (!exists()) mkdirs() }
    }

    private var pool: List<MotivationCard> = emptyList()
    private val userBitmaps = LinkedHashMap<String, Bitmap>()

    private data class SlotState(
        val align: Paint.Align,
        val relX: Float,
        val relY: Float,
        var current: MotivationCard? = null,
        var next: MotivationCard? = null,
        var lastSwitchTime: Long = 0L,
        var transitionStart: Long = 0L,
        var intervalMs: Long = 7000L,
        var lastKey: String? = null
    )

    private val upperSlot = SlotState(Paint.Align.CENTER, UPPER_RELX, UPPER_RELY, intervalMs = randomInterval())
    private val lowerSlot = SlotState(Paint.Align.LEFT, LOWER_RELX, LOWER_RELY, intervalMs = randomInterval())

    private fun randomInterval() = Random.nextLong(MIN_INTERVAL_MS, MAX_INTERVAL_MS)

    private val textPaint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    private val glowPaint = Paint().apply {
        isAntiAlias = true
        color = Color.argb(70, 255, 255, 255)
        maskFilter = BlurMaskFilter(28f, BlurMaskFilter.Blur.NORMAL)
    }

    fun refreshContent() {
        val textCards = ContentStore.getActiveCards(context).map { MotivationCard(it.type, it.text) }

        val dir = userImagesDir(context)
        val files = dir.listFiles { f -> f.isFile }?.sortedByDescending { it.lastModified() }
            ?.take(MAX_USER_IMAGES) ?: emptyList()

        val validPaths = files.map { it.absolutePath }.toSet()
        val it = userBitmaps.entries.iterator()
        while (it.hasNext()) { if (it.next().key !in validPaths) it.remove() }
        for (f in files) {
            if (!userBitmaps.containsKey(f.absolutePath)) {
                decodeSmall(f.absolutePath, 400)?.let { bmp -> userBitmaps[f.absolutePath] = bmp }
            }
        }

        val images = files.map {
            MotivationCard(CardType.IMAGE, it.nameWithoutExtension, imagePath = it.absolutePath)
        }
        pool = textCards + images
    }

    fun isTransitioning(): Boolean = upperSlot.next != null || lowerSlot.next != null

    fun update(now: Long) {
        if (pool.isEmpty()) return
        updateSlot(upperSlot, lowerSlot, now)
        updateSlot(lowerSlot, upperSlot, now)
    }

    private fun updateSlot(slot: SlotState, other: SlotState, now: Long) {
        if (slot.current == null) {
            slot.current = pickCard(slot, other)
            slot.lastSwitchTime = now
            return
        }
        if (slot.next == null && now - slot.lastSwitchTime >= slot.intervalMs) {
            slot.next = pickCard(slot, other)
            slot.transitionStart = now
        }
        if (slot.next != null && now - slot.transitionStart >= TRANSITION_MS) {
            slot.current = slot.next
            slot.next = null
            slot.lastSwitchTime = now
            slot.intervalMs = randomInterval()
        }
    }

    private fun pickCard(slot: SlotState, other: SlotState): MotivationCard {
        if (pool.size == 1) return pool[0]
        val otherKey = other.current?.title
        var card: MotivationCard
        var attempts = 0
        do {
            card = pool[Random.nextInt(pool.size)]
            attempts++
        } while (attempts < 25 && (card.title == slot.lastKey || card.title == otherKey))
        slot.lastKey = card.title
        return card
    }

    fun draw(canvas: Canvas, w: Float, h: Float, now: Long) {
        if (pool.isEmpty()) return
        drawSlot(canvas, upperSlot, w, h, now)
        drawSlot(canvas, lowerSlot, w, h, now)
    }

    private fun drawSlot(canvas: Canvas, slot: SlotState, w: Float, h: Float, now: Long) {
        val card = slot.current ?: return
        val cx = w * slot.relX
        val cy = h * slot.relY

        if (slot.next != null) {
            val raw = ((now - slot.transitionStart).toFloat() / TRANSITION_MS).coerceIn(0f, 1f)
            val eased = (1 - cos(raw * Math.PI)).toFloat() / 2f
            drawCard(canvas, card, cx, cy, w, h, slot.align, 1f - eased, -eased * 10f)
            drawCard(canvas, slot.next!!, cx, cy, w, h, slot.align, eased, (1f - eased) * 10f)
        } else {
            drawCard(canvas, card, cx, cy, w, h, slot.align, 1f, 0f)
        }
    }

    private fun drawCard(
        canvas: Canvas, card: MotivationCard, cx: Float, cy: Float,
        w: Float, h: Float, align: Paint.Align, alpha: Float, riseOffset: Float
    ) {
        if (alpha <= 0.01f) return
        val a = (alpha * 235).toInt().coerceIn(0, 235)

        if (card.type == CardType.IMAGE) {
            val bmp = userBitmaps[card.imagePath] ?: return
            val maxH = h * 0.10f
            val maxW = w * 0.32f
            var drawH = maxH
            var drawW = drawH * (bmp.width.toFloat() / bmp.height.toFloat())
            if (drawW > maxW) { drawW = maxW; drawH = drawW * (bmp.height.toFloat() / bmp.width.toFloat()) }

            val left = when (align) {
                Paint.Align.LEFT -> cx
                Paint.Align.RIGHT -> cx - drawW
                else -> cx - drawW / 2f
            }
            val top = cy + riseOffset - drawH / 2f
            val dst = RectF(left, top, left + drawW, top + drawH)
            val src = Rect(0, 0, bmp.width, bmp.height)
            val cornerRadius = drawH * 0.12f

            glowPaint.alpha = (70 * alpha).toInt().coerceIn(0, 70)
            val glowPad = drawH * 0.06f
            val glowRect = RectF(dst.left - glowPad, dst.top - glowPad, dst.right + glowPad, dst.bottom + glowPad)
            canvas.drawRoundRect(glowRect, cornerRadius + glowPad, cornerRadius + glowPad, glowPaint)

            val clipPath = Path().apply { addRoundRect(dst, cornerRadius, cornerRadius, Path.Direction.CW) }
            canvas.save()
            canvas.clipPath(clipPath)
            val p = Paint(Paint.ANTI_ALIAS_FLAG)
            p.alpha = a
            canvas.drawBitmap(bmp, src, dst, p)
            canvas.restore()
        } else {
            textPaint.textSize = w * 0.054f
            textPaint.textAlign = align
            textPaint.alpha = a
            canvas.drawText(card.title, cx, cy + riseOffset, textPaint)
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
}        const val UPPER_BAND_BOTTOM = 0.36f
        const val LOWER_BAND_TOP = 0.68f
        const val LOWER_BAND_BOTTOM = 0.87f

        const val LOWER_FIXED_RELX = 0.08f
        const val LOWER_FIXED_RELY_IN_BAND = 0.5f

        fun userImagesDir(context: Context): File =
            File(context.filesDir, USER_IMAGES_DIR).apply { if (!exists()) mkdirs() }

        fun getHiddenTitles(context: Context): MutableSet<String> {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return HashSet(prefs.getStringSet(KEY_HIDDEN, emptySet()) ?: emptySet())
        }

        fun hideTitle(context: Context, title: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val current = getHiddenTitles(context)
            current.add(title)
            prefs.edit().putStringSet(KEY_HIDDEN, current).apply()
        }

        fun restoreAllDefaults(context: Context) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putStringSet(KEY_HIDDEN, emptySet()).apply()
        }
    }

    private var pool: List<MotivationCard> = emptyList()
    private val userBitmaps = LinkedHashMap<String, Bitmap>()

    private data class SlotState(
        var current: MotivationCard? = null,
        var next: MotivationCard? = null,
        var lastSwitchTime: Long = 0L,
        var transitionStart: Long = 0L,
        var intervalMs: Long = 7000L,
        var align: Paint.Align = Paint.Align.LEFT,
        var relX: Float = 0.08f,
        var relYInBand: Float = 0.5f,
        var lastKey: String? = null,
        val positionIsFixed: Boolean = false
    )

    private val upperSlot = SlotState(intervalMs = randomInterval(), positionIsFixed = false)
    private val lowerSlot = SlotState(
        intervalMs = randomInterval(),
        align = Paint.Align.LEFT,
        relX = LOWER_FIXED_RELX,
        relYInBand = LOWER_FIXED_RELY_IN_BAND,
        positionIsFixed = true
    )

    private fun randomInterval() = Random.nextLong(MIN_INTERVAL_MS, MAX_INTERVAL_MS)

    private val textPaint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    // Soft glow behind uploaded images — subtle, no color, just a gentle
    // white blur so the image looks a touch "lifted" off the black background.
    private val glowPaint = Paint().apply {
        isAntiAlias = true
        color = Color.argb(70, 255, 255, 255)
        maskFilter = BlurMaskFilter(28f, BlurMaskFilter.Blur.NORMAL)
    }

    fun refreshContent() {
        val hidden = getHiddenTitles(context)
        val dir = userImagesDir(context)
        val files = dir.listFiles { f -> f.isFile }?.sortedByDescending { it.lastModified() }
            ?.take(MAX_USER_IMAGES) ?: emptyList()

        val validPaths = files.map { it.absolutePath }.toSet()
        val it = userBitmaps.entries.iterator()
        while (it.hasNext()) { if (it.next().key !in validPaths) it.remove() }
        for (f in files) {
            if (!userBitmaps.containsKey(f.absolutePath)) {
                decodeSmall(f.absolutePath, 400)?.let { bmp -> userBitmaps[f.absolutePath] = bmp }
            }
        }

        val defaults = MotivationContent.defaultCards().filter { it.title !in hidden }
        val images = files.map {
            MotivationCard(CardType.IMAGE, it.nameWithoutExtension, imagePath = it.absolutePath)
        }
        pool = defaults + images
    }

    fun isTransitioning(): Boolean = upperSlot.next != null || lowerSlot.next != null

    fun update(now: Long) {
        if (pool.isEmpty()) return
        updateSlot(upperSlot, now)
        updateSlot(lowerSlot, now)
    }

    private fun updateSlot(slot: SlotState, now: Long) {
        if (slot.current == null) {
            slot.current = pickCard(slot)
            slot.lastSwitchTime = now
            if (!slot.positionIsFixed) randomizePosition(slot)
            return
        }
        if (slot.next == null && now - slot.lastSwitchTime >= slot.intervalMs) {
            slot.next = pickCard(slot)
            slot.transitionStart = now
        }
        if (slot.next != null && now - slot.transitionStart >= TRANSITION_MS) {
            slot.current = slot.next
            slot.next = null
            slot.lastSwitchTime = now
            slot.intervalMs = randomInterval()
            if (!slot.positionIsFixed) randomizePosition(slot)
        }
    }

    private fun pickCard(slot: SlotState): MotivationCard {
        if (pool.size == 1) return pool[0]
        var card: MotivationCard
        do { card = pool[Random.nextInt(pool.size)] } while (card.title == slot.lastKey)
        slot.lastKey = card.title
        return card
    }

    private fun randomizePosition(slot: SlotState) {
        when (Random.nextInt(3)) {
            0 -> { slot.align = Paint.Align.LEFT; slot.relX = 0.07f }
            1 -> { slot.align = Paint.Align.CENTER; slot.relX = 0.5f }
            else -> { slot.align = Paint.Align.RIGHT; slot.relX = 0.93f }
        }
        slot.relYInBand = 0.15f + Random.nextFloat() * 0.7f
    }

    fun draw(canvas: Canvas, w: Float, h: Float, now: Long) {
        if (pool.isEmpty()) return
        drawSlot(canvas, upperSlot, w, h, UPPER_BAND_TOP, UPPER_BAND_BOTTOM, now)
        drawSlot(canvas, lowerSlot, w, h, LOWER_BAND_TOP, LOWER_BAND_BOTTOM, now)
    }

    private fun drawSlot(
        canvas: Canvas, slot: SlotState, w: Float, h: Float,
        bandTopRatio: Float, bandBottomRatio: Float, now: Long
    ) {
        val card = slot.current ?: return
        val cx = w * slot.relX
        val cy = h * (bandTopRatio + (bandBottomRatio - bandTopRatio) * slot.relYInBand)

        if (slot.next != null) {
            val raw = ((now - slot.transitionStart).toFloat() / TRANSITION_MS).coerceIn(0f, 1f)
            val eased = (1 - cos(raw * Math.PI)).toFloat() / 2f
            drawCard(canvas, card, cx, cy, w, h, slot.align, 1f - eased, -eased * 10f)
            drawCard(canvas, slot.next!!, cx, cy, w, h, slot.align, eased, (1f - eased) * 10f)
        } else {
            drawCard(canvas, card, cx, cy, w, h, slot.align, 1f, 0f)
        }
    }

    private fun drawCard(
        canvas: Canvas, card: MotivationCard, cx: Float, cy: Float,
        w: Float, h: Float, align: Paint.Align, alpha: Float, riseOffset: Float
    ) {
        if (alpha <= 0.01f) return
        val a = (alpha * 235).toInt().coerceIn(0, 235)

        if (card.type == CardType.IMAGE) {
            val bmp = userBitmaps[card.imagePath] ?: return
            val maxH = h * 0.10f
            val maxW = w * 0.32f
            var drawH = maxH
            var drawW = drawH * (bmp.width.toFloat() / bmp.height.toFloat())
            if (drawW > maxW) { drawW = maxW; drawH = drawW * (bmp.height.toFloat() / bmp.width.toFloat()) }

            val left = when (align) {
                Paint.Align.LEFT -> cx
                Paint.Align.RIGHT -> cx - drawW
                else -> cx - drawW / 2f
            }
            val top = cy + riseOffset - drawH / 2f
            val dst = RectF(left, top, left + drawW, top + drawH)
            val src = Rect(0, 0, bmp.width, bmp.height)
            val cornerRadius = drawH * 0.12f

            // Soft backlight glow, slightly larger than the image, faded by alpha.
            glowPaint.alpha = (70 * alpha).toInt().coerceIn(0, 70)
            val glowPad = drawH * 0.06f
            val glowRect = RectF(dst.left - glowPad, dst.top - glowPad, dst.right + glowPad, dst.bottom + glowPad)
            canvas.drawRoundRect(glowRect, cornerRadius + glowPad, cornerRadius + glowPad, glowPaint)

            val clipPath = Path().apply { addRoundRect(dst, cornerRadius, cornerRadius, Path.Direction.CW) }
            canvas.save()
            canvas.clipPath(clipPath)
            val p = Paint(Paint.ANTI_ALIAS_FLAG)
            p.alpha = a
            canvas.drawBitmap(bmp, src, dst, p)
            canvas.restore()
        } else {
            textPaint.textSize = w * 0.046f
            textPaint.textAlign = align
            textPaint.alpha = a
            canvas.drawText(card.title, cx, cy + riseOffset, textPaint)
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
