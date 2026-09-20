package com.countdown.wallpaper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import java.util.Calendar
import java.util.TimeZone

class CountdownWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = CountdownEngine()

    private inner class CountdownEngine : Engine() {

        private val handler = Handler(Looper.getMainLooper())
        private var visible = false
        private val motivationLayer = MotivationLayer(this@CountdownWallpaperService)

        private val targetMillis: Long by lazy {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
            cal.set(2027, Calendar.JANUARY, 1, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }

        private val bgPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }

        private val numberPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        private val labelPaint = Paint().apply {
            color = Color.GRAY
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            letterSpacing = 0.15f
        }

        private val trackPaint = Paint().apply { isAntiAlias = true; color = Color.argb(45, 255, 255, 255) }
        private val fillPaint = Paint().apply { isAntiAlias = true }
        private val tickPaint = Paint().apply {
            isAntiAlias = true
            color = Color.GRAY
            textAlign = Paint.Align.CENTER
        }

        private val drawRunnable = object : Runnable {
            override fun run() {
                drawFrame()
                if (visible) {
                    val delay = if (motivationLayer.isTransitioning()) 40L
                    else 1000 - (System.currentTimeMillis() % 1000)
                    handler.postDelayed(this, delay)
                }
            }
        }

        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            if (isVisible) {
                motivationLayer.refreshContent()
                handler.removeCallbacks(drawRunnable)
                handler.post(drawRunnable)
            } else {
                handler.removeCallbacks(drawRunnable)
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            visible = false
            handler.removeCallbacks(drawRunnable)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            if (visible) drawFrame()
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) render(canvas)
            } finally {
                if (canvas != null) holder.unlockCanvasAndPost(canvas)
            }
        }

        /**
         * Each 8-hour phase is its own "tank": full at the start of that
         * phase, drains to empty by the end of it, then the next phase
         * resets to full with its own color. (IST)
         *   6am-2pm  -> green,  drains over those 8 hours
         *   2pm-10pm -> yellow, drains over those 8 hours
         *   10pm-6am -> red,    drains over those 8 hours
         */
        private fun computeEnergy(now: Long): Pair<Int, Float> {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
            cal.timeInMillis = now
            val minutesNow = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            var sinceStart = minutesNow - 6 * 60
            if (sinceStart < 0) sinceStart += 1440

            val phaseLength = 480f
            return when {
                sinceStart < 480 -> {
                    val fill = 1f - (sinceStart / phaseLength)
                    Pair(Color.parseColor("#43A047"), fill.coerceIn(0f, 1f))
                }
                sinceStart < 960 -> {
                    val fill = 1f - ((sinceStart - 480) / phaseLength)
                    Pair(Color.parseColor("#FDD835"), fill.coerceIn(0f, 1f))
                }
                else -> {
                    val fill = 1f - ((sinceStart - 960) / phaseLength)
                    Pair(Color.parseColor("#E53935"), fill.coerceIn(0f, 1f))
                }
            }
        }

        private fun render(canvas: Canvas) {
            val w = canvas.width.toFloat()
            val h = canvas.height.toFloat()
            val now = System.currentTimeMillis()

            canvas.drawRect(0f, 0f, w, h, bgPaint)

            val remaining = (targetMillis - now).coerceAtLeast(0L)
            val totalSeconds = remaining / 1000
            val days = totalSeconds / 86400
            val hours = (totalSeconds % 86400) / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60

            val values = arrayOf(
                days.toString(),
                hours.toString().padStart(2, '0'),
                minutes.toString().padStart(2, '0'),
                seconds.toString().padStart(2, '0')
            )
            val labels = arrayOf("DAYS", "HOURS", "MINUTES", "SECONDS")

            val numberTextSize = w * 0.11f
            val labelTextSize = w * 0.028f
            numberPaint.textSize = numberTextSize
            labelPaint.textSize = labelTextSize

            val columnWidth = w / values.size
            val centerY = h / 2f

            for (i in values.indices) {
                val cx = columnWidth * i + columnWidth / 2f
                canvas.drawText(values[i], cx, centerY, numberPaint)
                canvas.drawText(labels[i], cx, centerY + labelTextSize * 2.2f, labelPaint)
            }

            // ---- Energy bar (flat colors, no glow) ----
            val (energyColor, energyFill) = computeEnergy(now)
            val barWidth = w * 0.78f
            val barHeight = h * 0.016f
            val barLeft = (w - barWidth) / 2f
            val barTop = centerY + labelTextSize * 4.6f
            val barRadius = barHeight / 2f

            canvas.drawRoundRect(RectF(barLeft, barTop, barLeft + barWidth, barTop + barHeight), barRadius, barRadius, trackPaint)
            fillPaint.color = energyColor
            canvas.drawRoundRect(RectF(barLeft, barTop, barLeft + barWidth * energyFill, barTop + barHeight), barRadius, barRadius, fillPaint)

            tickPaint.textSize = w * 0.022f
            val tickY = barTop + barHeight + tickPaint.textSize + h * 0.012f
            canvas.drawText("6 AM", barLeft, tickY, tickPaint)
            canvas.drawText("2 PM", barLeft + barWidth * (480f / 1440f), tickY, tickPaint)
            canvas.drawText("10 PM", barLeft + barWidth * (960f / 1440f), tickY, tickPaint)
            canvas.drawText("6 AM", barLeft + barWidth, tickY, tickPaint)

            motivationLayer.update(now)
            motivationLayer.draw(canvas, w, h, now)
        }
    }
}
