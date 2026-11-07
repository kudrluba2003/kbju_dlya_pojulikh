package com.senior.kbju.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.senior.kbju.BuildConfig
import com.senior.kbju.ui.components.BigButton
import com.senior.kbju.ui.components.OutlineBigButton
import com.senior.kbju.ui.components.SectionTitle
import com.senior.kbju.ui.theme.AccentBlue
import com.senior.kbju.ui.theme.AccentGreen
import com.senior.kbju.ui.theme.ErrorRed
import com.senior.kbju.ui.theme.SoftGray
import com.senior.kbju.utils.AppUpdateChecker
import com.senior.kbju.utils.AppUpdateInstaller
import com.senior.kbju.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val availableUpdate by viewModel.availableUpdate.collectAsState()
    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsState()
    val updateCheckMessage by viewModel.updateCheckMessage.collectAsState()
    val isSendingFeedback by viewModel.isSendingFeedback.collectAsState()
    val feedbackSentMessage by viewModel.feedbackSentMessage.collectAsState()
    val backupStatus by viewModel.backupStatus.collectAsState()
    val isBackupBusy by viewModel.isBackupBusy.collectAsState()

    val remindersEnabled by viewModel.remindersEnabled.collectAsState()
    val breakfastHour by viewModel.breakfastHour.collectAsState()
    val lunchHour by viewModel.lunchHour.collectAsState()
    val dinnerHour by viewModel.dinnerHour.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var downloadState by remember { mutableStateOf<AppUpdateInstaller.DownloadState>(AppUpdateInstaller.DownloadState.Idle) }
    var feedbackInput by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* результат запроса разрешения */ }

    val restoreFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val json = context.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader().readText()
                    }
                    if (json != null) {
                        viewModel.restoreBackup(json)
                    }
                } catch (e: Exception) {
                    viewModel.backupStatus.value = "Не удалось открыть файл: ${e.localizedMessage}"
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "⚙️ Настройки",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        // Баннер доступного обновления (если есть)
        availableUpdate?.let { update ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "🎉 Доступно обновление v${update.tagName}!",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGreen
                    )
                    Text(
                        text = "Новая версия с улучшениями готова к установке прямо в приложении.",
                        fontSize = 17.sp
                    )

                    when (val dl = downloadState) {
                        is AppUpdateInstaller.DownloadState.Idle -> {
                            BigButton(
                                text = "📥 СКАЧАТЬ И УСТАНОВИТЬ ОБНОВЛЕНИЕ",
                                onClick = {
                                    val downloadUrl = if (update.apkDownloadUrl != null && update.apkDownloadUrl.endsWith(".apk")) {
                                        update.apkDownloadUrl
                                    } else {
                                        "https://github.com/${AppUpdateChecker.GITHUB_REPO}/releases/download/v${update.tagName}/app-release.apk"
                                    }
                                    coroutineScope.launch {
                                        AppUpdateInstaller.downloadAndInstall(context, downloadUrl) { state ->
                                            downloadState = state
                                        }
                                    }
                                },
                                color = AccentGreen
                            )
                        }
                        is AppUpdateInstaller.DownloadState.Downloading -> {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (dl.percent >= 0) "Загрузка обновления: ${dl.percent}%" else "Загрузка обновления...",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentGreen
                                )
                                if (dl.percent >= 0) {
                                    LinearProgressIndicator(
                                        progress = { dl.percent / 100f },
                                        modifier = Modifier.fillMaxWidth().height(8.dp),
                                        color = AccentGreen
                                    )
                                } else {
                                    LinearProgressIndicator(
                                        modifier = Modifier.fillMaxWidth().height(8.dp),
                                        color = AccentGreen
                                    )
                                }
                            }
                        }
                        is AppUpdateInstaller.DownloadState.ReadyToInstall -> {
                            BigButton(
                                text = "✅ УСТАНОВИТЬ ОБНОВЛЕНИЕ",
                                onClick = { AppUpdateInstaller.installApk(context, dl.file) },
                                color = AccentGreen
                            )
                        }
                        is AppUpdateInstaller.DownloadState.Error -> {
                            Text(text = dl.message, fontSize = 16.sp, color = ErrorRed)
                            OutlineBigButton(
                                text = "ОТКРЫТЬ В БРАУЗЕРЕ",
                                onClick = {
                                    val url = update.apkDownloadUrl ?: update.releaseUrl
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                            )
                        }
                    }
                }
            }
        }

        // Блок: Напоминания о приёмах пищи
        SectionTitle(text = "Напоминания о приёмах пищи")
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SoftGray),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Напоминать о еде",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Уведомления о завтраке, обеде и ужине, чтобы не забыть сделать запись",
                            fontSize = 16.sp,
                            color = Color.DarkGray
                        )
                    }
                    Switch(
                        checked = remindersEnabled,
                        onCheckedChange = { checked ->
                            if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            }
                            viewModel.setRemindersEnabled(checked)
                        }
                    )
                }

                if (remindersEnabled) {
                    HorizontalDivider(thickness = 1.dp, color = Color(0xFFDDDDDD))
                    Text(text = "Время напоминаний:", fontSize = 18.sp, fontWeight = FontWeight.Bold)

                    // Завтрак
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Завтрак: $breakfastHour:00",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BigButton(
                                text = "➖ 1 ч",
                                onClick = { viewModel.adjustBreakfastHour(-1) },
                                color = Color.LightGray
                            )
                            BigButton(
                                text = "➕ 1 ч",
                                onClick = { viewModel.adjustBreakfastHour(1) },
                                color = AccentBlue
                            )
                        }
                    }

                    // Обед
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Обед: $lunchHour:00",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BigButton(
                                text = "➖ 1 ч",
                                onClick = { viewModel.adjustLunchHour(-1) },
                                color = Color.LightGray
                            )
                            BigButton(
                                text = "➕ 1 ч",
                                onClick = { viewModel.adjustLunchHour(1) },
                                color = AccentBlue
                            )
                        }
                    }

                    // Ужин
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ужин: $dinnerHour:00",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BigButton(
                                text = "➖ 1 ч",
                                onClick = { viewModel.adjustDinnerHour(-1) },
                                color = Color.LightGray
                            )
                            BigButton(
                                text = "➕ 1 ч",
                                onClick = { viewModel.adjustDinnerHour(1) },
                                color = AccentBlue
                            )
                        }
                    }
                }
            }
        }

        // Блок: Обновления приложения
        SectionTitle(text = "Обновление приложения")
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SoftGray),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Версия приложения: v${BuildConfig.VERSION_NAME}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.DarkGray
                    )
                    if (isCheckingUpdate) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                    }
                }

                OutlineBigButton(
                    text = "🔄 ПРОВЕРИТЬ ОБНОВЛЕНИЯ",
                    onClick = { viewModel.checkForUpdates(silent = false) }
                )

                updateCheckMessage?.let { msg ->
                    Text(
                        text = msg,
                        fontSize = 16.sp,
                        color = AccentBlue,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Блок: Отзыв разработчику (прямая отправка без почтового клиента)
        SectionTitle(text = "Отзыв и предложения")
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SoftGray),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "✉️ Написать разработчику",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Напишите, каких продуктов или функций вам не хватает в приложении. Нажмите кнопку «Отправить отзыв», и сообщение сразу улетит разработчику Любови Кудрявцевой без перехода в почту:",
                    fontSize = 16.sp,
                    color = Color.DarkGray
                )
                OutlinedTextField(
                    value = feedbackInput,
                    onValueChange = { feedbackInput = it },
                    placeholder = { Text("Напишите ваше пожелание...", fontSize = 17.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 18.sp),
                    shape = RoundedCornerShape(8.dp)
                )

                feedbackSentMessage?.let { msg ->
                    Text(
                        text = msg,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B5E20)
                    )
                }

                BigButton(
                    text = if (isSendingFeedback) "ОТПРАВЛЯЕМ..." else "📨 ОТПРАВИТЬ ОТЗЫВ",
                    onClick = {
                        val body = feedbackInput.trim()
                        if (body.isNotBlank() && !isSendingFeedback) {
                            viewModel.sendFeedback(body) {
                                feedbackInput = ""
                            }
                        }
                    },
                    color = AccentGreen
                )
            }
        }

        // Блок: Резервная копия
        SectionTitle(text = "Резервная копия данных")
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SoftGray),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Сохранение и перенос истории",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Сохраните всю историю питания, веса и продуктов в один файл. Его можно отправить детям в Telegram или восстановить при смене телефона.",
                    fontSize = 16.sp,
                    color = Color.DarkGray
                )

                if (isBackupBusy) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.height(24.dp), strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                        Text(text = "Обработка копии...", fontSize = 18.sp)
                    }
                } else {
                    BigButton(
                        text = "📤 ПОДЕЛИТЬСЯ РЕЗЕРВНОЙ КОПИЕЙ",
                        onClick = {
                            coroutineScope.launch {
                                val json = viewModel.getBackupJson()
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/json"
                                    putExtra(Intent.EXTRA_SUBJECT, "Резервная копия КБЖУ для старших")
                                    putExtra(Intent.EXTRA_TEXT, json)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Сохранить резервную копию"))
                            }
                        },
                        color = AccentBlue
                    )

                    OutlineBigButton(
                        text = "📥 ВОССТАНОВИТЬ ИЗ ФАЙЛА",
                        onClick = {
                            try {
                                restoreFileLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                            } catch (_: Exception) {}
                        }
                    )
                }

                backupStatus?.let { status ->
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = status,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1B5E20),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
