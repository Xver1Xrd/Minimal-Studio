package com.minimalstudio.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.minimalstudio.app.data.StudioCard
import com.minimalstudio.app.data.StudioRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class StudioViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: StudioRepository = (application as StudioApplication).repository
    val cards = repository.cards
    val trash = repository.trash
    val attachments = repository.attachments
    val storage = repository.storage
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 5)

    private fun operation(errorMessage: String, task: suspend () -> Unit) {
        viewModelScope.launch {
            try { task() } catch (t: Exception) {
                messages.emit("$errorMessage: ${t.localizedMessage ?: "ошибка"}")
            }
        }
    }

    fun save(card: StudioCard, files: List<Uri>, removedIds: Set<String>, result: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repository.save(card, files, removedIds)
                result(true)
                messages.emit("Карточка сохранена")
            } catch (e: Exception) {
                result(false)
                messages.emit("Не удалось сохранить: ${e.localizedMessage ?: "ошибка"}")
            }
        }
    }
    fun delete(id: String) = operation("Не удалось переместить в корзину") {
        repository.delete(id)
        messages.emit("Карточка перемещена в корзину")
    }
    fun recover(id: String) = operation("Не удалось восстановить") {
        repository.restoreCard(id)
        messages.emit("Карточка восстановлена")
    }
    fun destroy(id: String) = operation("Не удалось удалить окончательно") { repository.permanentlyDelete(id) }
    fun emptyTrash() = operation("Не удалось очистить корзину") {
        repository.emptyTrash()
        messages.emit("Корзина очищена")
    }
    fun duplicate(card: StudioCard) = operation("Не удалось скопировать") { repository.duplicate(card) }
    fun favorite(card: StudioCard) = operation("Ошибка") { repository.favorite(card) }
    fun archive(card: StudioCard) = operation("Ошибка") { repository.archive(card) }
    fun status(card: StudioCard, value: String) = operation("Ошибка") { repository.setStatus(card, value) }
    fun task(card: StudioCard, index: Int) = operation("Ошибка") { repository.toggleTask(card, index) }
    fun export(uri: Uri) = operation("Не удалось экспортировать") {
        repository.exportBackup(uri)
        messages.emit("Резервная копия сохранена")
    }
    fun restore(uri: Uri) = operation("Не удалось импортировать") {
        repository.importBackup(uri)
        messages.emit("Данные восстановлены из архива")
    }
}
