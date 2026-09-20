package com.countdown.wallpaper

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream

class MainActivity : Activity() {

    private val PICK_IMAGE_REQUEST = 101
    private lateinit var statusValue: TextView

    private val cardColor = Color.parseColor("#1C1C1E")
    private val accentColor = Color.parseColor("#2A2A2E")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    private fun cardBg(color: Int = cardColor, radius: Float = 28f): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
        }
    }

    private fun buildUi() {
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.BLACK) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 130, 40, 60)
        }
        scroll.addView(root)

        root.addView(TextView(this).apply {
            text = "YOUV Mind"
            setTextColor(Color.WHITE)
            textSize = 30f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "2027 Countdown"
            setTextColor(Color.GRAY)
            textSize = 14f
            setPadding(0, 8, 0, 36)
        })

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 2f
        }

        topRow.addView(iconCard(
            title = "Live Wallpaper",
            subtitle = "Tap to set",
            icon = "\uD83D\uDDA5\uFE0F",
            onClick = { openWallpaperPicker() }
        ), rowParams())

        val statsCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBg()
            setPadding(36, 36, 36, 36)
        }
        statusValue = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 30f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        statsCard.addView(statusValue)
        statsCard.addView(TextView(this).apply {
            text = "Images added"
            setTextColor(Color.GRAY)
            textSize = 13f
            setPadding(0, 6, 0, 0)
        })
        topRow.addView(statsCard, rowParams(marginStart = 20))

        root.addView(topRow, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        root.addView(spacer(28))

        root.addView(actionRow("\uD83D\uDCF7", "Add Image from Gallery", "Books, reminders, to-dos") { pickImage() })
        root.addView(spacer(20))
        root.addView(actionRow("\u2699\uFE0F", "Manage Content", "Remove cards or images") {
            startActivity(Intent(this, ManageContentActivity::class.java))
        })

        setContentView(scroll)
        updateStatusText()
    }

    private fun rowParams(marginStart: Int = 0) = LinearLayout.LayoutParams(
        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
    ).apply { this.marginStart = marginStart }

    private fun spacer(heightPx: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, heightPx)
    }

    private fun iconCard(title: String, subtitle: String, icon: String, onClick: () -> Unit): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBg()
            setPadding(36, 36, 36, 36)
            isClickable = true
            setOnClickListener { onClick() }
        }
        card.addView(TextView(this).apply { text = icon; textSize = 26f })
        card.addView(TextView(this).apply {
            text = title
            setTextColor(Color.WHITE)
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 24, 0, 4)
        })
        card.addView(TextView(this).apply {
            text = subtitle
            setTextColor(Color.GRAY)
            textSize = 12f
        })
        return card
    }

    private fun actionRow(icon: String, title: String, subtitle: String, onClick: () -> Unit): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = cardBg()
            setPadding(36, 32, 36, 32)
            isClickable = true
            setOnClickListener { onClick() }
        }
        val iconBubble = LinearLayout(this).apply {
            background = cardBg(accentColor, 24f)
            gravity = Gravity.CENTER
        }
        iconBubble.addView(TextView(this).apply { text = icon; textSize = 20f })
        row.addView(iconBubble, LinearLayout.LayoutParams(100, 100))

        val textCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        textCol.addView(TextView(this).apply {
            text = title; setTextColor(Color.WHITE); textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        textCol.addView(TextView(this).apply {
            text = subtitle; setTextColor(Color.GRAY); textSize = 12f
            setPadding(0, 4, 0, 0)
        })
        row.addView(textCol, LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        ).apply { marginStart = 28 })

        row.addView(TextView(this).apply {
            text = "\u203A"; setTextColor(Color.GRAY); textSize = 22f
        })
        return row
    }

    private fun openWallpaperPicker() {
        try {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            intent.putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this, CountdownWallpaperService::class.java)
            )
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Go to Settings > Wallpaper > Live Wallpapers", Toast.LENGTH_LONG).show()
        }
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            val uri: Uri = data?.data ?: return
            saveResizedImage(uri)
        }
    }

    private fun saveResizedImage(uri: Uri) {
        try {
            val maxDim = 480
            val input = contentResolver.openInputStream(uri) ?: return
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(input, null, boundsOpts)
            input.close()

            var sample = 1
            while (boundsOpts.outWidth / sample > maxDim || boundsOpts.outHeight / sample > maxDim) sample *= 2

            val input2 = contentResolver.openInputStream(uri) ?: return
            val bitmap: Bitmap? = BitmapFactory.decodeStream(input2, null, BitmapFactory.Options().apply { inSampleSize = sample })
            input2.close()

            if (bitmap == null) {
                Toast.makeText(this, "Couldn't read that image", Toast.LENGTH_SHORT).show()
                return
            }

            val dir = MotivationLayer.userImagesDir(this)
            val file = File(dir, "img_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out) }
            Toast.makeText(this, "Added to your live wallpaper rotation", Toast.LENGTH_SHORT).show()
            updateStatusText()
        } catch (e: Exception) {
            Toast.makeText(this, "Couldn't add image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateStatusText() {
        val count = MotivationLayer.userImagesDir(this).listFiles()?.size ?: 0
        statusValue.text = count.toString()
    }
}
