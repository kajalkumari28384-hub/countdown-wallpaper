package com.countdown.wallpaper

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class StoredCard(val id: String, val type: CardType, val text: String)

object ContentStore {

    private const val PREFS_NAME = "motivation_prefs"
    private const val KEY_DELETED_IDS = "deleted_default_ids"
    private const val KEY_EDITED_TEXTS = "edited_default_texts"
    private const val KEY_CUSTOM_CARDS = "custom_cards"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getActiveCards(context: Context): List<StoredCard> {
        val p = prefs(context)
        val deleted = p.getStringSet(KEY_DELETED_IDS, emptySet()) ?: emptySet()
        val editedJson = p.getString(KEY_EDITED_TEXTS, "{}") ?: "{}"
        val editedObj = try { JSONObject(editedJson) } catch (e: Exception) { JSONObject() }

        val defaults = MotivationContent.defaultCards().mapIndexed { index, card ->
            val id = "d$index"
            val text = if (editedObj.has(id)) editedObj.getString(id) else card.title
            StoredCard(id, card.type, text)
        }.filter { it.id !in deleted }

        val customJson = p.getString(KEY_CUSTOM_CARDS, "[]") ?: "[]"
        val customArray = try { JSONArray(customJson) } catch (e: Exception) { JSONArray() }
        val customs = mutableListOf<StoredCard>()
        for (i in 0 until customArray.length()) {
            val o = customArray.getJSONObject(i)
            customs.add(StoredCard(o.getString("id"), CardType.TEXT, o.getString("text")))
        }

        return defaults + customs
    }

    fun deleteCard(context: Context, id: String) {
        val p = prefs(context)
        if (id.startsWith("d")) {
            val deleted = HashSet(p.getStringSet(KEY_DELETED_IDS, emptySet()) ?: emptySet())
            deleted.add(id)
            p.edit().putStringSet(KEY_DELETED_IDS, deleted).apply()
        } else {
            val customArray = try {
                JSONArray(p.getString(KEY_CUSTOM_CARDS, "[]") ?: "[]")
            } catch (e: Exception) { JSONArray() }
            val newArray = JSONArray()
            for (i in 0 until customArray.length()) {
                val o = customArray.getJSONObject(i)
                if (o.getString("id") != id) newArray.put(o)
            }
            p.edit().putString(KEY_CUSTOM_CARDS, newArray.toString()).apply()
        }
    }

    fun editCard(context: Context, id: String, newText: String) {
        val p = prefs(context)
        if (id.startsWith("d")) {
            val editedJson = p.getString(KEY_EDITED_TEXTS, "{}") ?: "{}"
            val editedObj = try { JSONObject(editedJson) } catch (e: Exception) { JSONObject() }
            editedObj.put(id, newText)
            p.edit().putString(KEY_EDITED_TEXTS, editedObj.toString()).apply()
        } else {
            val customArray = try {
                JSONArray(p.getString(KEY_CUSTOM_CARDS, "[]") ?: "[]")
            } catch (e: Exception) { JSONArray() }
            val newArray = JSONArray()
            for (i in 0 until customArray.length()) {
                val o = customArray.getJSONObject(i)
                if (o.getString("id") == id) {
                    val updated = JSONObject()
                    updated.put("id", id)
                    updated.put("text", newText)
                    newArray.put(updated)
                } else {
                    newArray.put(o)
                }
            }
            p.edit().putString(KEY_CUSTOM_CARDS, newArray.toString()).apply()
        }
    }

    fun addCustomCard(context: Context, text: String) {
        val p = prefs(context)
        val customArray = try {
            JSONArray(p.getString(KEY_CUSTOM_CARDS, "[]") ?: "[]")
        } catch (e: Exception) { JSONArray() }
        val newCard = JSONObject()
        newCard.put("id", "c" + System.currentTimeMillis())
        newCard.put("text", text)
        customArray.put(newCard)
        p.edit().putString(KEY_CUSTOM_CARDS, customArray.toString()).apply()
    }

    fun restoreDefaults(context: Context) {
        prefs(context).edit()
            .putStringSet(KEY_DELETED_IDS, emptySet())
            .putString(KEY_EDITED_TEXTS, "{}")
            .apply()
    }
}
