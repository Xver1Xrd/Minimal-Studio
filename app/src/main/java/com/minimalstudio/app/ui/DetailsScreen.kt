package com.minimalstudio.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.minimalstudio.app.StudioViewModel
import com.minimalstudio.app.data.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    card: StudioCard,
    attachments: List<StudioAttachment>,
    vm: StudioViewModel,
    linkedCards: List<StudioCard>,
    onLinkedOpen: (String) -> Unit,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var deleteConfirm by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }
    val isWish = card.kind == CardKind.WISH
    val statuses = if (isWish) listOf("Хочу", "Очень хочу", "Коплю", "Сравниваю", "Получено")
        else listOf("Набросок", "Развиваю", "В работе", "Заморожено", "Реализовано")
    val images = attachments.filter { it.isImage }
    val others = attachments.filterNot { it.isImage }
    Column(Modifier.fillMaxSize()) {
        PageHeader(
            title = "Minimal Studio", caption = if (isWish) "ХОТЕЛКА" else "ИДЕЯ", onBack = onBack
        ) {
            IconButton(onClick = onShare) { Icon(Icons.Default.Share, "Поделиться") }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Редактировать", tint = Accent) }
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(3.dp))
            CardCover(card.kind, card.coverPath, Modifier.fillMaxWidth().height(278.dp).clip(RoundedCornerShape(25.dp)))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(card.title, style = MaterialTheme.typography.headlineSmall, lineHeight = 30.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(if (isWish) "Добавлено в хотелки" else "Моя идея", color = Muted, fontSize = 12.sp)
                }
                IconButton(onClick = { vm.favorite(card) }) {
                    Icon(if (card.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Избранное", tint = if (card.favorite) Rose else Accent)
                }
            }
            if (isWish && card.price.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    StudioLabel("СТОИМОСТЬ")
                    Text(card.price, style = MaterialTheme.typography.headlineSmall, color = Accent)
                }
            }
            if (isWish) PriceTracker(card)
            if (isWish && card.price.isNotBlank() && card.savedAmount.isNotBlank()) {
                val target = card.price.digitsAmount()
                val saved = card.savedAmount.digitsAmount()
                if (target > 0) {
                    val progress by animateFloatAsState((saved.toFloat() / target).coerceIn(0f, 1f), label = "savingProgress")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StudioLabel("НАКОПЛЕНИЕ")
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(9.dp).clip(CircleShape), color = Accent, trackColor = SurfaceRaised)
                        Text("${card.savedAmount} из ${card.price} · ${(progress * 100).toInt()}%", color = Muted, fontSize = 12.sp)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                MetadataPill(card.status, Accent)
                if (card.priority >= 2 && isWish) MetadataPill("Приоритет", Rose, Icons.Default.Star)
            }
            if (card.description.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StudioLabel("ОПИСАНИЕ")
                    Text(card.description, fontSize = 15.sp, lineHeight = 24.sp)
                }
            }
            if (images.size > 1) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StudioLabel("ГАЛЕРЕЯ • ${images.size}")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(images, key = { it.id }) { image ->
                            AsyncImage(
                                model = File(image.path), contentDescription = image.displayName,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.size(145.dp).background(SurfaceRaised, RoundedCornerShape(17.dp))
                                    .clickable { context.openLocalFile(vm, image) }
                            )
                        }
                    }
                }
            }
            if (card.url.isNotBlank()) {
                OutlinedCard(
                    onClick = { context.openWebLink(card.url) },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(19.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = Surface)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Link, null, tint = Accent)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            StudioLabel("ССЫЛКА")
                            Text(card.url, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Paper, fontSize = 12.sp)
                        }
                        Icon(Icons.Default.OpenInNew, "Открыть", tint = Accent, modifier = Modifier.size(17.dp))
                    }
                }
            }
            if (card.notes.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StudioLabel("ЗАМЕТКИ")
                    Surface(color = Surface, shape = RoundedCornerShape(19.dp)) {
                        Text(card.notes, modifier = Modifier.fillMaxWidth().padding(17.dp), fontSize = 13.sp, lineHeight = 21.sp)
                    }
                }
            }
            if (linkedCards.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    StudioLabel("СВЯЗАННЫЕ КАРТОЧКИ • ${linkedCards.size}")
                    linkedCards.forEach { related ->
                        Surface(onClick = { onLinkedOpen(related.id) }, color = Surface, shape = RoundedCornerShape(15.dp)) {
                            Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (related.kind == CardKind.WISH) Icons.Default.FavoriteBorder else Icons.Default.Lightbulb, null, tint = Accent)
                                Spacer(Modifier.width(11.dp))
                                Text(related.title, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                                Icon(Icons.Default.ChevronRight, null, tint = Muted, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
            val tasks = remember(card.tasksJson) { readTasks(card.tasksJson) }
            if (tasks.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    StudioLabel("ШАГИ • ${tasks.count { it.done }}/${tasks.size}")
                    Surface(color = Surface, shape = RoundedCornerShape(19.dp)) {
                        Column(Modifier.padding(vertical = 6.dp)) {
                            tasks.forEachIndexed { index, task ->
                                Row(
                                    Modifier.fillMaxWidth().clickable { vm.task(card, index) }.padding(horizontal = 13.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(checked = task.done, onCheckedChange = { vm.task(card, index) })
                                    Text(task.text, modifier = Modifier.weight(1f), fontSize = 13.sp, color = if (task.done) Muted else Paper)
                                }
                            }
                        }
                    }
                }
            }
            if (others.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StudioLabel("ФАЙЛЫ • ${others.size}")
                    others.forEach { item ->
                        Surface(
                            onClick = { context.openLocalFile(vm, item) },
                            shape = RoundedCornerShape(16.dp), color = Surface
                        ) {
                            Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.InsertDriveFile, null, tint = Accent)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(item.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                                    Text("${item.sizeBytes / 1024} КБ", color = Muted, fontSize = 11.sp)
                                }
                                Icon(Icons.Default.OpenInNew, "Открыть файл", tint = Muted, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
            if (card.tags.isNotBlank() || card.category.isNotBlank() || card.collection.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StudioLabel("ОРГАНИЗАЦИЯ")
                    if (card.category.isNotBlank()) Text("Категория: ${card.category}", color = Muted, fontSize = 12.sp)
                    if (card.collection.isNotBlank()) Text("Коллекция: ${card.collection}", color = Muted, fontSize = 12.sp)
                    if (card.tags.isNotBlank()) Text(card.tags.split(',').joinToString("   ") { "#${it.trim().replace(' ', '_')}" }, color = Accent, fontSize = 12.sp)
                }
            }
            Box {
                OutlinedCard(
                    onClick = { statusExpanded = true },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(19.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = Surface)
                ) {
                    Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Изменить статус", color = Muted, fontSize = 11.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(card.status, fontSize = 14.sp)
                        }
                        Icon(Icons.Default.ArrowDropDown, null, tint = Accent)
                    }
                }
                DropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                    statuses.forEach { value -> DropdownMenuItem(text = { Text(value) }, onClick = { vm.status(card, value); statusExpanded = false }) }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Создано ${dateText(card.createdAt)}", fontSize = 10.sp, color = Muted)
                Text("Изменено ${dateText(card.updatedAt)}", fontSize = 10.sp, color = Muted)
            }
            HorizontalDivider(color = SurfaceRaised)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { vm.archive(card); onBack() }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Archive, null, Modifier.size(17.dp)); Spacer(Modifier.width(5.dp))
                    Text(if (card.archived) "Разархивировать" else "В архив", fontSize = 12.sp)
                }
                OutlinedButton(onClick = { deleteConfirm = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.DeleteOutline, null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(5.dp))
                    Text("Удалить", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }
    if (deleteConfirm) DangerConfirmation("Удалить «${card.title}»?", "Карточка и её вложения будут перемещены в корзину. Их можно восстановить в настройках.", onConfirm = { deleteConfirm = false; onDelete() }, onDismiss = { deleteConfirm = false })
}

private fun dateText(timestamp: Long): String = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(timestamp))

private fun String.digitsAmount(): Long = filter(Char::isDigit).toLongOrNull() ?: 0L

@Composable
private fun PriceTracker(card: StudioCard) {
    val history = remember(card.priceHistoryJson, card.price) {
        readPriceHistory(card.priceHistoryJson).ifEmpty {
            card.price.digitsAmount().takeIf { it > 0L }?.let { listOf(PricePoint(it, card.updatedAt)) }.orEmpty()
        }
    }
    if (history.isEmpty()) return
    val current = history.last().amount
    val previous = history.dropLast(1).lastOrNull()?.amount
    val average = history.map { it.amount }.average().toLong()
    val minimum = history.minOf { it.amount }
    val maximum = history.maxOf { it.amount }
    val delta = previous?.let { current - it }
    val status = when {
        history.size < 2 -> "Первая запись"
        current < average * .95 -> "Ниже средней"
        current > average * 1.05 -> "Выше средней"
        else -> "Обычная цена"
    }
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        StudioLabel("ТРЕКЕР ЦЕНЫ")
        Surface(color = Surface, shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(status, color = Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("${history.size} ${if (history.size == 1) "запись" else "записей"}", color = Muted, fontSize = 11.sp)
                    }
                    if (delta != null) {
                        val down = delta < 0
                        MetadataPill(
                            text = "${if (down) "↓" else "↑"} ${formatPriceAmount(kotlin.math.abs(delta))}",
                            color = if (down) Color(0xFF83E6C0) else Rose
                        )
                    }
                }
                PriceHistoryChart(history)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    PriceStat("МИН.", minimum)
                    PriceStat("СРЕДНЯЯ", average)
                    PriceStat("МАКС.", maximum)
                }
            }
        }
        Text("Цена добавляется в историю при сохранении изменённой стоимости.", color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun PriceHistoryChart(history: List<PricePoint>) {
    val lineColor = Accent
    Canvas(
        Modifier.fillMaxWidth().height(78.dp)
            .clip(RoundedCornerShape(14.dp)).background(SurfaceRaised)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        val min = history.minOf { it.amount }.toFloat()
        val max = history.maxOf { it.amount }.toFloat()
        val range = (max - min).takeIf { it > 0f } ?: 1f
        val points = history.mapIndexed { index, item ->
            val x = if (history.size == 1) size.width / 2f else size.width * index / (history.size - 1)
            val y = size.height - ((item.amount - min) / range * size.height)
            Offset(x, y)
        }
        points.zipWithNext().forEach { (from, to) -> drawLine(lineColor, from, to, strokeWidth = 4f, cap = StrokeCap.Round) }
        points.forEachIndexed { index, point ->
            drawCircle(if (index == points.lastIndex) Paper else lineColor.copy(alpha = .55f), radius = if (index == points.lastIndex) 5f else 3f, center = point)
        }
    }
}

@Composable
private fun PriceStat(label: String, value: Long) {
    Column {
        Text(label, color = Muted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        Text(formatPriceAmount(value), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatPriceAmount(value: Long): String = "%,d ₽".format(Locale.US, value).replace(',', ' ')
