package com.minimalstudio.app.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import coil.compose.AsyncImage
import com.minimalstudio.app.SharedContent
import com.minimalstudio.app.data.*
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    key: String,
    card: StudioCard?,
    kind: String,
    allCards: List<StudioCard>,
    shared: SharedContent?,
    existingFiles: List<StudioAttachment>,
    onSave: (StudioCard, List<Uri>, Set<String>, (Boolean) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val draftStore = remember { DraftStore(context) }
    val draft = remember(key) { draftStore.read("draft:$key") }
    val hasDraft = draft != null
    val isWish = kind == CardKind.WISH
    val sharedLink = shared?.text?.trim()?.takeIf { it.startsWith("https://") || it.startsWith("http://") }.orEmpty()
    val linkTitle = runCatching { Uri.parse(sharedLink).host.orEmpty().removePrefix("www.") }.getOrDefault("")
    var title by remember(key) { mutableStateOf(draft?.optString("title") ?: card?.title ?: linkTitle) }
    var description by remember(key) { mutableStateOf(draft?.optString("description") ?: card?.description ?: if (sharedLink.isBlank()) shared?.text.orEmpty() else "") }
    var notes by remember(key) { mutableStateOf(draft?.optString("notes") ?: card?.notes.orEmpty()) }
    var price by remember(key) { mutableStateOf(draft?.optString("price") ?: card?.price.orEmpty()) }
    var url by remember(key) { mutableStateOf(draft?.optString("url") ?: card?.url ?: sharedLink) }
    var category by remember(key) { mutableStateOf(draft?.optString("category") ?: card?.category.orEmpty()) }
    var collection by remember(key) { mutableStateOf(draft?.optString("collection") ?: card?.collection.orEmpty()) }
    var tags by remember(key) { mutableStateOf(draft?.optString("tags") ?: card?.tags.orEmpty()) }
    var status by remember(key) { mutableStateOf(draft?.optString("status") ?: card?.status ?: if (isWish) "Хочу" else "Набросок") }
    var priority by remember(key) { mutableIntStateOf(draft?.optInt("priority") ?: card?.priority ?: 0) }
    var favorite by remember(key) { mutableStateOf(draft?.optBoolean("favorite") ?: card?.favorite ?: false) }
    var linkedIds by remember(key) { mutableStateOf(readLinkedIds(draft?.optString("linkedIds") ?: card?.linkedIds ?: "[]").toSet()) }
    var linksDialog by remember { mutableStateOf(false) }
    var taskLines by remember(key) { mutableStateOf(draft?.optString("taskLines") ?: readTasks(card?.tasksJson ?: "[]").joinToString("\n") { it.text }) }
    var pendingUris by remember(key) { mutableStateOf(shared?.uris.orEmpty()) }
    var removedIds by remember(key) { mutableStateOf(setOf<String>()) }
    var statusMenu by remember { mutableStateOf(false) }
    var confirmExit by remember { mutableStateOf(false) }
    var saving by remember(key) { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { result ->
        pendingUris = (pendingUris + result).distinct()
    }
    val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { result ->
        pendingUris = (pendingUris + result).distinct()
    }

    // Debounced draft survives process death and device restarts. Never persist external URIs.
    LaunchedEffect(key, title, description, notes, price, url, category, collection, tags, status,
        priority, favorite, taskLines, linkedIds) {
        delay(600)
        val payload = JSONObject().apply {
            put("title", title); put("description", description); put("notes", notes)
            put("price", price); put("url", url); put("category", category)
            put("collection", collection); put("tags", tags); put("status", status)
            put("priority", priority); put("favorite", favorite); put("taskLines", taskLines)
            put("linkedIds", writeLinkedIds(linkedIds.toList()))
        }
        runCatching { draftStore.write("draft:$key", payload) }
    }

    BackHandler(enabled = !saving) { confirmExit = true }
    val savedFiles = existingFiles.filterNot { it.id in removedIds }
    val statuses = if (isWish) listOf("Хочу", "Очень хочу", "Коплю", "Сравниваю", "Получено")
        else listOf("Набросок", "Развиваю", "В работе", "Заморожено", "Реализовано")

    Column(Modifier.fillMaxSize()) {
        PageHeader(
            title = if (card == null) "Новая ${if (isWish) "хотелка" else "идея"}" else "Редактировать",
            caption = if (isWish) "ПРОСТРАНСТВО ЖЕЛАНИЙ" else "ТВОРЧЕСКАЯ СТУДИЯ",
            onBack = { confirmExit = true }
        ) {
            IconButton(onClick = { favorite = !favorite }) {
                Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Избранное", tint = if (favorite) Rose else Muted)
            }
        }
        if (hasDraft) {
            Text("Восстановлен локальный черновик. Не забудь сохранить карточку.",
                color = Accent, fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp))
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(5.dp))
            StudioLabel("ОСНОВНОЕ")
            StudioTextInput(title, { title = it }, "Название *", "Например: Новая игра")
            StudioTextInput(description, { description = it }, "Краткое описание", "Что хочется сохранить?", lines = 3)
            StudioLabel("ИЗОБРАЖЕНИЯ И ФАЙЛЫ")
            if (savedFiles.isNotEmpty() || pendingUris.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(savedFiles, key = { it.id }) { attachment ->
                        EditorAttachmentTile(
                            text = attachment.displayName,
                            model = if (attachment.isImage) java.io.File(attachment.path) else null,
                            onRemove = { removedIds = removedIds + attachment.id }
                        )
                    }
                    items(pendingUris, key = { it.toString() }) { uri ->
                        val displayName = remember(uri) { runCatching { DocumentFile.fromSingleUri(context, uri)?.name ?: "Файл" }.getOrDefault("Файл") }
                        val mime = context.contentResolver.getType(uri).orEmpty()
                        EditorAttachmentTile(displayName, if (mime.startsWith("image/")) uri else null) {
                            pendingUris = pendingUris - uri
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                OutlinedButton(
                    onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(15.dp)
                ) { Icon(Icons.Default.AddPhotoAlternate, null, Modifier.size(18.dp)); Spacer(Modifier.width(7.dp)); Text("Фото", fontSize = 12.sp) }
                OutlinedButton(
                    onClick = { documentPicker.launch(arrayOf("*/*")) },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(15.dp)
                ) { Icon(Icons.Default.AttachFile, null, Modifier.size(18.dp)); Spacer(Modifier.width(7.dp)); Text("Файлы", fontSize = 12.sp) }
            }

            StudioLabel(if (isWish) "ПОДРОБНОСТИ ЖЕЛАНИЯ" else "ДЕТАЛИ ИДЕИ")
            if (isWish) StudioTextInput(price, { price = it }, "Цена и валюта", "Например: 29 990 ₽", keyboardType = KeyboardType.Text)
            StudioTextInput(url, { url = it }, "Ссылка", "https://example.com")
            StudioTextInput(category, { category = it }, "Категория", if (isWish) "Игры, путешествия, техника..." else "Приложение, вдохновение, проект...")
            StudioTextInput(collection, { collection = it }, "Коллекция", "Например: Мои проекты")
            StudioTextInput(tags, { tags = it }, "Теги", "дизайн, вдохновение, важное")
            OutlinedCard(
                onClick = { linksDialog = true },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = Surface)
            ) {
                Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Hub, null, tint = Accent)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Связанные карточки", fontSize = 13.sp)
                        Text(if (linkedIds.isEmpty()) "Привязать идеи и хотелки" else "Выбрано: ${linkedIds.size}", color = Muted, fontSize = 11.sp)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = Muted)
                }
            }

            Box {
                OutlinedCard(
                    onClick = { statusMenu = true },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = Surface)
                ) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Статус", fontSize = 11.sp, color = Muted)
                            Spacer(Modifier.height(4.dp))
                            Text(status, fontSize = 14.sp)
                        }
                        Icon(Icons.Default.KeyboardArrowDown, null, tint = Muted)
                    }
                }
                DropdownMenu(expanded = statusMenu, onDismissRequest = { statusMenu = false }) {
                    statuses.forEach { item -> DropdownMenuItem(text = { Text(item) }, onClick = { status = item; statusMenu = false }) }
                }
            }
            if (isWish) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Насколько сильно хочешь?", fontSize = 13.sp)
                        Text("$priority / 3", color = Accent, fontSize = 12.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (0..3).forEach { item ->
                            FilterChip(selected = priority == item, onClick = { priority = item }, label = { Text(if (item == 0) "Не важно" else "$item", fontSize = 11.sp) })
                        }
                    }
                }
            }
            StudioLabel("ЛИЧНОЕ ПРОСТРАНСТВО")
            StudioTextInput(notes, { notes = it }, "Заметки", "Детали, мысли, полезная информация...", lines = 5)
            if (!isWish) {
                StudioTextInput(taskLines, { taskLines = it }, "Этапы / чек-лист", "Каждый шаг с новой строки", lines = 5)
                Text("Отмечать завершённые шаги можно внутри карточки.", color = Muted, fontSize = 11.sp)
            }
            Spacer(Modifier.height(24.dp))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { confirmExit = true }, enabled = !saving, modifier = Modifier.weight(1f), shape = CircleShape) { Text("Отмена") }
            Button(
                onClick = {
                    if (saving) return@Button
                    if (title.isBlank()) {
                        Toast.makeText(context, "Укажи название карточки", Toast.LENGTH_SHORT).show()
                    } else {
                        val existingTasks = readTasks(card?.tasksJson ?: "[]").associateBy { it.text }
                        val tasks = taskLines.lines().mapNotNull { line ->
                            val text = line.trim()
                            if (text.isEmpty()) null else StudioTask(text, existingTasks[text]?.done ?: false)
                        }
                        saving = true
                        onSave(
                            (card ?: StudioCard(kind = kind, id = UUID.randomUUID().toString())).copy(
                                kind = kind, title = title.trim(), description = description.trim(),
                                notes = notes, price = price.trim(), url = url.trim(), category = category.trim(),
                                collection = collection.trim(), tags = tags.trim(), status = status,
                                priority = priority, favorite = favorite, tasksJson = writeTasks(tasks),
                                linkedIds = writeLinkedIds(linkedIds.toList())
                            ),
                            pendingUris, removedIds
                        ) { successful ->
                            if (successful) draftStore.discard("draft:$key")
                            else saving = false
                        }
                    }
                },
                enabled = !saving,
                modifier = Modifier.weight(1.4f), shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)
            ) {
                Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text(if (saving) "Сохраняю…" else "Сохранить")
            }
        }
    }

    if (linksDialog) {
        AlertDialog(
            onDismissRequest = { linksDialog = false },
            title = { Text("Связанные карточки") },
            text = {
                Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                    val other = allCards.filter { it.id != card?.id }
                    if (other.isEmpty()) Text("Сначала создай хотя бы ещё одну карточку", color = Muted)
                    other.forEach { option ->
                        Row(Modifier.fillMaxWidth().clickable {
                            linkedIds = if (option.id in linkedIds) linkedIds - option.id else linkedIds + option.id
                        }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = option.id in linkedIds,
                                onCheckedChange = { checked ->
                                    linkedIds = if (checked) linkedIds + option.id else linkedIds - option.id
                                }
                            )
                            Text(option.title, maxLines = 2, fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { linksDialog = false }) { Text("Готово") } },
            containerColor = SurfaceRaised
        )
    }

    if (confirmExit) AlertDialog(
        onDismissRequest = { confirmExit = false },
        title = { Text("Закрыть редактор?") },
        text = { Text("Несохранённые изменения будут потеряны.") },
        confirmButton = { TextButton(onClick = { confirmExit = false; onBack() }) { Text("Выйти") } },
        dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("Остаться") } },
        containerColor = SurfaceRaised
    )
}

