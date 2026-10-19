package com.minimalstudio.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import com.minimalstudio.app.data.CardKind
import kotlin.math.abs
import kotlin.math.roundToInt

val StudioShape = RoundedCornerShape(22.dp)

@Composable
fun StudioIsland(kind: String, onKind: (String) -> Unit, hasPriority: Boolean = false, modifier: Modifier = Modifier) {
    var widthPx by remember { mutableFloatStateOf(1f) }
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(if (kind == CardKind.WISH) 0f else 1f) }
    val haptic = LocalHapticFeedback.current
    val selectedFraction = if (kind == CardKind.WISH) 0f else 1f
    val idle = rememberInfiniteTransition(label = "islandIdle")
    val idleScale by idle.animateFloat(
        initialValue = 1f, targetValue = 1.018f,
        animationSpec = infiniteRepeatable(tween(1900, delayMillis = 2600)), label = "islandBreathe"
    )
    val indicatorFraction by animateFloatAsState(
        targetValue = if (dragging) dragFraction else selectedFraction,
        animationSpec = if (dragging) tween(durationMillis = 0) else spring(stiffness = Spring.StiffnessMediumLow),
        label = "islandIndicator"
    )
    val pull by animateFloatAsState(
        targetValue = if (dragging) abs(dragFraction - selectedFraction) else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "islandPull"
    )
    val islandScale by animateFloatAsState(if (dragging) 0.97f else idleScale, spring(stiffness = Spring.StiffnessMediumLow), label = "islandPress")
    val indicatorOffset = (widthPx / 2f * indicatorFraction).roundToInt()
    val dragState = rememberDraggableState { amount ->
        val beforeIdea = dragFraction >= 0.5f
        val raw = (dragFraction + amount / (widthPx / 2f)).coerceIn(0f, 1f)
        dragFraction = when {
            raw in 0.43f..0.57f -> if (amount >= 0f) 0.57f else 0.43f
            else -> raw
        }
        if (beforeIdea != (dragFraction >= 0.5f)) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    Box(
        modifier = modifier
            .width(224.dp)
            .height(60.dp)
            .clip(CircleShape)
            .background(SurfaceRaised)
            .onSizeChanged { widthPx = it.width.toFloat() }
            .graphicsLayer { scaleX = islandScale; scaleY = islandScale }
            .draggable(
                state = dragState,
                orientation = Orientation.Horizontal,
                onDragStarted = { dragging = true; dragFraction = selectedFraction },
                onDragStopped = {
                    dragging = false
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onKind(if (dragFraction >= 0.5f) CardKind.IDEA else CardKind.WISH)
                }
            )
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(4.dp)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(Accent.copy(alpha = 0.20f), Accent.copy(alpha = 0.02f))))
                .graphicsLayer { alpha = pull }
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f)
                .padding(5.dp)
                .offset { IntOffset(indicatorOffset, 0) }
                .clip(CircleShape)
                .background(Paper.copy(alpha = 1f - pull * 0.12f))
                .graphicsLayer {
                    scaleX = 1f + pull * 0.14f
                    translationY = -pull * 2f
                }
        )
        Row(Modifier.fillMaxSize()) {
            listOf(CardKind.WISH to "Хотелки", CardKind.IDEA to "Идеи").forEach { (value, title) ->
            val active = kind == value
            val fg by animateColorAsState(if (active) Ink else Muted, animationSpec = spring(), label = "islandText")
            val textScale by animateFloatAsState(if (active) 1.03f else 0.96f, spring(stiffness = Spring.StiffnessMediumLow), label = "islandTextScale")
            val iconScale by animateFloatAsState(if (active) 1.12f else 0.82f, spring(stiffness = Spring.StiffnessMediumLow), label = "islandIconScale")
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onKind(value)
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.graphicsLayer { scaleX = textScale; scaleY = textScale }) {
                    Icon(if (value == CardKind.WISH) Icons.Default.FavoriteBorder else Icons.Default.Lightbulb, null, tint = fg, modifier = Modifier.size(16.dp).graphicsLayer { scaleX = iconScale; scaleY = iconScale; rotationZ = if (active) 0f else -4f })
                    Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = fg)
                    if (hasPriority && !active) Box(Modifier.size(6.dp).clip(CircleShape).background(Rose).graphicsLayer { alpha = 0.85f + pull * 0.15f })
                }
            }
        }
        }
    }
}

@Composable
fun IconPill(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(42.dp).clip(CircleShape).background(Surface)
    ) { Icon(icon, contentDescription = label, tint = Paper, modifier = Modifier.size(20.dp)) }
}

@Composable
fun PageHeader(title: String, caption: String?, onBack: () -> Unit, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconPill(Icons.AutoMirrored.Filled.ArrowBack, "Назад", onBack)
        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
            if (!caption.isNullOrBlank()) Text(caption, fontSize = 11.sp, color = Muted)
        }
        action?.invoke()
    }
}

@Composable
fun StudioLabel(label: String, modifier: Modifier = Modifier) {
    Text(label.uppercase(), modifier = modifier, color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
fun MetadataPill(text: String, color: Color? = null, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    val resolvedColor = color ?: Accent
    Row(
        modifier = Modifier.clip(CircleShape).background(resolvedColor.copy(alpha = 0.11f)).padding(horizontal = 11.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = resolvedColor, modifier = Modifier.size(13.dp))
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = resolvedColor)
    }
}

@Composable
fun CategoryArtwork(kind: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    if (kind == CardKind.WISH) listOf(Color(0xFF38324B), Color(0xFF242634), Color(0xFF202129))
                    else listOf(Color(0xFF3A3D4B), Color(0xFF292736), Color(0xFF23222B))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (kind == CardKind.WISH) Icons.Default.FavoriteBorder else Icons.Default.Lightbulb,
            contentDescription = null,
            tint = Accent.copy(alpha = 0.85f),
            modifier = Modifier.size(42.dp)
        )
    }
}

@Composable
fun EmptyLibrary(kind: String, onAdd: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp, vertical = 65.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Box(
            Modifier.size(112.dp).clip(RoundedCornerShape(35.dp)).background(Surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (kind == CardKind.WISH) Icons.Default.FavoriteBorder else Icons.Default.AutoAwesome,
                null, tint = Accent, modifier = Modifier.size(48.dp)
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(if (kind == CardKind.WISH) "Место для твоих желаний" else "Каждая идея важна", style = MaterialTheme.typography.titleLarge)
        Text(
            if (kind == CardKind.WISH) "Сохрани вещь, игру, поездку или просто мечту. Всё, что хочется запомнить."
            else "Записывай мысли, прикладывай изображения и файлы. Идеи можно развить позже.",
            style = MaterialTheme.typography.bodyMedium,
            color = Muted
        )
        Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)) {
            Icon(Icons.Default.Add, null, Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
            Text(if (kind == CardKind.WISH) "Новая хотелка" else "Новая идея")
        }
    }
}

@Composable
fun DangerConfirmation(title: String, message: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Удалить", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
        containerColor = SurfaceRaised
    )
}
