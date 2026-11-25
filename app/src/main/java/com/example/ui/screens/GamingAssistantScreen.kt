package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenshotMonitor
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ai.GamingTacticalResult
import com.example.data.local.NexoraSettings
import com.example.ui.VisionUiState
import com.example.ui.components.NexoraGlassCard
import com.example.ui.components.NexoraSectionHeader
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonBearish
import com.example.ui.theme.DeepNavyBackground
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldBullish
import com.example.ui.theme.MutedSlateText
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceHigh
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftSlateText
import com.example.ui.theme.VioletNeon
import com.example.util.DeviceGamingTelemetry
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GamingAssistantScreen(
    settings: NexoraSettings,
    telemetry: DeviceGamingTelemetry,
    gameBitmap: Bitmap?,
    gamingVisionState: VisionUiState<GamingTacticalResult>,
    onToggleVoiceCoach: (Boolean) -> Unit,
    onLaunchFreeFire: () -> Unit,
    onPickGameScreenshot: () -> Unit,
    onCaptureGameScreen: () -> Unit,
    onAnalyzeGaming: (gameName: String, scenario: String) -> Unit,
    onSpeakText: (String) -> Unit
) {
    var gameTitle by rememberSaveable { mutableStateOf("Free Fire") }
    var situationInput by rememberSaveable { mutableStateOf("") }

    val tacticalQuickScenarios = listOf(
        "HP কমে ৪০-এর নিচে — দ্রুত Medkit ও কভার কৌশল বলো",
        "খোলা মাঠে এনিমি ফায়ার করছে — Gloo Wall পজিশনিং বলো",
        "শেষ সেফ জোন 1v2 পরিস্থিতি — সারভাইভাল স্ট্র্যাটেজি দাও",
        "স্কোয়াড রাশ আসছে — হাই-গ্রাউন্ড ডিফেন্স ও টিম কলআউট দাও",
        "Free Fire ল্যাগ কমানো ও স্মুথ FPS পারফরম্যান্স টিপস"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 680.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Fair-Play Compliance Badge
        item {
            NexoraGlassCard(accentColor = EmeraldBullish) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = "Fair-play badge",
                        tint = EmeraldBullish,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(id = R.string.gaming_fairplay_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = PureWhite
                    )
                }
            }
        }

        // Real Device Gaming Telemetry & Launch Controls
        item {
            NexoraSectionHeader(
                title = "AI Gaming Coach",
                subtitle = "বাংলা ভয়েস গেমিং কোচ ও লাইভ ডিভাইস পারফরম্যান্স",
                icon = Icons.Default.SportsEsports,
                accentColor = VioletNeon
            )
            Spacer(modifier = Modifier.height(8.dp))
            NexoraGlassCard(accentColor = VioletNeon) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Bengali Voice Gaming Coach", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                        Text(
                            "বিশ্লেষণের সাথে সাথেই বাংলায় ট্যাকটিক্যাল ভয়েস কলআউট শোনাবে",
                            style = MaterialTheme.typography.bodySmall,
                            color = SoftSlateText
                        )
                    }
                    Switch(
                        checked = settings.gamingVoiceCoachEnabled,
                        onCheckedChange = onToggleVoiceCoach,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DeepNavyBackground,
                            checkedTrackColor = ElectricCyan
                        ),
                        modifier = Modifier.testTag("gaming_voice_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Real Android Hardware Telemetry
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TelemetryBadge(
                        icon = Icons.Default.Memory,
                        label = "Free RAM",
                        value = "${telemetry.availableRamMb} MB",
                        color = if (telemetry.isLowMemory) CrimsonBearish else ElectricCyan
                    )
                    TelemetryBadge(
                        icon = Icons.Default.BatteryChargingFull,
                        label = "Battery",
                        value = "${telemetry.batteryPercent}%",
                        color = if (telemetry.batteryPercent < 20) AmberWarning else EmeraldBullish
                    )
                    TelemetryBadge(
                        icon = Icons.Default.Speed,
                        label = "Temp",
                        value = String.format(Locale.US, "%.1f°C", telemetry.batteryTempC),
                        color = if (telemetry.batteryTempC > 41f) CrimsonBearish else VioletNeon
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onLaunchFreeFire,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = DeepNavyBackground),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("launch_freefire_btn")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Launch Free Fire + Floating Coach")
                }
            }
        }

        // Game Screen Tactical Analyzer (Gemini Vision)
        item {
            NexoraGlassCard(accentColor = ElectricCyan) {
                Text("Game Screen Understanding", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                Text(
                    "গেম স্ক্রিন ক্যাপচার বা স্ক্রিনশট দিন — NEXORA AI এনিমি পজিশন, HP, Medkit ও Gloo Wall পরামর্শ দেবে",
                    style = MaterialTheme.typography.bodySmall,
                    color = SoftSlateText
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onPickGameScreenshot,
                        colors = ButtonDefaults.buttonColors(containerColor = VioletNeon, contentColor = PureWhite),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("pick_game_screenshot_btn")
                    ) {
                        Icon(Icons.Default.Image, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick Screen")
                    }
                    OutlinedButton(
                        onClick = onCaptureGameScreen,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("capture_game_screen_btn")
                    ) {
                        Icon(Icons.Default.ScreenshotMonitor, null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Live Capture", color = PureWhite)
                    }
                }

                if (gameBitmap != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Image(
                        bitmap = gameBitmap.asImageBitmap(),
                        contentDescription = "Selected gameplay screen",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = gameTitle,
                    onValueChange = { gameTitle = it },
                    label = { Text("Game Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = NavyBorder,
                        focusedContainerColor = NavySurface,
                        unfocusedContainerColor = NavySurface,
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = situationInput,
                    onValueChange = { situationInput = it },
                    placeholder = { Text("গেমের পরিস্থিতি লিখুন (যেমন: 30 HP, সামনে ২ এনিমি)...", color = MutedSlateText) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VioletNeon,
                        unfocusedBorderColor = NavyBorder,
                        focusedContainerColor = NavySurface,
                        unfocusedContainerColor = NavySurface,
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gaming_situation_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { onAnalyzeGaming(gameTitle, situationInput) },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = DeepNavyBackground),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("analyze_gaming_btn")
                ) {
                    Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Get Tactical AI Coaching")
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Instant Voice Callout Scenarios", style = MaterialTheme.typography.labelMedium, color = SoftSlateText)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tacticalQuickScenarios.forEach { preset ->
                        AssistChip(
                            onClick = { onAnalyzeGaming(gameTitle, preset) },
                            label = { Text(preset, style = MaterialTheme.typography.bodySmall) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = NavySurfaceHigh,
                                labelColor = PureWhite
                            )
                        )
                    }
                }
            }
        }

        // Tactical Coaching Output Cards
        when (gamingVisionState) {
            is VisionUiState.Loading -> {
                item {
                    NexoraGlassCard(accentColor = VioletNeon) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = ElectricCyan, strokeWidth = 2.5.dp, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("গেম স্ক্রিন ও ট্যাকটিক্যাল পরিস্থিতি বিশ্লেষণ করা হচ্ছে...", color = PureWhite)
                        }
                    }
                }
            }

            is VisionUiState.Error -> {
                item {
                    NexoraGlassCard(accentColor = CrimsonBearish) {
                        Text(gamingVisionState.message, style = MaterialTheme.typography.bodyMedium, color = CrimsonBearish)
                    }
                }
            }

            is VisionUiState.Success -> {
                val res = gamingVisionState.data
                item {
                    NexoraGlassCard(accentColor = ElectricCyan) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Bengali Voice Callout", style = MaterialTheme.typography.titleMedium, color = ElectricCyan)
                            IconButton(onClick = { onSpeakText(res.bengaliVoiceCallout) }) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Replay tactical voice", tint = ElectricCyan)
                            }
                        }
                        Text(res.bengaliVoiceCallout, style = MaterialTheme.typography.bodyLarge, color = PureWhite)
                    }
                }
                item {
                    TacticalAdviceCard(
                        icon = Icons.Default.Explore,
                        title = "Enemy Awareness & Direction",
                        body = "${res.enemyStatus}\nDirection: ${res.enemyDirection}",
                        accent = CrimsonBearish
                    )
                }
                item {
                    TacticalAdviceCard(
                        icon = Icons.Default.HealthAndSafety,
                        title = "HP & Medkit Alert",
                        body = res.hpAndMedkitAlert,
                        accent = EmeraldBullish
                    )
                }
                item {
                    TacticalAdviceCard(
                        icon = Icons.Default.Shield,
                        title = "Gloo Wall & Cover Advice",
                        body = res.glooWallAdvice,
                        accent = ElectricCyan
                    )
                }
                item {
                    TacticalAdviceCard(
                        icon = Icons.Default.SportsEsports,
                        title = "Survival Strategy & Performance",
                        body = "${res.survivalStrategy}\n\nPerformance Tip: ${res.performanceTip}",
                        accent = VioletNeon
                    )
                }
            }

            VisionUiState.Idle -> Unit
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun TelemetryBadge(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, color = PureWhite)
        Text(label, style = MaterialTheme.typography.labelSmall, color = SoftSlateText)
    }
}

@Composable
private fun TacticalAdviceCard(
    icon: ImageVector,
    title: String,
    body: String,
    accent: Color
) {
    NexoraGlassCard(accentColor = accent) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, color = accent)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = PureWhite)
    }
}
