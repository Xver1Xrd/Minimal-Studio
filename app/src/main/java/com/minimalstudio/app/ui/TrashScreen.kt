package com.minimalstudio.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimalstudio.app.data.StudioCard

/** Cards in Trash keep their attachments until the user explicitly destroys them. */
@Composable
fun TrashScreen(
    cards: List<StudioCard>,
    onRestore: (String) -> Unit,
    onDestroy: (String) -> Unit,
    onEmpty: () -> Unit,
    onBack: () -> Unit
) {
    var pending: StudioCard? by remember { mutableStateOf(null) }
    var confirmAll by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        PageHeader("Корзина", "МОЖНО ВОССТАНОВИТЬ", onBack)
        if (cards.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("Корзина пуста", color = Muted, fontSize = 16.sp)
            }
        } else {
            Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${cards.size} карточек", color = Muted, fontSize = 12.sp)
                TextButton(onClick = { confirmAll = true }) {
                    Icon(Icons.Default.DeleteSweep, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Очистить корзину")
                }
            }
            LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 35.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(cards, key = { it.id }) { card ->
                    Surface(color = Surface, shape = RoundedCornerShape(19.dp)) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            CardCover(card.kind, card.coverPath, Modifier.size(66.dp))
                            Spacer(Modifier.width(13.dp))
                            Column(Modifier.weight(1f)) {
                                Text(card.title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                                Text(if (card.kind == "wish") "Хотелка" else "Идея", color = Muted, fontSize = 11.sp)
                            }
                            IconButton(onClick = { onRestore(card.id) }) {
                                Icon(Icons.Default.Restore, "Восстановить", tint = Accent)
                            }
                            IconButton(onClick = { pending = card }) {
                                Icon(Icons.Default.DeleteForever, "Удалить навсегда", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
    if (pending != null) {
        val card = pending!!
        DangerConfirmation("Удалить навсегда?", "«${card.title}» и вложения нельзя будет восстановить без ZIP-копии.",
            onConfirm = { pending = null; onDestroy(card.id) }, onDismiss = { pending = null })
    }
    if (confirmAll) {
        DangerConfirmation("Очистить всю корзину?", "Все удалённые карточки и их файлы исчезнут без возможности отмены. Рекомендуется сначала экспортировать ZIP.",
            onConfirm = { confirmAll = false; onEmpty() }, onDismiss = { confirmAll = false })
    }
}
