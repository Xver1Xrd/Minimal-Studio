package com.minimalstudio.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.minimalstudio.app.data.CardKind
import com.minimalstudio.app.data.StudioAttachment
import com.minimalstudio.app.data.StudioCard
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    kind: String,
    cards: List<StudioCard>,
    attachments: List<StudioAttachment>,
    onKind: (String) -> Unit,
    onAdd: () -> Unit,
    onDetail: (String) -> Unit,
    onEdit: (StudioCard) -> Unit,
    onSettings: () -> Unit,
    onFavorite: (StudioCard) -> Unit,
    onArchive: (StudioCard) -> Unit,
    onDuplicate: (StudioCard) -> Unit,
    onDelete: (String) -> Unit,
    onShare: (StudioCard) -> Unit
) {
    var searchActive by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("Все") }
    var collection by remember { mutableStateOf("Все коллекции") }
    var compact by remember { mutableStateOf(false) }
    var sort by remember { mutableStateOf("Новые") }
    var sortExpanded by remember { mutableStateOf(false) }
    var actionCard by remember { mutableStateOf<StudioCard?>(null) }
    var deleting by remember { mutableStateOf<StudioCard?>(null) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    val haptic = LocalHapticFeedback.current

    val collections = remember(cards, kind) { cards.filter { it.kind == kind }.map { it.collection.trim() }.filter { it.isNotBlank() }.distinct().sorted() }
    val visible = remember(cards, kind, query, filter, collection, sort) {
        cards.asSequence()
            .filter { it.kind == kind }
            .filter { when (filter) { "Избранное" -> it.favorite && !it.archived; "Архив" -> it.archived; else -> !it.archived } }
            .filter { collection == "Все коллекции" || it.collection == collection }
            .filter { query.isBlank() || listOf(it.title, it.description, it.tags, it.collection, it.category, it.notes, it.url).any { field -> field.contains(query.trim(), ignoreCase = true) } }
            .let { list -> when (sort) {
                "А–Я" -> list.sortedBy { it.title.lowercase() }
                "Старые" -> list.sortedBy { it.createdAt }
                "Приоритет" -> list.sortedWith(compareByDescending<StudioCard> { it.priority }.thenByDescending { it.updatedAt })
                else -> list.sortedByDescending { it.updatedAt }
            } }.toList()
    }
    val allVisibleSelected = visible.isNotEmpty() && visible.all { it.id in selected }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 11.dp, start = 21.dp, end = 21.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconPill(Icons.Default.Search, "Поиск") { searchActive = !searchActive; if (!searchActive) query = "" }
                Spacer(Modifier.weight(1f))
                IconPill(Icons.Default.Tune, "Настройки", onSettings)
            }

            AnimatedVisibility(searchActive) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 10.dp),
                    singleLine = true, shape = CircleShape,
                    placeholder = { Text("Поиск по карточкам", color = Muted) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = { if (query.isNotBlank()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "Очистить") } }
                )
            }

            Row(Modifier.fillMaxWidth().padding(start = 23.dp, end = 23.dp, top = 35.dp), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    StudioLabel("ТВОЯ КОЛЛЕКЦИЯ")
                    Spacer(Modifier.height(7.dp))
                    Text(if (kind == CardKind.WISH) "Мои хотелки" else "Мои идеи", style = MaterialTheme.typography.headlineMedium, color = Paper)
                    Spacer(Modifier.height(5.dp))
                    Text("${cards.count { it.kind == kind && !it.archived }} сохранено", fontSize = 12.sp, color = Muted)
                }
                IconPill(if (compact) Icons.Default.GridView else Icons.Default.ViewAgenda, "Изменить вид") { compact = !compact }
            }

            Spacer(Modifier.height(21.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 22.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf("Все", "Избранное", "Архив")) { item ->
                    FilterChip(
                        selected = filter == item, onClick = { filter = item; selected = emptySet() },
                        label = { Text(item, fontSize = 12.sp) },
                        leadingIcon = if (item == "Избранное") {{ Icon(Icons.Default.FavoriteBorder, null, Modifier.size(14.dp)) }} else null,
                        shape = CircleShape
                    )
                }
            }

            if (collections.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 22.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    items(listOf("Все коллекции") + collections) { item ->
                        SuggestionChip(onClick = { collection = item }, label = {
                            Text(if (collection == item) "✓ $item" else item, fontSize = 11.sp)
                        }, shape = CircleShape)
                    }
                }
            }

            Row(Modifier.fillMaxWidth().padding(horizontal = 23.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (selected.isNotEmpty()) "Выбрано: ${selected.size}" else "${visible.size} карточек", color = Muted, fontSize = 11.sp, modifier = Modifier.weight(1f))
                if (selected.isNotEmpty()) {
                    TextButton(onClick = { selected = if (allVisibleSelected) emptySet() else visible.map { it.id }.toSet() }) {
                        Text(if (allVisibleSelected) "Снять" else "Выбрать все", fontSize = 12.sp)
                    }
                } else {
                    Box {
                        TextButton(onClick = { sortExpanded = true }) {
                            Text(sort, fontSize = 12.sp, color = Muted)
                            Icon(Icons.Default.ArrowDropDown, null, tint = Muted, modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                            listOf("Новые", "Старые", "А–Я", "Приоритет").forEach { item ->
                                DropdownMenuItem(text = { Text(item) }, onClick = { sort = item; sortExpanded = false })
                            }
                        }
                    }
                }
            }

            if (visible.isEmpty()) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    if (query.isNotBlank() || filter != "Все" || collection != "Все коллекции") {
                        Text("Ничего не найдено", modifier = Modifier.fillMaxWidth(), color = Muted, fontSize = 15.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    } else EmptyLibrary(kind, onAdd)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (compact) 1 else 2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 3.dp, bottom = 115.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(visible, key = { it.id }) { card ->
                        CardTile(
                            card = card,
                            cover = card.coverPath.ifBlank { attachments.firstOrNull { it.cardId == card.id && it.isImage }?.path.orEmpty() },
                            compact = compact, selected = card.id in selected,
                            onClick = {
                                if (selected.isNotEmpty()) selected = if (card.id in selected) selected - card.id else selected + card.id
                                else onDetail(card.id)
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (selected.isNotEmpty()) selected = if (card.id in selected) selected - card.id else selected + card.id
                                else actionCard = card
                            }
                        )
                    }
                }
            }
        }
        if (selected.isEmpty()) {
            FloatingActionButton(
                onClick = onAdd,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 94.dp),
                shape = CircleShape,
                containerColor = Accent, contentColor = Ink
            ) { Icon(Icons.Default.Add, "Добавить карточку", modifier = Modifier.size(27.dp)) }
        } else {
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 19.dp, vertical = 18.dp)
                    .clip(CircleShape).background(SurfaceRaised).padding(horizontal = 12.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${selected.size}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 7.dp))
                IconButton(onClick = { selected = emptySet() }) { Icon(Icons.Default.Close, "Отмена") }
                IconButton(onClick = { selected.mapNotNull { id -> cards.firstOrNull { it.id == id } }.forEach(onArchive); selected = emptySet() }) { Icon(Icons.Default.Archive, "Архивировать") }
                IconButton(onClick = { deleting = cards.firstOrNull { it.id in selected } }) { Icon(Icons.Default.DeleteOutline, "Удалить выбранные", tint = MaterialTheme.colorScheme.error) }
            }
        }
        if (selected.isEmpty()) {
            StudioIsland(
                kind = kind,
                onKind = onKind,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp)
            )
        }
    }

    if (actionCard != null) {
        val card = actionCard!!
        ModalBottomSheet(onDismissRequest = { actionCard = null }, containerColor = SurfaceRaised, dragHandle = { BottomSheetDefaults.DragHandle() }) {
            Column(Modifier.padding(horizontal = 22.dp, vertical = 8.dp)) {
                Text(card.title, style = MaterialTheme.typography.titleLarge, maxLines = 2)
                Spacer(Modifier.height(14.dp))
                ActionItem(Icons.Default.Checklist, "Выбрать") { selected = setOf(card.id); actionCard = null }
                ActionItem(Icons.Default.Edit, "Редактировать") { onEdit(card); actionCard = null }
                ActionItem(Icons.Default.Share, "Поделиться") { onShare(card); actionCard = null }
                ActionItem(if (card.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, if (card.favorite) "Убрать из избранного" else "В избранное") { onFavorite(card); actionCard = null }
                ActionItem(Icons.Default.ContentCopy, "Создать копию") { onDuplicate(card); actionCard = null }
                ActionItem(Icons.Default.Archive, if (card.archived) "Вернуть из архива" else "В архив") { onArchive(card); actionCard = null }
                ActionItem(Icons.Default.DeleteOutline, "Удалить", MaterialTheme.colorScheme.error) { deleting = card; actionCard = null }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
    if (deleting != null) {
        DangerConfirmation(
            title = if (selected.isNotEmpty()) "Удалить ${selected.size} карточек?" else "Удалить карточку?",
            message = "Карточки будут перемещены в корзину. Их можно будет восстановить через настройки.",
            onConfirm = {
                val ids = if (selected.isNotEmpty()) selected.toList() else listOf(deleting!!.id)
                ids.forEach(onDelete)
                selected = emptySet()
                deleting = null
            },
            onDismiss = { deleting = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CardTile(card: StudioCard, cover: String, compact: Boolean, selected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val shape = RoundedCornerShape(23.dp)
    val modifier = Modifier.clip(shape).background(if (selected) Color(0xFF393449) else Surface)
        .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    if (compact) {
        Row(modifier.fillMaxWidth().heightIn(min = 92.dp).padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            CardCover(card.kind, cover, Modifier.size(78.dp).clip(RoundedCornerShape(16.dp)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(card.title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                if (card.price.isNotBlank() && card.kind == CardKind.WISH) Text(card.price, color = Accent, fontSize = 12.sp)
                else Text(card.category.ifBlank { card.status }, color = Muted, fontSize = 11.sp)
            }
            if (selected) Icon(Icons.Default.CheckCircle, null, tint = Accent)
            else if (card.favorite) Icon(Icons.Default.Favorite, null, tint = Rose, modifier = Modifier.size(17.dp))
        }
    } else {
        Column(modifier.fillMaxWidth()) {
            Box {
                CardCover(card.kind, cover, Modifier.fillMaxWidth().height(159.dp))
                if (selected) Icon(Icons.Default.CheckCircle, null, tint = Accent, modifier = Modifier.align(Alignment.TopEnd).padding(9.dp))
                else if (card.favorite) Icon(Icons.Default.Favorite, null, tint = Rose, modifier = Modifier.align(Alignment.TopEnd).padding(11.dp).size(18.dp))
            }
            Column(Modifier.fillMaxWidth().padding(start = 13.dp, end = 13.dp, top = 13.dp, bottom = 15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(card.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 2, lineHeight = 18.sp, overflow = TextOverflow.Ellipsis)
                Text(if (card.kind == CardKind.WISH && card.price.isNotBlank()) card.price else card.category.ifBlank { card.status }, color = if (card.kind == CardKind.WISH && card.price.isNotBlank()) Accent else Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (card.priority >= 2) MetadataPill("Приоритет", Rose)
            }
        }
    }
}

@Composable
fun CardCover(kind: String, path: String, modifier: Modifier = Modifier) {
    if (path.isNotBlank() && File(path).exists()) {
        AsyncImage(model = File(path), contentDescription = "Обложка карточки", contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = modifier)
    } else CategoryArtwork(kind, modifier)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActionItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color = Paper, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
            .combinedClickable(onClick = onClick, onLongClick = onClick)
            .padding(horizontal = 8.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(21.dp))
        Text(label, color = tint, fontSize = 14.sp)
    }
}
