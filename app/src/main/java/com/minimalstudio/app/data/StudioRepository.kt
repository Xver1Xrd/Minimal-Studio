package com.minimalstudio.app.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Offline-only repository. Database updates are transactional; owned attachment files are private. */
class StudioRepository(private val context: Context, private val database: StudioDatabase) {
    private val dao = database.dao()
    // Serialize file/database mutations. Without this, restoring a ZIP could race a card save.
    private val mutationLock = Mutex()
    private suspend fun <T> lockedIO(operation: suspend () -> T): T =
        mutationLock.withLock { withContext(Dispatchers.IO) { operation() } }
    val cards: Flow<List<StudioCard>> = dao.cards()
    val trash: Flow<List<StudioCard>> = dao.trashedCards()
    val attachments: Flow<List<StudioAttachment>> = dao.attachments()
    val storage = AttachmentStorage(context)

    suspend fun save(card: StudioCard, newFiles: List<Uri>, removedIds: Set<String>) = lockedIO {
        require(card.title.isNotBlank()) { "Название не может быть пустым" }
        require(card.kind == CardKind.WISH || card.kind == CardKind.IDEA) { "Неизвестный тип карточки" }
        val added = mutableListOf<StudioAttachment>()
        var removedAfterCommit: List<StudioAttachment> = emptyList()
        try {
            newFiles.distinct().forEach { added += storage.import(it, card.id) }
            val old = dao.attachmentsFor(card.id)
            val actuallyRemoved = old.filter { it.id in removedIds }
            val remaining = old.filterNot { it.id in removedIds } + added
            val cover = remaining.firstOrNull { it.isImage }?.path.orEmpty()
            database.withTransaction {
                val existing = dao.byId(card.id)
                val previousHistory = existing?.priceHistoryJson ?: card.priceHistoryJson
                val newAmount = card.price.filter(Char::isDigit).toLongOrNull() ?: 0L
                val history = readPriceHistory(previousHistory).toMutableList()
                if (newAmount > 0L && history.lastOrNull()?.amount != newAmount) {
                    history += PricePoint(newAmount, System.currentTimeMillis())
                }
                dao.putCard(card.copy(
                    createdAt = existing?.createdAt ?: card.createdAt,
                    deletedAt = existing?.deletedAt ?: 0,
                    priceHistoryJson = writePriceHistory(history),
                    coverPath = cover,
                    updatedAt = System.currentTimeMillis()
                ))
                added.forEach { dao.putAttachment(it) }
                if (actuallyRemoved.isNotEmpty()) dao.removeAttachments(actuallyRemoved.map { it.id })
            }
            removedAfterCommit = actuallyRemoved
        } catch (e: Exception) {
            added.forEach(storage::delete)
            throw e
        }
        removedAfterCommit.forEach { runCatching { storage.delete(it) } }
    }

    /** Deleting never loses data immediately; the user can restore from Trash. */
    suspend fun delete(id: String) = lockedIO {
        database.withTransaction {
            dao.byId(id)?.takeIf { it.deletedAt == 0L }?.let {
                dao.putCard(it.copy(deletedAt = System.currentTimeMillis()))
            }
        }
    }

    suspend fun restoreCard(id: String) = lockedIO {
        database.withTransaction {
            dao.byId(id)?.let { dao.putCard(it.copy(deletedAt = 0L, updatedAt = System.currentTimeMillis())) }
        }
    }

    suspend fun permanentlyDelete(id: String) = lockedIO {
        val old = dao.attachmentsFor(id)
        database.withTransaction {
            val card = dao.byId(id) ?: error("Карточка не найдена")
            require(card.deletedAt != 0L) { "Сначала перемести карточку в корзину" }
            dao.removeAttachmentsFor(id)
            dao.removeCard(id)
        }
        old.forEach(storage::delete)
    }

    suspend fun emptyTrash() = lockedIO {
        val removed = mutableListOf<StudioAttachment>()
        database.withTransaction {
            dao.trashedNow().forEach { card ->
                removed += dao.attachmentsFor(card.id)
                dao.removeAttachmentsFor(card.id)
                dao.removeCard(card.id)
            }
        }
        removed.forEach(storage::delete)
    }

