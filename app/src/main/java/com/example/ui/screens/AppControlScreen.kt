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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.ScreenshotMonitor
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ai.ScreenAnalysisResult
import com.example.service.AccessibilityScreenState
import com.example.ui.VisionUiState
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
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceHigh
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftSlateText
import com.example.ui.theme.VioletNeon

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppControlScreen(
    accessibilityState: AccessibilityScreenState,
    accessibilityEnabled: Boolean,
    controlFeedback: String?,
    capturedBitmap: Bitmap?,
    captureError: String?,
    screenVisionState: VisionUiState<ScreenAnalysisResult>,
    onOpenAccessibilitySettings: () -> Unit,
    onLaunchYouTube: (String) -> Unit,
    onLaunchWhatsApp: (String) -> Unit,
    onLaunchChrome: (String) -> Unit,
    onLaunchSetting: (String) -> Unit,
    onAccessibilityAction: (String, String) -> Unit,
    onRequestScreenCapture: () -> Unit,
    onPickScreenshot: () -> Unit,
    onAnalyzeScreen: (String) -> Unit,
    onSpeakText: (String) -> Unit
) {
    var youtubeQuery by rememberSaveable { mutableStateOf("") }
    var whatsappMessage by rememberSaveable { mutableStateOf("") }
    var chromeUrl by rememberSaveable { mutableStateOf("") }
    var tapLabel by rememberSaveable { mutableStateOf("") }
    var typeInput by rememberSaveable { mutableStateOf("") }
    var screenQuestion by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 680.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Vision & OCR AI Section
        item {
            NexoraSectionHeader(
                title = "AI Screen Understanding",
                subtitle = "স্ক্রিন ক্যাপচার বা স্ক্রিনশট বিশ্লেষণ (OCR + Vision AI)",
                icon = Icons.Default.ScreenshotMonitor,
                accentColor = ElectricCyan
            )
        }

        item {
            NexoraGlassCard(accentColor = ElectricCyan) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onRequestScreenCapture,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = DeepNavyBackground
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("capture_live_screen_btn")
                    ) {
                        Icon(Icons.Default.ScreenshotMonitor, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Capture Screen")
                    }

                    OutlinedButton(
                        onClick = onPickScreenshot,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("pick_screen_image_btn")
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick Image", color = PureWhite)
                    }
                }

                if (!captureError.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = captureError, style = MaterialTheme.typography.bodySmall, color = AmberWarning)
                }

                if (capturedBitmap != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Image(
                        bitmap = capturedBitmap.asImageBitmap(),
                        contentDescription = "Captured screen preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = screenQuestion,
                    onValueChange = { screenQuestion = it },
                    placeholder = { Text("স্ক্রিনের নির্দিষ্ট কোনো অংশ সম্পর্কে প্রশ্ন করুন...", color = MutedSlateText) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = NavyBorder,
                        focusedContainerColor = NavySurface,
                        unfocusedContainerColor = NavySurface,
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("screen_question_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { onAnalyzeScreen(screenQuestion) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VioletNeon,
                        contentColor = PureWhite
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("analyze_screen_btn")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Explain Screen with Gemini Vision")
                }

                when (screenVisionState) {
                    is VisionUiState.Loading -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = ElectricCyan, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("স্ক্রিনের টেক্সট ও UI উপাদান বিশ্লেষণ করা হচ্ছে...", color = SoftSlateText)
                        }
                    }

                    is VisionUiState.Error -> {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(screenVisionState.message, style = MaterialTheme.typography.bodySmall, color = CrimsonBearish)
                    }

                    is VisionUiState.Success -> {
                        val res = screenVisionState.data
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(res.screenTitle, style = MaterialTheme.typography.titleMedium, color = ElectricCyan)
                            IconButton(onClick = { onSpeakText(res.summaryBengali) }) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Speak screen summary", tint = ElectricCyan)
                            }
                        }
                        Text(res.summaryBengali, style = MaterialTheme.typography.bodyMedium, color = PureWhite)

                        if (res.detectedElements.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Detected UI Elements", style = MaterialTheme.typography.labelMedium, color = SoftSlateText)
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                res.detectedElements.take(8).forEach { el ->
                                    AssistChip(
                                        onClick = { tapLabel = el },
                                        label = { Text(el, style = MaterialTheme.typography.bodySmall) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = NavySurfaceHigh,
                                            labelColor = PureWhite
                                        )
                                    )
                                }
                            }
                        }

                        if (res.suggestedActions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Suggested Next Steps", style = MaterialTheme.typography.labelMedium, color = EmeraldBullish)
                            res.suggestedActions.forEach { act ->
                                Text("• $act", style = MaterialTheme.typography.bodySmall, color = SoftSlateText)
                            }
                        }
                    }

                    VisionUiState.Idle -> Unit
                }
            }
        }

        // Universal App Control Hub
        item {
            NexoraSectionHeader(
                title = "Universal Android App Control",
                subtitle = "অনুমোদিত অ্যাপ ও সেটিংস সরাসরি পরিচালনা করুন",
                icon = Icons.Default.Apps,
                accentColor = VioletNeon
            )
        }

        if (!controlFeedback.isNullOrBlank()) {
            item {
                NexoraGlassCard(accentColor = EmeraldBullish) {
                    Text(
                        text = controlFeedback,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PureWhite
                    )
                }
            }
        }

        // YouTube, WhatsApp, Chrome & Settings Launchers
        item {
            NexoraGlassCard(accentColor = ElectricCyan) {
                // YouTube
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = youtubeQuery,
                        onValueChange = { youtubeQuery = it },
                        placeholder = { Text("YouTube ভিডিও বা গান খুঁজুন...", color = MutedSlateText) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CrimsonBearish,
                            unfocusedBorderColor = NavyBorder,
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("youtube_query_input")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onLaunchYouTube(youtubeQuery) },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonBearish, contentColor = PureWhite),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("launch_youtube_btn")
                    ) {
                        Icon(Icons.Default.PlayCircle, contentDescription = "Open YouTube")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // WhatsApp
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = whatsappMessage,
                        onValueChange = { whatsappMessage = it },
                        placeholder = { Text("WhatsApp মেসেজ ড্রাফট লিখুন...", color = MutedSlateText) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldBullish,
                            unfocusedBorderColor = NavyBorder,
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("whatsapp_message_input")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onLaunchWhatsApp(whatsappMessage) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldBullish, contentColor = DeepNavyBackground),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("launch_whatsapp_btn")
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = "Open WhatsApp")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Chrome
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = chromeUrl,
                        onValueChange = { chromeUrl = it },
                        placeholder = { Text("Chrome ওয়েবসাইট বা সার্চ লিখুন...", color = MutedSlateText) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = NavyBorder,
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chrome_url_input")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onLaunchChrome(chromeUrl) },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = DeepNavyBackground),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("launch_chrome_btn")
                    ) {
                        Icon(Icons.Default.Language, contentDescription = "Open Chrome")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Quick Android Settings", style = MaterialTheme.typography.labelMedium, color = SoftSlateText)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Wi-Fi" to "wifi", "Bluetooth" to "bluetooth", "Display" to "display", "Sound" to "sound", "Battery" to "battery", "System" to "general").forEach { (label, key) ->
                        AssistChip(
                            onClick = { onLaunchSetting(key) },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp), tint = ElectricCyan) },
                            label = { Text(label) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = NavySurfaceHigh,
                                labelColor = PureWhite
                            )
                        )
                    }
                }
            }
        }

        // Accessibility Service Safe Action Deck
        item {
            NexoraGlassCard(accentColor = if (accessibilityEnabled) EmeraldBullish else AmberWarning) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessibilityNew, contentDescription = null, tint = ElectricCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Accessibility Automation Engine", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                    }
                    NexoraStatusPill(
                        label = if (accessibilityEnabled) "ACTIVE" else "OFF",
                        dotColor = if (accessibilityEnabled) EmeraldBullish else AmberWarning
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (accessibilityEnabled) {
                        "সক্রিয় অ্যাপ: ${accessibilityState.foregroundPackage.ifBlank { "Nexora AI" }} • ${accessibilityState.visibleElements.size} UI elements"
                    } else {
                        "অন্যান্য অ্যাপে স্ক্রল, ক্লিক এবং টাইপ সহায়তা দিতে Android Accessibility Service চালু করুন।"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = SoftSlateText
                )

                if (!accessibilityEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onOpenAccessibilitySettings,
                        colors = ButtonDefaults.buttonColors(containerColor = AmberWarning, contentColor = DeepNavyBackground),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("enable_accessibility_btn")
                    ) {
                        Text("Enable NEXORA Accessibility Service")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { onAccessibilityAction("SCROLL_DOWN", "") },
                        leadingIcon = { Icon(Icons.Default.ArrowDownward, null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Scroll Down") }
                    )
                    AssistChip(
                        onClick = { onAccessibilityAction("SCROLL_UP", "") },
                        leadingIcon = { Icon(Icons.Default.ArrowUpward, null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Scroll Up") }
                    )
                    AssistChip(
                        onClick = { onAccessibilityAction("NAVIGATE_BACK", "") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Back") }
                    )
                    AssistChip(
                        onClick = { onAccessibilityAction("NAVIGATE_HOME", "") },
                        leadingIcon = { Icon(Icons.Default.Home, null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Home") }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = tapLabel,
                        onValueChange = { tapLabel = it },
                        placeholder = { Text("বাটনের নাম লিখে ট্যাপ করুন...", color = MutedSlateText) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = NavyBorder,
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onAccessibilityAction("CLICK_TEXT", tapLabel) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(50.dp)
                    ) {
                        Icon(Icons.Default.TouchApp, contentDescription = "Click UI element")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = typeInput,
                        onValueChange = { typeInput = it },
                        placeholder = { Text("ফোকাসড বক্সে টাইপ করুন...", color = MutedSlateText) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VioletNeon,
                            unfocusedBorderColor = NavyBorder,
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onAccessibilityAction("TYPE_TEXT", typeInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = VioletNeon, contentColor = PureWhite),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(50.dp)
                    ) {
                        Text("Type")
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
