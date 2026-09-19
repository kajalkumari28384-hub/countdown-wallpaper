package com.countdown.wallpaper

import android.app.Activity
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class ManageContentActivity : Activity() {

    private lateinit var listContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
        }
        val scroll = ScrollView(this)
        listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 140, 48, 80)
        }
        scroll.addView(listContainer)
        root.addView(scroll)
        setContentView(root)

        rebuildList()
    }

    private fun rebuildList() {
        listContainer.removeAllViews()

        listContainer.addView(sectionTitle("Manage Content"))

        listContainer.addView(Button(this).apply {
            text = "Restore all removed defaults"
            setOnClickListener {
                MotivationLayer.restoreAllDefaults(this@ManageContentActivity)
                Toast.makeText(this@ManageContentActivity, "Defaults restored", Toast.LENGTH_SHORT).show()
                rebuildList()
            }
        })

        listContainer.addView(sectionTitle("\nDefault cards"))
        val hidden = MotivationLayer.getHiddenTitles(this)
        val defaults = MotivationContent.defaultCards().filter { it.title !in hidden }
        if (defaults.isEmpty()) listContainer.addView(grayText("All default cards are hidden."))
        for (card in defaults) {
            listContainer.addView(row(card.title) {
                MotivationLayer.hideTitle(this@ManageContentActivity, card.title)
                rebuildList()
            })
        }

        listContainer.addView(sectionTitle("\nYour uploaded images"))
        val dir = MotivationLayer.userImagesDir(this)
        val files = dir.listFiles { f -> f.isFile }?.sortedByDescending { it.lastModified() } ?: emptyList()
        if (files.isEmpty()) listContainer.addView(grayText("No images added yet."))
        for (file in files) {
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 16, 0, 16)
            }
            val thumb = ImageView(this)
            try {
                val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                thumb.setImageBitmap(BitmapFactory.decodeFile(file.absolutePath, opts))
            } catch (e: Exception) { }
            thumb.layoutParams = LinearLayout.LayoutParams(140, 140)
            rowLayout.addView(thumb)

            val name = TextView(this).apply {
                text = file.nameWithoutExtension
                setTextColor(Color.WHITE)
                setPadding(24, 0, 24, 0)
            }
            rowLayout.addView(name, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            rowLayout.addView(Button(this).apply {
                text = "Delete"
                setOnClickListener {
                    file.delete()
                    Toast.makeText(this@ManageContentActivity, "Image removed", Toast.LENGTH_SHORT).show()
                    rebuildList()
                }
            })
            listContainer.addView(rowLayout)
        }
    }

    private fun sectionTitle(text: String) = TextView(this).apply {
        this.text = text; setTextColor(Color.WHITE); textSize = 18f
    }

    private fun grayText(text: String) = TextView(this).apply {
        this.text = text; setTextColor(Color.GRAY); textSize = 14f; setPadding(0, 12, 0, 12)
    }

    private fun row(title: String, onRemove: () -> Unit): LinearLayout {
        val rowLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 16, 0, 16)
        }
        rowLayout.addView(TextView(this).apply {
            text = title; setTextColor(Color.WHITE)
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        rowLayout.addView(Button(this).apply {
            text = "✕"
            setOnClickListener { onRemove() }
        })
        return rowLayout
    }
}
