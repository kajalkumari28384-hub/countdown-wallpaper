package com.countdown.wallpaper

import android.app.Activity
import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.EditText
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

    private fun showEditDialog(existingText: String?, onSave: (String) -> Unit) {
        val input = EditText(this).apply {
            setText(existingText ?: "")
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            hint = "Your text"
            inputType = InputType.TYPE_CLASS_TEXT
            setPadding(28, 24, 28, 24)
        }
        AlertDialog.Builder(this)
            .setTitle(if (existingText == null) "Add new card" else "Edit card")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val text = input.text.toString().trim()
                if (text.isNotEmpty()) onSave(text)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun rebuildList() {
        listContainer.removeAllViews()

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(TextView(this).apply {
            text = "Manage Content"
            setTextColor(Color.WHITE); textSize = 26f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(addButton {
            showEditDialog(null) { text ->
                ContentStore.addCustomCard(this@ManageContentActivity, text)
                rebuildList()
            }
        })
        listContainer.addView(header)
        listContainer.addView(spacer(24))

        listContainer.addView(pillButton("Restore all removed defaults") {
            ContentStore.restoreDefaults(this@ManageContentActivity)
            Toast.makeText(this@ManageContentActivity, "Defaults restored", Toast.LENGTH_SHORT).show()
            rebuildList()
        })

        val allCards = ContentStore.getActiveCards(this)
        val defaults = allCards.filter { it.id.startsWith("d") }
        val customs = allCards.filter { it.id.startsWith("c") }

        listContainer.addView(sectionTitle("Default cards"))
        if (defaults.isEmpty()) listContainer.addView(grayText("All default cards are hidden."))
        for (card in defaults) {
            listContainer.addView(row(card))
            listContainer.addView(spacer(16))
        }

        listContainer.addView(sectionTitle("Your custom cards"))
        if (customs.isEmpty()) listContainer.addView(grayText("None added yet - tap + above to add one."))
        for (card in customs) {
            listContainer.addView(row(card))
            listContainer.addView(spacer(16))
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
            listContainer.addView(spacer(16))
        }
    }

    private fun spacer(heightPx: Int) = android.view.View(this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, heightPx)
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

    private fun addButton(onClick: () -> Unit) = TextView(this).apply {
        text = "+"
        setTextColor(Color.WHITE)
        textSize = 24f
        gravity = Gravity.CENTER
        background = cardBg(20f)
        setPadding(28, 6, 28, 10)
        isClickable = true
        setOnClickListener { onClick() }
    }

    private fun editButton(onClick: () -> Unit) = TextView(this).apply {
        text = "Edit"
        setTextColor(Color.parseColor("#8AB4F8"))
        textSize = 13f
        setPadding(20, 10, 12, 10)
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

    private fun row(card: StoredCard): LinearLayout {
        val rowLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = cardBg()
            setPadding(28, 26, 28, 26)
        }
        rowLayout.addView(TextView(this).apply {
            text = card.text; setTextColor(Color.WHITE); textSize = 14f
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        rowLayout.addView(editButton {
            showEditDialog(card.text) { newText ->
                ContentStore.editCard(this@ManageContentActivity, card.id, newText)
                rebuildList()
            }
        })
        rowLayout.addView(removeButton {
            ContentStore.deleteCard(this@ManageContentActivity, card.id)
            rebuildList()
        })
        return rowLayout
    }
}
