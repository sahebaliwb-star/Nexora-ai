package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.ScreenshotMonitor
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.ActivityLogEntity
import com.example.data.local.NexoraSettings
import com.example.ui.AssistantUiState
import com.example.ui.NexoraDestination
import com.example.ui.NexoraOrbState
import com.example.ui.components.NexoraGlassCard
import com.example.ui.components.NexoraNeuralOrb
import com.example.ui.components.NexoraSectionHeader
import com.example.ui.components.NexoraStatusPill
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeDashboardScreen(
    settings: NexoraSettings,
    orbState: NexoraOrbState,
    rmsLevel: Float,
    isListening: Boolean,
    isSpeaking: Boolean,
    partialTranscript: String,
    voiceError: String?,
    assistantState: AssistantUiState,
    floatingRunning: Boolean,
    recentActivities: List<ActivityLogEntity>,
    onVoiceTap: () -> Unit,
    onSendCommand: (String) -> Unit,
    onToggleLanguage: () -> Unit,
    onToggleFloating: (Boolean) -> Unit,
    onSpeakText: (String) -> Unit,
    onStopSpeaking: () -> Unit,
    onNavigate: (NexoraDestination) -> Unit,
    onRequestScreenCapture: () -> Unit
) {
    var commandText by rememberSaveable { mutableStateOf("") }

    val quickPrompts = listOf(
        "YouTube-এ নতুন বাংলা গান চালাও",
        "আজকের কাজের পরিকল্পনা সাজিয়ে দাও",
        "Chrome-এ আজকের মার্কেট নিউজ খোলো",
        "Free Fire-এ লাস্ট জোন সারভাইভাল টিপস দাও"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Atmospheric neural wave header artwork
        Image(
            painter = painterResource(id = R.drawable.img_nexora_neural_banner_1795614505005),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .alpha(0.28f)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(270.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            DeepNavyBackground.copy(alpha = 0.72f),
                            DeepNavyBackground
                        )
                    )
                )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Brand Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.img_nexora_logo_transparent),
                            contentDescription = "NEXORA AI Emblem",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(id = R.string.brand_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = PureWhite
                            )
                            Text(
                                text = stringResource(id = R.string.brand_tagline),
                                style = MaterialTheme.typography.bodySmall,
                                color = ElectricCyan
                            )
                        }
                    }

                    NexoraStatusPill(
                        label = if (settings.assistantEnabled) "ONLINE" else "PAUSED",
                        dotColor = if (settings.assistantEnabled) EmeraldBullish else CrimsonBearish
                    )
                }
            }

            // Centerpiece Animated AI Neural Orb
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NexoraNeuralOrb(
                        state = orbState,
                        rmsLevel = rmsLevel,
                        onTap = onVoiceTap,
                        size = 192.dp
                    )

                    AnimatedVisibility(visible = isListening && partialTranscript.isNotBlank()) {
                        Surface(
                            modifier = Modifier.padding(top = 10.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = ElectricCyan.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "\"$partialTranscript\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PureWhite,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }

                    AnimatedVisibility(visible = !voiceError.isNullOrBlank()) {
                        Text(
                            text = voiceError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = AmberWarning,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            // Voice & Text Command Input Deck
            item {
                NexoraGlassCard(accentColor = ElectricCyan) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Voice & Text Command",
                            style = MaterialTheme.typography.titleSmall,
                            color = PureWhite
                        )
                        AssistChip(
                            onClick = onToggleLanguage,
                            label = {
                                Text(
                                    text = if (settings.voiceLanguage.startsWith("bn")) "বাংলা (Female AI)" else "English (AI)",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = VioletNeon.copy(alpha = 0.18f),
                                labelColor = PureWhite
                            ),
                            modifier = Modifier.testTag("language_toggle_chip")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = commandText,
                            onValueChange = { commandText = it },
                            placeholder = {
                                Text(
                                    text = "বাংলা বা ইংরেজিতে নির্দেশ লিখুন...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MutedSlateText
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = NavyBorder,
                                focusedContainerColor = NavySurface,
                                unfocusedContainerColor = NavySurface,
                                focusedTextColor = PureWhite,
                                unfocusedTextColor = PureWhite
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (commandText.isNotBlank()) {
                                        onSendCommand(commandText)
                                        commandText = ""
                                    }
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_command_input")
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        FilledIconButton(
                            onClick = onVoiceTap,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (isListening) CrimsonBearish else VioletNeon,
                                contentColor = PureWhite
                            ),
                            modifier = Modifier
                                .size(50.dp)
                                .testTag("home_voice_button")
                        ) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = if (isListening) "Stop listening" else "Start Bengali voice command"
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        FilledIconButton(
                            onClick = {
                                if (commandText.isNotBlank()) {
                                    onSendCommand(commandText)
                                    commandText = ""
                                }
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = ElectricCyan,
                                contentColor = DeepNavyBackground
                            ),
                            modifier = Modifier
                                .size(50.dp)
                                .testTag("home_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send command"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(quickPrompts) { prompt ->
                            AssistChip(
                                onClick = { onSendCommand(prompt) },
                                label = {
                                    Text(
                                        text = prompt,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = NavySurfaceHigh,
                                    labelColor = SoftSlateText
                                )
                            )
                        }
                    }
                }
            }

            // Live AI Response Panel
            if (assistantState !is AssistantUiState.Idle) {
                item {
                    AiResponseCard(
                        state = assistantState,
                        isSpeaking = isSpeaking,
                        onSpeak = onSpeakText,
                        onStopSpeaking = onStopSpeaking
                    )
                }
            }

            // Floating AI Assistant & Screen Vision Controls
            item {
                NexoraGlassCard(accentColor = VioletNeon) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = VioletNeon.copy(alpha = 0.2f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BubbleChart,
                                    contentDescription = null,
                                    tint = VioletNeon,
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Floating AI Assistant",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = PureWhite
                                )
                                Text(
                                    text = if (floatingRunning) "সক্রিয় — অন্যান্য অ্যাপের উপর ভাসমান বাবল চালু আছে"
                                    else "অন্য অ্যাপ চালানোর সময়ও AI বাবল ব্যবহার করুন",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SoftSlateText
                                )
                            }
                        }
                        Switch(
                            checked = floatingRunning,
                            onCheckedChange = onToggleFloating,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DeepNavyBackground,
                                checkedTrackColor = ElectricCyan
                            ),
                            modifier = Modifier.testTag("floating_assistant_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onRequestScreenCapture,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NavySurfaceHigh,
                            contentColor = ElectricCyan
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("home_screen_capture_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ScreenshotMonitor,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Capture & Understand Screen (AI Vision)")
                    }
                }
            }

            // 4 Core Dashboards Grid
            item {
                NexoraSectionHeader(
                    title = "AI Command Modules",
                    subtitle = "যেকোনো স্মার্ট অ্যাসিস্ট্যান্ট মডিউল খুলুন",
                    icon = Icons.Default.AutoAwesome
                )
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ModuleTileCard(
                            title = "App Control",
                            subtitle = "YouTube, WhatsApp, Chrome & Screen AI",
                            icon = Icons.Default.Apps,
                            accent = ElectricCyan,
                            testTag = "module_app_control",
                            onClick = { onNavigate(NexoraDestination.CONTROL) },
                            modifier = Modifier.weight(1f)
                        )
                        ModuleTileCard(
                            title = "Trading AI",
                            subtitle = "Chart Analysis, Signals & Demo P/L",
                            icon = Icons.Default.CandlestickChart,
                            accent = EmeraldBullish,
                            testTag = "module_trading",
                            onClick = { onNavigate(NexoraDestination.TRADING) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ModuleTileCard(
                            title = "Gaming Coach",
                            subtitle = "Free Fire Tactics & Bengali Voice",
                            icon = Icons.Default.SportsEsports,
                            accent = VioletNeon,
                            testTag = "module_gaming",
                            onClick = { onNavigate(NexoraDestination.GAMING) },
                            modifier = Modifier.weight(1f)
                        )
                        ModuleTileCard(
                            title = "AI Settings",
                            subtitle = "Voice, Permissions, Memory & Privacy",
                            icon = Icons.Default.Settings,
                            accent = AmberWarning,
                            testTag = "module_settings",
                            onClick = { onNavigate(NexoraDestination.SETTINGS) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Recent AI Activities
            item {
                NexoraSectionHeader(
                    title = "Recent AI Activities",
                    subtitle = "সাম্প্রতিক কমান্ড ও বিশ্লেষণের তালিকা",
                    icon = Icons.Default.History,
                    accentColor = ElectricCyan
                )
            }

            if (recentActivities.isEmpty()) {
                item {
                    NexoraGlassCard(accentColor = NavyBorder) {
                        Text(
                            text = "এখনো কোনো অ্যাক্টিভিটি নেই। উপরের AI Orb-এ ট্যাপ করুন বা একটি কমান্ড লিখুন।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SoftSlateText
                        )
                    }
                }
            } else {
                items(recentActivities.take(8), key = { it.id }) { log ->
                    ActivityLogRow(log = log)
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun AiResponseCard(
    state: AssistantUiState,
    isSpeaking: Boolean,
    onSpeak: (String) -> Unit,
    onStopSpeaking: () -> Unit
) {
    val accent = if (state is AssistantUiState.Error) CrimsonBearish else ElectricCyan
    NexoraGlassCard(accentColor = accent) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (state is AssistantUiState.Error) Icons.Default.ErrorOutline else Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (state) {
                        is AssistantUiState.Loading -> "NEXORA Thinking..."
                        is AssistantUiState.Streaming -> "NEXORA Responding..."
                        is AssistantUiState.Success -> "NEXORA AI Response"
                        is AssistantUiState.Error -> "Action Notice"
                        AssistantUiState.Idle -> ""
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = accent
                )
            }

            val speakableText = when (state) {
                is AssistantUiState.Success -> state.replyText
                is AssistantUiState.Streaming -> state.text
                else -> null
            }
            if (!speakableText.isNullOrBlank()) {
                FilledIconButton(
                    onClick = { if (isSpeaking) onStopSpeaking() else onSpeak(speakableText) },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = NavySurfaceHigh,
                        contentColor = ElectricCyan
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isSpeaking) Icons.Default.StopCircle else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = if (isSpeaking) "Stop speaking" else "Speak response"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (state) {
            is AssistantUiState.Loading -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        color = ElectricCyan,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "আপনার নির্দেশ বুঝে পরিকল্পনা তৈরি করা হচ্ছে...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SoftSlateText
                    )
                }
            }

            is AssistantUiState.Streaming -> {
                Text(
                    text = state.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = PureWhite
                )
            }

            is AssistantUiState.Success -> {
                Text(
                    text = state.replyText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = PureWhite
                )
            }

            is AssistantUiState.Error -> {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CrimsonBearish
                )
            }

            AssistantUiState.Idle -> Unit
        }
    }
}

@Composable
private fun ModuleTileCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NexoraGlassCard(
        modifier = modifier.testTag(testTag),
        accentColor = accent,
        onClick = onClick
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = accent.copy(alpha = 0.16f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accent,
                modifier = Modifier
                    .padding(9.dp)
                    .size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = PureWhite
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = SoftSlateText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ActivityLogRow(log: ActivityLogEntity) {
    val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(log.timestamp))
    val badgeColor = when (log.module) {
        "TRADING" -> EmeraldBullish
        "GAMING" -> VioletNeon
        "SCREEN" -> AmberWarning
        "APP_CONTROL" -> ElectricCyan
        else -> ElectricCyan
    }
    NexoraGlassCard(accentColor = badgeColor.copy(alpha = 0.55f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NexoraStatusPill(label = log.module, dotColor = badgeColor)
            Text(
                text = timeStr,
                style = MaterialTheme.typography.labelSmall,
                color = MutedSlateText
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = log.userCommand,
            style = MaterialTheme.typography.titleSmall,
            color = PureWhite,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = log.aiResponse,
            style = MaterialTheme.typography.bodySmall,
            color = SoftSlateText,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}
