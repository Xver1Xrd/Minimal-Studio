package com.minimalstudio.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object CardKind {
    const val WISH = "wish"
    const val IDEA = "idea"
}

@Entity(tableName = "cards", indices = [Index("kind"), Index("updatedAt")])
data class StudioCard(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val kind: String = CardKind.WISH,
    val title: String = "",
    val description: String = "",
    val notes: String = "",
    val price: String = "",
    @ColumnInfo(defaultValue = "''") val savedAmount: String = "",
    val url: String = "",
    val collection: String = "",
    val category: String = "",
    val tags: String = "",
    val status: String = "Хочу",
    val priority: Int = 0,
    val tasksJson: String = "[]",
    val linkedIds: String = "[]",
    val coverPath: String = "",
    val favorite: Boolean = false,
    val archived: Boolean = false,
    @ColumnInfo(defaultValue = "0") val deletedAt: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "attachments", indices = [Index("cardId")])
data class StudioAttachment(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val cardId: String,
    val displayName: String,
    val mimeType: String,
    val path: String,
    val sizeBytes: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isImage: Boolean get() = mimeType.startsWith("image/")
}

data class StudioTask(val text: String, val done: Boolean = false)

fun readTasks(serialized: String): List<StudioTask> = try {
    val array = JSONArray(serialized)
    (0 until array.length()).map { index ->
        val item = array.getJSONObject(index)
        StudioTask(item.optString("text"), item.optBoolean("done"))
    }
} catch (_: Exception) { emptyList() }

fun writeTasks(tasks: List<StudioTask>): String = JSONArray().apply {
    tasks.filter { it.text.isNotBlank() }.forEach { task ->
        put(JSONObject().put("text", task.text.trim()).put("done", task.done))
    }
}.toString()

fun readLinkedIds(serialized: String): List<String> = try {
    val array = JSONArray(serialized)
    (0 until array.length()).map { array.getString(it) }
} catch (_: Exception) { emptyList() }

fun writeLinkedIds(ids: List<String>): String = JSONArray().apply {
    ids.distinct().forEach { put(it) }
}.toString()

fun StudioCard.toJson(): JSONObject = JSONObject().apply {
    put("id", id); put("kind", kind); put("title", title)
    put("description", description); put("notes", notes); put("price", price); put("savedAmount", savedAmount)
    put("url", url); put("collection", collection); put("category", category)
    put("tags", tags); put("status", status); put("priority", priority)
    put("tasksJson", tasksJson); put("linkedIds", linkedIds); put("coverPath", coverPath)
    put("favorite", favorite); put("archived", archived); put("deletedAt", deletedAt)
    put("createdAt", createdAt); put("updatedAt", updatedAt)
}

fun JSONObject.asCard(): StudioCard = StudioCard(
    id = optString("id").ifBlank { UUID.randomUUID().toString() },
    kind = optString("kind", CardKind.WISH),
    title = optString("title"),
    description = optString("description"),
    notes = optString("notes"),
    price = optString("price"),
    savedAmount = optString("savedAmount"),
    url = optString("url"),
    collection = optString("collection"),
    category = optString("category"),
    tags = optString("tags"),
    status = optString("status", "Хочу"),
    priority = optInt("priority"),
    tasksJson = optString("tasksJson", "[]"),
    linkedIds = optString("linkedIds", "[]"),
    coverPath = optString("coverPath"),
    favorite = optBoolean("favorite"),
    archived = optBoolean("archived"),
    deletedAt = optLong("deletedAt"),
    createdAt = optLong("createdAt", System.currentTimeMillis()),
    updatedAt = optLong("updatedAt", System.currentTimeMillis())
)

fun StudioAttachment.toJson(): JSONObject = JSONObject().apply {
    put("id", id); put("cardId", cardId); put("displayName", displayName)
    put("mimeType", mimeType); put("path", path); put("sizeBytes", sizeBytes)
    put("createdAt", createdAt)
}