    suspend fun duplicate(card: StudioCard) = lockedIO {
        val fresh = dao.byId(card.id) ?: error("Карточка не найдена")
        val id = UUID.randomUUID().toString()
        val added = mutableListOf<StudioAttachment>()
        try {
            dao.attachmentsFor(card.id).forEach { added += storage.clone(it, id) }
            database.withTransaction {
                val now = System.currentTimeMillis()
                dao.putCard(fresh.copy(
                    id = id, title = "${fresh.title} — копия",
                    linkedIds = "[]", deletedAt = 0,
                    coverPath = added.firstOrNull { it.isImage }?.path.orEmpty(),
                    createdAt = now, updatedAt = now
                ))
                added.forEach { dao.putAttachment(it) }
            }
        } catch (e: Exception) {
            added.forEach(storage::delete)
            throw e
        }
    }

    suspend fun favorite(card: StudioCard) = edit(card.id) { it.copy(favorite = !it.favorite) }
    suspend fun archive(card: StudioCard) = edit(card.id) { it.copy(archived = !it.archived) }
    suspend fun setStatus(card: StudioCard, value: String) = edit(card.id) { it.copy(status = value) }
    suspend fun toggleTask(card: StudioCard, index: Int) = edit(card.id) { current ->
        val tasks = readTasks(current.tasksJson).toMutableList()
        if (index in tasks.indices) tasks[index] = tasks[index].copy(done = !tasks[index].done)
        current.copy(tasksJson = writeTasks(tasks))
    }
    private suspend fun edit(id: String, operation: (StudioCard) -> StudioCard) = lockedIO {
        database.withTransaction {
            dao.byId(id)?.let { dao.putCard(operation(it).copy(updatedAt = System.currentTimeMillis())) }
        }
    }

