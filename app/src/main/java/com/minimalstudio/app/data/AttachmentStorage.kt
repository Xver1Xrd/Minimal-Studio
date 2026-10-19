package com.minimalstudio.app.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

class AttachmentStorage(private val context: Context) {
    private val root: File get() = File(context.filesDir, "attachments").apply { mkdirs() }

    fun import(uri: Uri, cardId: String): StudioAttachment {
        val resolver = context.contentResolver
        var name = "Файл"
        var length = 0L
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val lengthIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: name
                if (lengthIndex >= 0 && !cursor.isNull(lengthIndex)) length = cursor.getLong(lengthIndex)
            }
        }
        val id = UUID.randomUUID().toString()
        val destination = File(root, id)
        require(length <= 200L * 1024 * 1024) { "Файл больше 200 МБ" }
        try {
            resolver.openInputStream(uri)?.use { input ->
                destination.outputStream().use { output ->
                    val buffer = ByteArray(32 * 1024)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= 200L * 1024 * 1024) { "Файл больше 200 МБ" }
                        output.write(buffer, 0, count)
                    }
                }
            } ?: error("Не удалось прочитать выбранный файл")
        } catch (ex: Exception) {
            destination.delete()
            throw ex
        }
        return StudioAttachment(
            id = id,
            cardId = cardId,
            displayName = name.take(255),
            mimeType = resolver.getType(uri) ?: "application/octet-stream",
            path = destination.absolutePath,
            sizeBytes = if (length > 0) length else destination.length()
        )
    }

    fun clone(item: StudioAttachment, toCardId: String): StudioAttachment {
        val id = UUID.randomUUID().toString()
        val destination = File(root, id)
        try {
            fileForArchive(item).copyTo(destination)
        } catch (e: Exception) {
            destination.delete()
            throw e
        }
        return item.copy(id = id, cardId = toCardId, path = destination.absolutePath)
    }

    /** Removes orphan files left by interrupted imports; only app-owned files are touched. */
    fun cleanupUnused(keep: Set<String>) {
        val expected = keep.mapTo(mutableSetOf()) { File(it).absolutePath }
        root.listFiles()?.filter { it.isFile && it.absolutePath !in expected }?.forEach { it.delete() }
    }

    fun delete(item: StudioAttachment) {
        val file = File(item.path)
        if (file.parentFile?.canonicalPath == root.canonicalPath) file.delete()
    }

    fun contentUri(item: StudioAttachment): Uri = FileProvider.getUriForFile(
        context, "${context.packageName}.fileprovider", File(item.path)
    )

    fun fileForArchive(item: StudioAttachment): File {
        val file = File(item.path)
        require(file.parentFile?.canonicalPath == root.canonicalPath) { "Некорректный путь вложения" }
        return file
    }
    fun newDestination(id: String): File = File(root, id)
    fun clear() { root.listFiles()?.forEach { it.delete() } }
}
