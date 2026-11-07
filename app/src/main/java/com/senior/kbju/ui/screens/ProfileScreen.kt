package com.senior.kbju.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import com.senior.kbju.BuildConfig
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.senior.kbju.data.model.ActivityLevel
import com.senior.kbju.data.model.Gender
import com.senior.kbju.data.model.Goal
import com.senior.kbju.ui.components.BigButton
import com.senior.kbju.ui.components.BigToggleButton
import com.senior.kbju.ui.components.ErrorText
import com.senior.kbju.ui.components.NumberField
import com.senior.kbju.ui.components.OutlineBigButton
import com.senior.kbju.ui.components.SectionTitle
import com.senior.kbju.ui.theme.AccentBlue
import com.senior.kbju.ui.theme.AccentGreen
import com.senior.kbju.ui.theme.ErrorRed
import com.senior.kbju.ui.theme.SoftGray
import com.senior.kbju.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(onFinished: () -> Unit, viewModel: ProfileViewModel = viewModel()) {
    val height by viewModel.height.collectAsState()
    val weight by viewModel.weight.collectAsState()
    val age by viewModel.age.collectAsState()
    val gender by viewModel.gender.collectAsState()
    val activityLevel by viewModel.activityLevel.collectAsState()
    val goal by viewModel.goal.collectAsState()
    val remindersEnabled by viewModel.remindersEnabled.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val saved by viewModel.saved.collectAsState()
    val isEditing by viewModel.isEditing.collectAsState()
    val backupStatus by viewModel.backupStatus.collectAsState()
    val isBackupBusy by viewModel.isBackupBusy.collectAsState()
    val crashReport by viewModel.crashReport.collectAsState()
    val availableUpdate by viewModel.availableUpdate.collectAsState()
    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsState()
    val updateCheckMessage by viewModel.updateCheckMessage.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* результат запроса разрешения */ }

    val restoreFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!json.isNullOrBlank()) {
                    viewModel.restoreBackup(json)
                }
            } catch (e: Exception) {
                viewModel.backupStatus.value = "Не удалось открыть файл: ${e.localizedMessage}"
            }
        }
    }

    // После сохранения параметров в DataStore уходим на главный экран.
    LaunchedEffect(saved) {
        if (saved) onFinished()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        crashReport?.let { report ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "⚠️ Зафиксирован сбой приложения",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ErrorRed
                    )
                    Text(
                        text = "Ранее приложение неожиданно закрылось. Вы можете отправить отчёт родным или разработчику, чтобы мы всё исправили.",
                        fontSize = 16.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BigButton(
                            text = "ОТПРАВИТЬ ОТЧЁТ",
                            onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Сбой SeniorKbju")
                                    putExtra(Intent.EXTRA_TEXT, report)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Отправить отчёт о сбое"))
                            },
                            color = ErrorRed,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = viewModel::clearCrashReport) {
                            Text(text = "Закрыть", fontSize = 16.sp, color = Color.DarkGray)
                        }
                    }
                }
            }
        }
        Text(
            text = if (isEditing) "Ваш профиль" else "Давайте познакомимся",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
        Text(text = "Мы посчитаем вашу норму калорий. Отвечайте как есть.", fontSize = 20.sp)

        NumberField(
            label = "Рост",
            value = height,
            onValueChange = { viewModel.height.value = it },
            suffix = "см"
        )
        NumberField(
            label = "Вес",
            value = weight,
            onValueChange = { viewModel.weight.value = it },
            suffix = "кг"
        )
        NumberField(
            label = "Возраст",
            value = age,
            onValueChange = { viewModel.age.value = it },
            suffix = "полных лет"
        )

        SectionTitle(text = "Пол")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigToggleButton(
                text = "Мужской",
                selected = gender == Gender.MALE,
                onClick = { viewModel.gender.value = Gender.MALE },
                modifier = Modifier.weight(1f)
            )
            BigToggleButton(
                text = "Женский",
                selected = gender == Gender.FEMALE,
                onClick = { viewModel.gender.value = Gender.FEMALE },
                modifier = Modifier.weight(1f)
            )
        }

        SectionTitle(text = "Как вы проводите день?")
        ActivityLevel.entries.forEach { level ->
            BigToggleButton(
                text = level.label,
                selected = activityLevel == level,
                onClick = { viewModel.activityLevel.value = level }
            )
        }

        SectionTitle(text = "Какая у вас цель?")
        Goal.entries.forEach { goalItem ->
            BigToggleButton(
                text = goalItem.label,
                selected = goal == goalItem,
                onClick = { viewModel.goal.value = goalItem }
            )
        }

        SectionTitle(text = "Напоминания")
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SoftGray)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Напоминать о еде",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "В 13:00 (обед) и 19:00 (ужин), чтобы не забыть сделать запись",
                        fontSize = 16.sp
                    )
                }
                Switch(
                    checked = remindersEnabled,
                    onCheckedChange = { checked ->
                        if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                        viewModel.setReminders(checked)
                    }
                )
            }
        }

        // Живой предпросмотр: норма пересчитывается при каждом изменении полей.
        val preview = viewModel.calculateDailyTargets()
        if (preview != null) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "Приблизительная норма:", fontSize = 20.sp)
                    Text(
                        text = "${preview.kcal} ккал в день",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Белки: ${preview.protein} г • Жиры: ${preview.fat} г • Углеводы: ${preview.carbs} г",
                        fontSize = 20.sp
                    )
                }
            }
        }

        ErrorText(errorMessage)
        BigButton(text = "РАССЧИТАТЬ И СОХРАНИТЬ", onClick = viewModel::save)

        SectionTitle(text = "Резервная копия данных")
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SoftGray),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Сохранение и перенос",
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
                            scope.launch {
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

        SectionTitle(text = "Обновление приложения")
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SoftGray),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Текущая версия: ${BuildConfig.VERSION_NAME} (сборка ${BuildConfig.VERSION_CODE})",
                    fontSize = 18.sp,
                    color = Color.DarkGray
                )

                availableUpdate?.let { update ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "🎉 Доступна новая версия: ${update.tagName}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentBlue
                            )
                            if (update.releaseNotes.isNotBlank()) {
                                Text(
                                    text = update.releaseNotes,
                                    fontSize = 16.sp
                                )
                            }
                            BigButton(
                                text = "СКАЧАТЬ ОБНОВЛЕНИЕ",
                                onClick = {
                                    val targetUrl = update.apkDownloadUrl ?: update.releaseUrl
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                                    context.startActivity(browserIntent)
                                },
                                color = AccentGreen
                            )
                        }
                    }
                }

                if (isCheckingUpdate) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.height(24.dp), strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                        Text(text = "Проверяем наличие обновлений...", fontSize = 17.sp)
                    }
                } else {
                    updateCheckMessage?.let { msg ->
                        Text(text = msg, fontSize = 16.sp, color = Color.DarkGray)
                    }

                    OutlineBigButton(
                        text = "🔄 ПРОВЕРИТЬ ОБНОВЛЕНИЯ",
                        onClick = viewModel::checkForUpdates
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
