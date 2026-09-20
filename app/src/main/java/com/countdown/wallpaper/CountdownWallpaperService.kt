package com.countdown.wallpaper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.sin

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
        private val phasePaint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        private val drawRunnable = object : Runnable {
            override fun run() {
                drawFrame()
                if (visible) handler.postDelayed(this, 180L)
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

        private fun computeEnergy(now: Long): Triple<Int, Float, Int> {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
            cal.timeInMillis = now
            val minutesNow = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            var sinceStart = minutesNow - 6 * 60
            if (sinceStart < 0) sinceStart += 1440

            val phaseLength = 480
            val withinPhase = sinceStart % phaseLength
            val minutesLeft = phaseLength - withinPhase
            val fill = 1f - (withinPhase.toFloat() / phaseLength)

            val color = when {
                sinceStart < 480 -> Color.parseColor("#43A047")
                sinceStart < 960 -> Color.parseColor("#FDD835")
                else -> Color.parseColor("#E53935")
            }
            return Triple(color, fill.coerceIn(0f, 1f), minutesLeft)
        }

        private fun formatMinutes(mins: Int): String {
            val h = mins / 60
            val m = mins % 60
            return if (h > 0) "${h}h ${m}m left" else "${m}m left"
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

            val (energyColor, energyFill, minutesLeft) = computeEnergy(now)
            val barWidth = w * 0.78f
            val barHeight = h * 0.016f
            val barLeft = (w - barWidth) / 2f
            val barTop = centerY + labelTextSize * 4.6f
            val barCenterY = barTop + barHeight / 2f
            val barRadius = barHeight / 2f

            canvas.drawRoundRect(RectF(barLeft, barTop, barLeft + barWidth, barTop + barHeight), barRadius, barRadius, trackPaint)

            val fillWidth = barWidth * energyFill
            if (fillWidth > 1f) {
                fillPaint.color = energyColor
                val waveLengthPx = h * 0.05f
                val amplitude = barHeight * 0.32f
                val phase = (now % 4000L).toFloat() / 4000f * (Math.PI.toFloat() * 2f)
                val step = (h * 0.006f).coerceAtLeast(3f)

                val topPoints = ArrayList<Pair<Float, Float>>()
                val bottomPoints = ArrayList<Pair<Float, Float>>()
                var xx = 0f
                while (xx <= fillWidth) {
                    val angle = (xx / waveLengthPx) + phase
                    val wob = amplitude * sin(angle.toDouble()).toFloat()
                    topPoints.add(Pair(barLeft + xx, barCenterY - barHeight / 2f - wob))
                    bottomPoints.add(Pair(barLeft + xx, barCenterY + barHeight / 2f - wob))
                    xx += step
                }
                if (topPoints.isNotEmpty()) {
                    val wavePath = Path()
                    wavePath.moveTo(topPoints[0].first, topPoints[0].second)
                    for (pt in topPoints) wavePath.lineTo(pt.first, pt.second)
                    for (pt in bottomPoints.asReversed()) wavePath.lineTo(pt.first, pt.second)
                    wavePath.close()
                    canvas.save()
                    canvas.clipRect(barLeft, barTop - amplitude - 4f, barLeft + barWidth, barTop + barHeight + amplitude + 4f)
                    canvas.drawPath(wavePath, fillPaint)
                    canvas.restore()
                }
            }

            phasePaint.textSize = w * 0.026f
            phasePaint.color = Color.argb(200, Color.red(energyColor), Color.green(energyColor), Color.blue(energyColor))
            val phaseTextY = barTop + barHeight + phasePaint.textSize + h * 0.014f
            canvas.drawText(formatMinutes(minutesLeft), w / 2f, phaseTextY, phasePaint)

            tickPaint.textSize = w * 0.022f
            val tickY = phaseTextY + tickPaint.textSize + h * 0.016f
            canvas.drawText("6 AM", barLeft, tickY, tickPaint)
            canvas.drawText("2 PM", barLeft + barWidth * (480f / 1440f), tickY, tickPaint)
            canvas.drawText("10 PM", barLeft + barWidth * (960f / 1440f), tickY, tickPaint)
            canvas.drawText("6 AM", barLeft + barWidth, tickY, tickPaint)

            motivationLayer.update(now)
            motivationLayer.draw(canvas, w, h, now)
        }
    }
}
