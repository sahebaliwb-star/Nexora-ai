package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.AiMemoryEntity
import com.example.data.local.NexoraSettings
import com.example.ui.components.NexoraGlassCard
import com.example.ui.components.NexoraSectionHeader
import com.example.ui.components.NexoraStatusPill
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonBearish
import com.example.ui.theme.DeepNavyBackground
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldBullish
import com.example.ui.theme.MutedSlateText
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftSlateText
import com.example.ui.theme.VioletNeon
import com.example.util.PermissionStatusSnapshot
import java.util.Locale

@Composable
fun SettingsPrivacyScreen(
    settings: NexoraSettings,
    permissions: PermissionStatusSnapshot,
    activeVoiceLabel: String,
    memories: List<AiMemoryEntity>,
    onToggleMasterAssistant: (Boolean) -> Unit,
    onToggleVoiceAutoSpeak: (Boolean) -> Unit,
    onSelectVoiceLanguage: (String) -> Unit,
    onChangeVoiceSpeed: (Float) -> Unit,
    onChangeVoiceVolume: (Float) -> Unit,
    onTestVoice: () -> Unit,
    onRequestMicPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onRequestScreenCapture: () -> Unit,
    onToggleAiMemory: (Boolean) -> Unit,
    onAddMemory: (label: String, detail: String, category: String) -> Unit,
    onDeleteMemory: (Long) -> Unit,
    onToggleSensitiveGuard: (Boolean) -> Unit,
    onClearAllPrivacyData: () -> Unit
) {
    var memLabel by rememberSaveable { mutableStateOf("") }
    var memDetail by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 680.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Brand Identity Card showcasing NEXORA AI companion art & transparent logo
        item {
            NexoraGlassCard(accentColor = ElectricCyan) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.img_nexora_companion_art),
                        contentDescription = "NEXORA AI Brand Artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(84.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.img_nexora_logo_transparent),
                                contentDescription = "NEXORA AI Transparent Logo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(id = R.string.brand_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = PureWhite
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(id = R.string.brand_tagline),
                            style = MaterialTheme.typography.bodySmall,
                            color = ElectricCyan
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Gemini 3.8 Flash via Firebase AI Logic • Zero Hardcoded API Keys",
                            style = MaterialTheme.typography.labelSmall,
                            color = SoftSlateText
                        )
                    }
                }
            }
        }

        // Master Kill Switch & Privacy Security
        item {
            NexoraSectionHeader(
                title = "Security & Master Control",
                subtitle = "যেকোনো সময় AI সম্পূর্ণ বন্ধ বা চালু করুন",
                icon = Icons.Default.PowerSettingsNew,
                accentColor = if (settings.assistantEnabled) EmeraldBullish else CrimsonBearish
            )
            Spacer(modifier = Modifier.height(8.dp))
            NexoraGlassCard(accentColor = if (settings.assistantEnabled) EmeraldBullish else CrimsonBearish) {
                SettingToggleRow(
                    title = "Master AI Assistant Switch",
                    subtitle = if (settings.assistantEnabled) "NEXORA AI সক্রিয় আছে" else "সম্পূর্ণ বন্ধ (Kill Switch Active)",
                    checked = settings.assistantEnabled,
                    onCheckedChange = onToggleMasterAssistant,
                    testTag = "master_kill_switch"
                )
                HorizontalDivider(color = NavyBorder)
                SettingToggleRow(
                    title = "Sensitive App Confirmation Guard",
                    subtitle = "ব্যাংকিং, মেসেজিং ও লাইভ ট্রেডের আগে আপনার কনফার্মেশন নেবে",
                    checked = settings.confirmSensitiveApps,
                    onCheckedChange = onToggleSensitiveGuard,
                    testTag = "sensitive_guard_switch"
                )
            }
        }

        // Transparent Android Permissions Center
        item {
            NexoraSectionHeader(
                title = "Transparent Permission Center",
                subtitle = "প্রতিটি পারমিশন কেন দরকার তা স্বচ্ছভাবে দেখুন — গোপনে কোনো পারমিশন ব্যবহৃত হয় না",
                icon = Icons.Default.VerifiedUser,
                accentColor = ElectricCyan
            )
            Spacer(modifier = Modifier.height(8.dp))
            NexoraGlassCard(accentColor = ElectricCyan) {
                PermissionRowItem(
                    title = "Microphone (RECORD_AUDIO)",
                    reason = "বাংলা ও ইংরেজি ভয়েস কমান্ড শোনার জন্য প্রয়োজন।",
                    granted = permissions.microphoneGranted,
                    actionLabel = "Grant Mic",
                    onAction = onRequestMicPermission
                )
                HorizontalDivider(color = NavyBorder)
                PermissionRowItem(
                    title = "Display Over Other Apps",
                    reason = "অন্যান্য অ্যাপের উপর Floating AI Bubble ও Panel দেখানোর জন্য।",
                    granted = permissions.overlayGranted,
                    actionLabel = "Open Setting",
                    onAction = onOpenOverlaySettings
                )
                HorizontalDivider(color = NavyBorder)
                PermissionRowItem(
                    title = "Accessibility Service",
                    reason = "অনুমোদিত অ্যাপে স্ক্রল, ক্লিক, টাইপ ও UI এলিমেন্ট পড়ার জন্য।",
                    granted = permissions.accessibilityEnabled,
                    actionLabel = "Enable",
                    onAction = onOpenAccessibilitySettings
                )
                HorizontalDivider(color = NavyBorder)
                PermissionRowItem(
                    title = "Notifications (Foreground Service)",
                    reason = "ব্যাকগ্রাউন্ড Floating AI সার্ভিস সক্রিয় থাকার নোটিফিকেশন দেখাতে।",
                    granted = permissions.notificationsGranted,
                    actionLabel = "Allow",
                    onAction = onRequestNotificationPermission
                )
                HorizontalDivider(color = NavyBorder)
                PermissionRowItem(
                    title = "On-Demand Screen Capture",
                    reason = "শুধুমাত্র আপনি চাপলেই ১টি ফ্রেম ক্যাপচার করে Vision AI বিশ্লেষণ করবে।",
                    granted = false,
                    actionLabel = "Capture Now",
                    onAction = onRequestScreenCapture
                )
            }
        }

        // Bengali Female Voice Assistant Settings
        item {
            NexoraSectionHeader(
                title = "Bengali Female Voice Assistant",
                subtitle = "ভয়েস ভাষা, স্পিড ও ভলিউম কাস্টমাইজ করুন",
                icon = Icons.Default.RecordVoiceOver,
                accentColor = VioletNeon
            )
            Spacer(modifier = Modifier.height(8.dp))
            NexoraGlassCard(accentColor = VioletNeon) {
                SettingToggleRow(
                    title = "Auto-Speak AI Responses",
                    subtitle = "Active Engine: $activeVoiceLabel",
                    checked = settings.voiceAutoSpeak,
                    onCheckedChange = onToggleVoiceAutoSpeak,
                    testTag = "voice_autospeak_switch"
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = settings.voiceLanguage.startsWith("bn"),
                        onClick = { onSelectVoiceLanguage("bn-BD") },
                        label = { Text("বাংলা (Female AI)") }
                    )
                    FilterChip(
                        selected = settings.voiceLanguage.startsWith("en"),
                        onClick = { onSelectVoiceLanguage("en-US") },
                        label = { Text("English (Female AI)") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = String.format(Locale.US, "Voice Speed: %.2fx", settings.voiceSpeed),
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricCyan
                )
                Slider(
                    value = settings.voiceSpeed,
                    onValueChange = onChangeVoiceSpeed,
                    valueRange = 0.5f..1.8f,
                    colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
                )

                Text(
                    text = "Voice Volume: ${(settings.voiceVolume * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = VioletNeon
                )
                Slider(
                    value = settings.voiceVolume,
                    onValueChange = onChangeVoiceVolume,
                    valueRange = 0.1f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = VioletNeon, activeTrackColor = VioletNeon)
                )

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onTestVoice,
                    colors = ButtonDefaults.buttonColors(containerColor = VioletNeon, contentColor = PureWhite),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("test_bengali_voice_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test Bengali Female AI Voice")
                }
            }
        }

        // AI Memory Manager
        item {
            NexoraSectionHeader(
                title = "AI Memory Settings",
                subtitle = "ব্যক্তিগত পছন্দ মনে রাখবে যেন NEXORA আরও স্মার্ট উত্তর দেয়",
                icon = Icons.Default.Psychology,
                accentColor = ElectricCyan
            )
            Spacer(modifier = Modifier.height(8.dp))
            NexoraGlassCard(accentColor = ElectricCyan) {
                SettingToggleRow(
                    title = "Enable Contextual AI Memory",
                    subtitle = "সংরক্ষিত মেমরি AI উত্তরে ব্যবহার করবে",
                    checked = settings.aiMemoryEnabled,
                    onCheckedChange = onToggleAiMemory,
                    testTag = "ai_memory_switch"
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = memLabel,
                    onValueChange = { memLabel = it },
                    placeholder = { Text("বিষয় (যেমন: আমার নাম / প্রিয় গেম)", color = MutedSlateText) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = NavyBorder,
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = memDetail,
                    onValueChange = { memDetail = it },
                    placeholder = { Text("তথ্য লিখুন...", color = MutedSlateText) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = NavyBorder,
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (memLabel.isNotBlank() && memDetail.isNotBlank()) {
                            onAddMemory(memLabel, memDetail, "PREFERENCE")
                            memLabel = ""
                            memDetail = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = DeepNavyBackground),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("add_memory_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save to AI Memory")
                }
            }
        }

        items(memories, key = { it.id }) { mem ->
            NexoraGlassCard(accentColor = NavyBorder) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(mem.label, style = MaterialTheme.typography.titleSmall, color = ElectricCyan)
                        Text(mem.detail, style = MaterialTheme.typography.bodySmall, color = PureWhite)
                    }
                    IconButton(onClick = { onDeleteMemory(mem.id) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete memory", tint = MutedSlateText)
                    }
                }
            }
        }

        // Privacy Wipe & Android Studio APK Build Guide
        item {
            NexoraGlassCard(accentColor = CrimsonBearish) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = CrimsonBearish)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Privacy & Data Control", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "আপনার সকল অ্যাক্টিভিটি হিস্ট্রি ও মেমরি শুধুমাত্র এই ডিভাইসেই নিরাপদে সংরক্ষিত থাকে। এক ক্লিকে সব মুছে ফেলতে পারবেন।",
                    style = MaterialTheme.typography.bodySmall,
                    color = SoftSlateText
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onClearAllPrivacyData,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("clear_privacy_data_btn")
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = CrimsonBearish)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear All AI History & Memory", color = CrimsonBearish)
                }
            }
        }

        item {
            NexoraGlassCard(accentColor = AmberWarning) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Build, contentDescription = null, tint = AmberWarning)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Android Studio & APK Build Guide", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "১. প্রোজেক্টটি ZIP হিসেবে Export করুন বা GitHub-এ Push করুন।\n" +
                        "২. Android Studio-তে File > Open করে প্রোজেক্ট ফোল্ডার খুলুন এবং Gradle Sync সম্পন্ন হতে দিন।\n" +
                        "৩. Debug APK তৈরি করতে: Build > Build Bundle(s) / APK(s) > Build APK(s) অথবা টার্মিনালে 'gradle :app:assembleDebug' চালান।\n" +
                        "৪. Release APK/AAB-এর জন্য Build > Generate Signed Bundle / APK নির্বাচন করুন।",
                    style = MaterialTheme.typography.bodySmall,
                    color = SoftSlateText
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = PureWhite)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SoftSlateText)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = DeepNavyBackground,
                checkedTrackColor = ElectricCyan
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun PermissionRowItem(
    title: String,
    reason: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = PureWhite)
                Spacer(modifier = Modifier.width(8.dp))
                NexoraStatusPill(
                    label = if (granted) "GRANTED" else "ACTION",
                    dotColor = if (granted) EmeraldBullish else AmberWarning
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(reason, style = MaterialTheme.typography.bodySmall, color = SoftSlateText)
        }
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedButton(
            onClick = onAction,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(actionLabel, style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
        }
    }
}
