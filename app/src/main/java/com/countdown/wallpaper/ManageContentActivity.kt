package com.countdown.wallpaper

import android.app.Activity
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class ManageContentActivity : Activity() {

    private lateinit var listContainer: LinearLayout
    private val cardColor = Color.parseColor("#1C1C1E")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = ScrollView(this).apply { setBackgroundColor(Color.BLACK) }
        listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 130, 40, 80)
        }
        scroll.addView(listContainer)
        setContentView(scroll)

        rebuildList()
    }

    private fun cardBg(radius: Float = 22f) = GradientDrawable().apply {
        setColor(cardColor); cornerRadius = radius
    }

    private fun rebuildList() {
        listContainer.removeAllViews()

        listContainer.addView(TextView(this).apply {
            text = "Manage Content"
            setTextColor(Color.WHITE); textSize = 26f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 28)
        })

        listContainer.addView(pillButton("Restore all removed defaults") {
            MotivationLayer.restoreAllDefaults(this@ManageContentActivity)
            Toast.makeText(this@ManageContentActivity, "Defaults restored", Toast.LENGTH_SHORT).show()
            rebuildList()
        })

        listContainer.addView(sectionTitle("Default cards"))
        val hidden = MotivationLayer.getHiddenTitles(this)
        val defaults = MotivationContent.defaultCards().filter { it.title !in hidden }
        if (defaults.isEmpty()) listContainer.addView(grayText("All default cards are hidden."))
        for (card in defaults) {
            listContainer.addView(row(card.title) {
                MotivationLayer.hideTitle(this@ManageContentActivity, card.title)
                rebuildList()
            })
            listContainer.addView(spacer())
        }

        listContainer.addView(sectionTitle("Your uploaded images"))
        val dir = MotivationLayer.userImagesDir(this)
        val files = dir.listFiles { f -> f.isFile }?.sortedByDescending { it.lastModified() } ?: emptyList()
        if (files.isEmpty()) listContainer.addView(grayText("No images added yet."))
        for (file in files) {
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = cardBg()
                setPadding(28, 20, 28, 20)
            }
            val thumb = ImageView(this)
            try {
                val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                thumb.setImageBitmap(BitmapFactory.decodeFile(file.absolutePath, opts))
            } catch (e: Exception) { }
            thumb.layoutParams = LinearLayout.LayoutParams(120, 120)
            rowLayout.addView(thumb)

            rowLayout.addView(TextView(this).apply {
                text = file.nameWithoutExtension
                setTextColor(Color.WHITE)
                setPadding(28, 0, 20, 0)
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            rowLayout.addView(removeButton {
                file.delete()
                Toast.makeText(this@ManageContentActivity, "Image removed", Toast.LENGTH_SHORT).show()
                rebuildList()
            })
            listContainer.addView(rowLayout)
            listContainer.addView(spacer())
        }
    }

    private fun spacer() = android.view.View(this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 16)
    }

    private fun sectionTitle(text: String) = TextView(this).apply {
        this.text = text; setTextColor(Color.WHITE); textSize = 17f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, 36, 0, 16)
    }

    private fun grayText(text: String) = TextView(this).apply {
        this.text = text; setTextColor(Color.GRAY); textSize = 13f; setPadding(0, 8, 0, 8)
    }

    private fun pillButton(text: String, onClick: () -> Unit) = TextView(this).apply {
        this.text = text
        setTextColor(Color.WHITE)
        textSize = 14f
        gravity = Gravity.CENTER
        background = cardBg()
        setPadding(28, 30, 28, 30)
        isClickable = true
        setOnClickListener { onClick() }
    }

    private fun removeButton(onClick: () -> Unit) = TextView(this).apply {
        text = "X"
        setTextColor(Color.parseColor("#FF5C5C"))
        textSize = 18f
        setPadding(20, 10, 20, 10)
        isClickable = true
        setOnClickListener { onClick() }
    }

    private fun row(title: String, onRemove: () -> Unit): LinearLayout {
        val rowLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = cardBg()
            setPadding(28, 26, 28, 26)
        }
        rowLayout.addView(TextView(this).apply {
            text = title; setTextColor(Color.WHITE); textSize = 14f
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        rowLayout.addView(removeButton(onRemove))
        return rowLayout
    }
}
