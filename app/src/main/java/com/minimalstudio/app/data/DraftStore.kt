package com.minimalstudio.app.data

import android.content.Context
import org.json.JSONObject

/** Small crash-safe private text drafts. Attachments are imported only on explicit card save. */
class DraftStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("editor_drafts", Context.MODE_PRIVATE)
    fun read(key: String): JSONObject? = prefs.getString(key, null)?.let { runCatching { JSONObject(it) }.getOrNull() }
    fun write(key: String, draft: JSONObject) {
        // commit instead of apply so an OS kill after the debounce won't drop the last draft
        check(prefs.edit().putString(key, draft.toString()).commit()) { "Не удалось записать черновик" }
    }
    fun discard(key: String) { prefs.edit().remove(key).apply() }
}