    /** A complete portable ZIP with JSON metadata and the file bytes. No remote services. */
    suspend fun exportBackup(destination: Uri) = lockedIO {
        val snapshot = database.withTransaction { dao.allCards() to dao.allAttachments() }
        val cards = snapshot.first
        val files = snapshot.second
        val manifest = JSONObject().apply {
            put("format", "minimal-studio")
            put("version", 1) // Backward-compatible format with the original application
            put("exportedAt", System.currentTimeMillis())
            put("cards", JSONArray().apply { cards.forEach { put(it.toJson()) } })
            put("attachments", JSONArray().apply { files.forEach { put(it.toJson()) } })
        }
        // Refuse to write a misleading backup if any referenced attachment has disappeared.
        files.forEach { check(storage.fileForArchive(it).isFile) { "Не найдено вложение: ${it.displayName}" } }
        context.contentResolver.openOutputStream(destination, "w")?.use { stream ->
            ZipOutputStream(stream).use { zip ->
                zip.putNextEntry(ZipEntry("data.json"))
                zip.write(manifest.toString().toByteArray(Charsets.UTF_8))
                zip.closeEntry()
                files.forEach { item ->
                    zip.putNextEntry(ZipEntry("files/${item.id}"))
                    storage.fileForArchive(item).inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        } ?: error("Не удалось создать резервную копию")
    }

    /**
     * Restores into NEW filenames, keeping the old library intact until the Room commit.
     * Power loss before commit cannot invalidate existing card/file references.
     * Limits reject corrupted or malicious ZIPs without excessive memory consumption.
     */
    suspend fun importBackup(source: Uri) = lockedIO {
        val stage = File(context.cacheDir, "restore-${UUID.randomUUID()}").apply { mkdirs() }
        val created = mutableListOf<File>()
        var committed = false
        try {
            var json: String? = null
            val extracted = mutableMapOf<String, File>()
            var totalBytes = 0L
            var entryCount = 0
            context.contentResolver.openInputStream(source)?.use { stream ->
                ZipInputStream(stream).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        entryCount++
                        require(entryCount <= 4000) { "Слишком много записей ZIP" }
                        if (entry.isDirectory) { zip.closeEntry(); continue }
                        when {
                            entry.name == "data.json" -> {
                                require(json == null) { "Дублирующаяся запись data.json" }
                                val buffer = java.io.ByteArrayOutputStream()
                                copyLimited(zip, buffer, 12L * 1024 * 1024)
                                json = buffer.toString(Charsets.UTF_8.name())
                            }
                            entry.name.matches(Regex("files/[0-9a-fA-F-]{36}")) -> {
                                require(extracted.size < 3000) { "Слишком много вложений" }
                                val id = entry.name.substringAfter('/')
                                require(runCatching { UUID.fromString(id) }.isSuccess) { "Некорректный ID вложения" }
                                require(id !in extracted) { "Дублирующееся вложение" }
                                val temp = File(stage, id)
                                val copied = temp.outputStream().use { copyLimited(zip, it, 200L * 1024 * 1024) }
                                totalBytes += copied
                                require(totalBytes <= 1024L * 1024 * 1024) { "Архив превышает 1 ГБ" }
                                extracted[id] = temp
                            }
                            else -> error("Неизвестная запись в архиве: ${entry.name.take(80)}")
                        }
                        zip.closeEntry()
                    }
                }
            } ?: error("Не удалось открыть резервную копию")
            val root = JSONObject(json ?: error("В архиве отсутствует data.json"))
            require(root.optString("format") == "minimal-studio" && root.optInt("version") == 1) { "Неподдерживаемый формат резервной копии" }
            val jsonCards = root.getJSONArray("cards")
            val jsonFiles = root.getJSONArray("attachments")
            require(jsonCards.length() <= 20_000 && jsonFiles.length() <= 3000) { "Превышен лимит записей" }
            val restoredCards = (0 until jsonCards.length()).map { jsonCards.getJSONObject(it).asCard() }
            require(restoredCards.all { it.title.isNotBlank() && (it.kind == CardKind.WISH || it.kind == CardKind.IDEA) && isUuid(it.id) }) { "Неверные данные карточки" }
            require(restoredCards.map { it.id }.distinct().size == restoredCards.size) { "Дублирующиеся карточки" }
            val cardIds = restoredCards.mapTo(mutableSetOf()) { it.id }
            val attachmentIds = mutableSetOf<String>()
            val restoredFiles = (0 until jsonFiles.length()).map { i ->
                val item = jsonFiles.getJSONObject(i)
                val id = item.getString("id")
                val owner = item.getString("cardId")
                require(isUuid(id) && attachmentIds.add(id) && owner in cardIds && id in extracted) { "Повреждены данные вложений" }
                val dest = storage.newDestination(UUID.randomUUID().toString())
                extracted.getValue(id).copyTo(dest)
                created += dest
                StudioAttachment(
                    id = id, cardId = owner,
                    displayName = item.optString("displayName", "Файл").take(255),
                    mimeType = item.optString("mimeType", "application/octet-stream").take(150),
                    path = dest.absolutePath,
                    sizeBytes = dest.length(),
                    createdAt = item.optLong("createdAt", System.currentTimeMillis())
                )
            }
            require(attachmentIds == extracted.keys) { "Найдены файлы без описания" }
            val coverByCard = restoredFiles.filter { it.isImage }.groupBy { it.cardId }
            val normalized = restoredCards.map { it.copy(coverPath = coverByCard[it.id]?.firstOrNull()?.path.orEmpty()) }
            database.withTransaction {
                dao.removeAllAttachments()
                dao.removeAllCards()
                dao.putCards(normalized)
                dao.putAttachments(restoredFiles)
            }
            committed = true
            // Orphan cleanup is best-effort; once committed we must NEVER delete new files.
            val keep = restoredFiles.mapTo(mutableSetOf()) { it.path }
            runCatching { storage.cleanupUnused(keep) }
        } catch (e: Exception) {
            if (!committed) created.forEach { it.delete() }
            throw e
        } finally {
            stage.deleteRecursively()
        }
    }

    suspend fun cleanupUnusedFiles() = lockedIO {
        val keep = dao.allAttachments().mapTo(mutableSetOf()) { it.path }
        storage.cleanupUnused(keep)
    }

    private fun isUuid(value: String): Boolean = runCatching { UUID.fromString(value).toString().equals(value, ignoreCase = true) }.getOrDefault(false)

    /** Reads at most limit+1 bytes even when ZIP entry announces a fake compressed size. */
    private fun copyLimited(input: InputStream, output: OutputStream, limit: Long): Long {
        var total = 0L
        val buffer = ByteArray(32 * 1024)
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            total += n
            require(total <= limit) { "Слишком большой файл в архиве" }
            output.write(buffer, 0, n)
        }
        return total
    }
}
