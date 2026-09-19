package com.countdown.wallpaper

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream

class MainActivity : Activity() {

    private val PICK_IMAGE_REQUEST = 101
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            setPadding(56, 140, 56, 56)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        root.addView(TextView(this).apply {
            text = "2027 Countdown"
            setTextColor(Color.WHITE)
            textSize = 24f
            gravity = Gravity.CENTER
        })

        root.addView(Button(this).apply {
            text = "Set as Live Wallpaper"
            setOnClickListener { openWallpaperPicker() }
        }, spacedParams())

        root.addView(TextView(this).apply {
            text = "\nMotivation layer — custom images"
            setTextColor(Color.LTGRAY)
            textSize = 16f
            gravity = Gravity.CENTER
        })

        statusText = TextView(this).apply {
            setTextColor(Color.GRAY)
            textSize = 13f
            gravity = Gravity.CENTER
        }
        root.addView(statusText)
        updateStatusText()

        root.addView(Button(this).apply {
            text = "Add Image from Gallery"
            setOnClickListener { pickImage() }
        }, spacedParams())

        root.addView(Button(this).apply {
            text = "Remove All Custom Images"
            setOnClickListener { clearCustomImages() }
        }, spacedParams())

        setContentView(root)
    }

    private fun spacedParams(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 32 }

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

    private fun clearCustomImages() {
        MotivationLayer.userImagesDir(this).listFiles()?.forEach { it.delete() }
        Toast.makeText(this, "Custom images removed", Toast.LENGTH_SHORT).show()
        updateStatusText()
    }

    private fun updateStatusText() {
        val count = MotivationLayer.userImagesDir(this).listFiles()?.size ?: 0
        statusText.text = "$count custom image(s) added"
    }
}