@Composable
private fun StudioTextInput(
    value: String, onChange: (String) -> Unit, label: String, placeholder: String,
    lines: Int = 1, keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, color = Muted.copy(alpha = 0.6f)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = lines == 1,
        minLines = lines,
        maxLines = if (lines == 1) 1 else 14,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Surface, unfocusedContainerColor = Surface,
            focusedBorderColor = Accent.copy(alpha = 0.7f), unfocusedBorderColor = SurfaceRaised
        )
    )
}

@Composable
private fun EditorAttachmentTile(text: String, model: Any?, onRemove: () -> Unit) {
    Box(Modifier.width(120.dp).height(130.dp).clip(RoundedCornerShape(18.dp)).background(SurfaceRaised)) {
        if (model != null) AsyncImage(
            model = model, contentDescription = text,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        ) else Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.InsertDriveFile, null, tint = Accent, modifier = Modifier.size(30.dp))
            Spacer(Modifier.height(8.dp))
            Text(text, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 10.sp)
        }
        IconButton(
            onClick = onRemove,
            modifier = Modifier.align(Alignment.TopEnd).padding(5.dp).size(27.dp).clip(CircleShape).background(Ink.copy(alpha = 0.88f))
        ) { Icon(Icons.Default.Close, "Удалить вложение", tint = Paper, modifier = Modifier.size(17.dp)) }
    }
}
