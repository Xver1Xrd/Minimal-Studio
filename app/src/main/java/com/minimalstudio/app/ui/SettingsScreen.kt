package com.minimalstudio.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimalstudio.app.StudioViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(vm: StudioViewModel, total: Int, trashed: Int, onTrash: () -> Unit, onBack: () -> Unit) {
    var confirmRestore by remember { mutableStateOf<Uri?>(null) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) vm.export(uri)
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) confirmRestore = uri
    }
    Column(Modifier.fillMaxSize()) {
        PageHeader("Настройки", "МИНИМУМ ШУМА, МАКСИМУМ СМЫСЛА", onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Spacer(Modifier.height(15.dp))
            Surface(color = Surface, shape = RoundedCornerShape(25.dp)) {
                Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Icon(Icons.Default.AutoAwesome, null, tint = Accent, modifier = Modifier.size(32.dp))
                    Text("Minimal Studio", style = MaterialTheme.typography.headlineSmall)
                    Text("Твоё пространство желаний и идей", color = Muted, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    MetadataPill("$total карточек локально")
                }
            }
            StudioLabel("ХРАНЕНИЕ ДАННЫХ")
            SettingsAction(Icons.Default.DeleteOutline, "Корзина ($trashed)", "Восстановить удалённые карточки или освободить место", onTrash)
            SettingsAction(Icons.Default.FileUpload, "Создать резервную копию", "Сохранить карточки, заметки, фото и файлы одним ZIP") {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                exporter.launch("minimal-studio-$date.zip")
            }
            SettingsAction(Icons.Default.FileDownload, "Восстановить из архива", "Заменить текущую библиотеку данными из ZIP") {
                importer.launch(arrayOf("application/zip", "application/octet-stream"))
            }
            StudioLabel("О ПРИЛОЖЕНИИ")
            Surface(color = Surface, shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Локально и конфиденциально", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                    Text("Карточки и вложения хранятся на этом устройстве. Регистрация и интернет не нужны. Данные из корзины тоже входят в резервную копию. Для переноса на другой телефон создай ZIP-копию.", color = Muted, fontSize = 12.sp, lineHeight = 19.sp)
                    HorizontalDivider(color = SurfaceRaised)
                    Text("Версия 1.1.0", color = Muted, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
    if (confirmRestore != null) {
        AlertDialog(
            onDismissRequest = { confirmRestore = null },
            title = { Text("Восстановить библиотеку?") },
            text = { Text("Текущие карточки и файлы будут заменены содержимым архива. Сначала создай резервную копию важных данных.") },
            confirmButton = {
                TextButton(onClick = { confirmRestore?.let(vm::restore); confirmRestore = null }) { Text("Восстановить") }
            },
            dismissButton = { TextButton(onClick = { confirmRestore = null }) { Text("Отмена") } },
            containerColor = SurfaceRaised
        )
    }
}

@Composable
private fun SettingsAction(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Surface, shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(15.dp)) {
            Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontSize = 14.sp, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, fontSize = 11.sp, color = Muted, lineHeight = 16.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Muted, modifier = Modifier.size(18.dp))
        }
    }
}
