package com.minimalstudio.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.minimalstudio.app.SharedContent
import com.minimalstudio.app.StudioViewModel
import com.minimalstudio.app.data.CardKind
import com.minimalstudio.app.data.StudioAttachment
import com.minimalstudio.app.data.StudioCard
import com.minimalstudio.app.util.ExternalLinks
import kotlinx.coroutines.flow.collect

sealed interface StudioRoute {
    data object Home : StudioRoute
    data object Settings : StudioRoute
    data object Trash : StudioRoute
    data class Details(val id: String) : StudioRoute
    data class Editor(val id: String?, val kind: String, val shared: SharedContent? = null) : StudioRoute
}

@Composable
fun StudioApp(vm: StudioViewModel, shared: SharedContent?, theme: ThemeConfig, onThemeChange: (ThemeConfig) -> Unit, onShareHandled: () -> Unit) {
    val cards by vm.cards.collectAsState(initial = emptyList())
    val attachments by vm.attachments.collectAsState(initial = emptyList())
    val trash by vm.trash.collectAsState(initial = emptyList())
    var route by remember { mutableStateOf<StudioRoute>(StudioRoute.Home) }
    var kind by remember { mutableStateOf(CardKind.WISH) }
    val snackbarHost = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(vm) { vm.messages.collect { snackbarHost.showSnackbar(it) } }
    LaunchedEffect(shared?.id) {
        if (shared != null) {
            val suggested = if (shared.uris.isNotEmpty() && shared.text.isBlank()) CardKind.IDEA
                else if (shared.text.trim().startsWith("http")) CardKind.WISH else CardKind.IDEA
            kind = suggested
            route = StudioRoute.Editor(null, suggested, shared)
            onShareHandled()
        }
    }

    BackHandler(enabled = route != StudioRoute.Home && route !is StudioRoute.Editor) {
        route = StudioRoute.Home
    }

    Scaffold(
        containerColor = Ink,
        snackbarHost = { SnackbarHost(snackbarHost) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).statusBarsPadding().navigationBarsPadding()) {
            when (val current = route) {
                StudioRoute.Home -> HomeScreen(
                    kind = kind,
                    cards = cards,
                    attachments = attachments,
                    onKind = { kind = it },
                    onAdd = { route = StudioRoute.Editor(null, kind) },
                    onDetail = { route = StudioRoute.Details(it) },
                    onEdit = { route = StudioRoute.Editor(it.id, it.kind) },
                    onSettings = { route = StudioRoute.Settings },
                    onFavorite = vm::favorite,
                    onArchive = vm::archive,
                    onDuplicate = vm::duplicate,
                    onDelete = vm::delete,
                    onShare = { context.shareCard(it) }
                )
                is StudioRoute.Details -> {
                    val card = cards.firstOrNull { it.id == current.id }
                    if (card != null) DetailsScreen(
                        card = card,
                        attachments = attachments.filter { it.cardId == current.id },
                        vm = vm,
                        linkedCards = cards.filter { it.id in com.minimalstudio.app.data.readLinkedIds(card.linkedIds) },
                        onLinkedOpen = { route = StudioRoute.Details(it) },
                        onBack = { route = StudioRoute.Home },
                        onEdit = { route = StudioRoute.Editor(card.id, card.kind) },
                        onShare = { context.shareCard(card) },
                        onDelete = { vm.delete(card.id); route = StudioRoute.Home }
                    ) else LaunchedEffect(current.id, cards) { route = StudioRoute.Home }
                }
                is StudioRoute.Editor -> {
                    EditorScreen(
                        key = current.id ?: current.shared?.id?.toString() ?: "new-${current.kind}",
                        card = cards.firstOrNull { it.id == current.id },
                        kind = current.kind,
                        allCards = cards,
                        shared = current.shared,
                        existingFiles = attachments.filter { it.cardId == current.id },
                        onSave = { card, newFiles, removed, result ->
                            vm.save(card, newFiles, removed) { successful ->
                                result(successful)
                                if (successful) {
                                    kind = card.kind
                                    route = StudioRoute.Home
                                }
                            }
                        },
                        onBack = { route = StudioRoute.Home }
                    )
                }
                StudioRoute.Settings -> SettingsScreen(
                    vm = vm,
                    total = cards.size,
                    trashed = trash.size,
                    theme = theme,
                    onThemeChange = onThemeChange,
                    onTrash = { route = StudioRoute.Trash },
                    onBack = { route = StudioRoute.Home }
                )
                StudioRoute.Trash -> TrashScreen(
                    cards = trash,
                    onRestore = vm::recover,
                    onDestroy = vm::destroy,
                    onEmpty = vm::emptyTrash,
                    onBack = { route = StudioRoute.Settings }
                )
            }
        }
    }
}

fun Context.shareCard(card: StudioCard) {
    val message = buildString {
        append(card.title)
        if (card.description.isNotBlank()) append("\n\n${card.description}")
        if (card.price.isNotBlank()) append("\n${card.price}")
        if (card.url.isNotBlank()) append("\n${card.url}")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, message)
    }
    try { startActivity(Intent.createChooser(intent, "Поделиться карточкой")) }
    catch (_: ActivityNotFoundException) { Toast.makeText(this, "Нет приложения для отправки", Toast.LENGTH_SHORT).show() }
}

fun Context.openWebLink(input: String) {
    val url = ExternalLinks.safeHttpUrl(input) ?: run {
        Toast.makeText(this, "Некорректная веб-ссылка", Toast.LENGTH_SHORT).show(); return
    }
    try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    catch (_: Exception) { Toast.makeText(this, "Не удалось открыть ссылку", Toast.LENGTH_SHORT).show() }
}

fun Context.openLocalFile(vm: StudioViewModel, attachment: StudioAttachment) {
    try {
        val fileUri = vm.storage.contentUri(attachment)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, attachment.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, attachment.displayName))
    } catch (_: Exception) {
        Toast.makeText(this, "Нет приложения для этого типа файла", Toast.LENGTH_LONG).show()
    }
}
