package com.countdown.wallpaper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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

        private val drawRunnable = object : Runnable {
            override fun run() {
                drawFrame()
                if (visible) {
                    val delay = if (motivationLayer.isTransitioning()) {
                        40L
                    } else {
                        1000 - (System.currentTimeMillis() % 1000)
                    }
                    handler.postDelayed(this, delay)
                }
            }
        }

        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            if (isVisible) {
                motivationLayer.refreshUserImages()
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

            motivationLayer.update(now)
            motivationLayer.draw(canvas, w, h, now)
        }
    }
}
