package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CandlestickChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.configureAppCheck
import com.example.service.ScreenCaptureService
import com.example.ui.NexoraDestination
import com.example.ui.NexoraViewModel
import com.example.ui.components.NexoraSplashScreen
import com.example.ui.screens.AppControlScreen
import com.example.ui.screens.GamingAssistantScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.SettingsPrivacyScreen
import com.example.ui.screens.TradingAssistantScreen
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.DeepNavyBackground
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanContainer
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NexoraAiTheme
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftSlateText
import com.example.util.PermissionHelper

private enum class ImagePickTarget {
    SCREEN_VISION,
    TRADING_CHART,
    GAMING_SCREEN
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1 (MANDATORY): Configure Firebase App Check with launch intent before UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        setContent {
            NexoraAiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DeepNavyBackground
                ) {
                    NexoraAppRoot()
                }
            }
        }
    }
}

private data class NavItemSpec(
    val destination: NexoraDestination,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
private fun NexoraAppRoot(
    viewModel: NexoraViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var showSplash by rememberSaveable { mutableStateOf(true) }
    var currentDestination by rememberSaveable { mutableStateOf(NexoraDestination.HOME) }
    var imagePickTarget by remember { mutableStateOf(ImagePickTarget.SCREEN_VISION) }
    var showScreenCaptureDisclosure by remember { mutableStateOf(false) }
    var showOverlayDisclosure by remember { mutableStateOf(false) }

    // Refresh permissions & telemetry whenever user returns from Android system settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshSystemStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Return to Home screen on back press when on a secondary destination
    BackHandler(enabled = !showSplash && currentDestination != NexoraDestination.HOME) {
        currentDestination = NexoraDestination.HOME
    }

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val orbState by viewModel.orbState.collectAsStateWithLifecycle()
    val assistantState by viewModel.assistantState.collectAsStateWithLifecycle()
    val pendingSensitivePlan by viewModel.pendingSensitivePlan.collectAsStateWithLifecycle()
    val recentActivities by viewModel.recentActivities.collectAsStateWithLifecycle()
    val memories by viewModel.memories.collectAsStateWithLifecycle()
    val demoTrades by viewModel.demoTrades.collectAsStateWithLifecycle()
    val accessibilityState by viewModel.accessibilityState.collectAsStateWithLifecycle()
    val floatingRunning by viewModel.floatingRunning.collectAsStateWithLifecycle()
    val capturedScreenBitmap by viewModel.capturedScreenBitmap.collectAsStateWithLifecycle()
    val captureError by viewModel.captureError.collectAsStateWithLifecycle()
    val screenVisionState by viewModel.screenVisionState.collectAsStateWithLifecycle()
    val tradingVisionState by viewModel.tradingVisionState.collectAsStateWithLifecycle()
    val tradingChartBitmap by viewModel.tradingChartBitmap.collectAsStateWithLifecycle()
    val gamingVisionState by viewModel.gamingVisionState.collectAsStateWithLifecycle()
    val gamingScreenBitmap by viewModel.gamingScreenBitmap.collectAsStateWithLifecycle()
    val gamingTelemetry by viewModel.gamingTelemetry.collectAsStateWithLifecycle()
    val brokerHealth by viewModel.brokerHealth.collectAsStateWithLifecycle()
    val isTestingBroker by viewModel.isTestingBroker.collectAsStateWithLifecycle()
    val controlFeedback by viewModel.controlFeedback.collectAsStateWithLifecycle()
    val voiceError by viewModel.voiceError.collectAsStateWithLifecycle()

    val isListening by viewModel.voiceEngine.isListening.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.voiceEngine.isSpeaking.collectAsStateWithLifecycle()
    val rmsLevel by viewModel.voiceEngine.rmsLevel.collectAsStateWithLifecycle()
    val partialTranscript by viewModel.voiceEngine.partialTranscript.collectAsStateWithLifecycle()
    val activeVoiceLabel by viewModel.voiceEngine.activeVoiceLabel.collectAsStateWithLifecycle()

    // Runtime Permission Launchers
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.refreshSystemStatus()
        if (granted) {
            viewModel.toggleVoiceListening()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshSystemStatus()
    }

    // On-Demand MediaProjection Screen Capture Launcher
    val screenCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            ScreenCaptureService.startCapture(context, result.resultCode, result.data!!)
            if (currentDestination == NexoraDestination.HOME) {
                currentDestination = NexoraDestination.CONTROL
            }
        }
    }

    // Zero-Permission Android Photo Picker Launcher (Play Policy compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bitmap = PermissionHelper.decodeUriToBitmap(context, uri)
            when (imagePickTarget) {
                ImagePickTarget.SCREEN_VISION -> viewModel.setScreenBitmap(bitmap)
                ImagePickTarget.TRADING_CHART -> viewModel.setTradingChartBitmap(bitmap)
                ImagePickTarget.GAMING_SCREEN -> viewModel.setGamingScreenBitmap(bitmap)
            }
        }
    }

    fun requestVoiceAction() {
        if (permissions.microphoneGranted) {
            viewModel.toggleVoiceListening()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun requestNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permissions.notificationsGranted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun launchPhotoPickerFor(target: ImagePickTarget) {
        imagePickTarget = target
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    if (showSplash) {
        NexoraSplashScreen(onFinished = { showSplash = false })
        return
    }

    val navItems = listOf(
        NavItemSpec(NexoraDestination.HOME, R.string.nav_home, Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
        NavItemSpec(NexoraDestination.CONTROL, R.string.nav_control, Icons.Filled.Apps, Icons.Outlined.Apps),
        NavItemSpec(NexoraDestination.TRADING, R.string.nav_trading, Icons.Filled.CandlestickChart, Icons.Outlined.CandlestickChart),
        NavItemSpec(NexoraDestination.GAMING, R.string.nav_gaming, Icons.Filled.SportsEsports, Icons.Outlined.SportsEsports),
        NavItemSpec(NexoraDestination.SETTINGS, R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 720.dp

        Scaffold(
            containerColor = DeepNavyBackground,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = NavySurface,
                        tonalElevation = 0.dp
                    ) {
                        navItems.forEach { item ->
                            val selected = currentDestination == item.destination
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentDestination = item.destination },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(id = item.labelRes)
                                    )
                                },
                                label = { Text(stringResource(id = item.labelRes)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ElectricCyan,
                                    selectedTextColor = ElectricCyan,
                                    indicatorColor = ElectricCyanContainer,
                                    unselectedIconColor = SoftSlateText,
                                    unselectedTextColor = SoftSlateText
                                ),
                                modifier = Modifier.testTag("nav_${item.destination.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = NavySurface,
                        contentColor = PureWhite
                    ) {
                        navItems.forEach { item ->
                            val selected = currentDestination == item.destination
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentDestination = item.destination },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(id = item.labelRes)
                                    )
                                },
                                label = { Text(stringResource(id = item.labelRes)) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = ElectricCyan,
                                    selectedTextColor = ElectricCyan,
                                    indicatorColor = ElectricCyanContainer,
                                    unselectedIconColor = SoftSlateText,
                                    unselectedTextColor = SoftSlateText
                                ),
                                modifier = Modifier.testTag("rail_${item.destination.route}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = currentDestination,
                        label = "nexora_screen_switch"
                    ) { destination ->
                        when (destination) {
                            NexoraDestination.HOME -> HomeDashboardScreen(
                                settings = settings,
                                orbState = orbState,
                                rmsLevel = rmsLevel,
                                isListening = isListening,
                                isSpeaking = isSpeaking,
                                partialTranscript = partialTranscript,
                                voiceError = voiceError,
                                assistantState = assistantState,
                                floatingRunning = floatingRunning,
                                recentActivities = recentActivities,
                                onVoiceTap = { requestVoiceAction() },
                                onSendCommand = { viewModel.submitCommand(it) },
                                onToggleLanguage = {
                                    val nextLang = if (settings.voiceLanguage.startsWith("bn")) "en-US" else "bn-BD"
                                    viewModel.setVoiceLanguage(nextLang)
                                },
                                onToggleFloating = { enable ->
                                    if (enable) {
                                        requestNotificationsIfNeeded()
                                        val started = viewModel.toggleFloatingAssistant(true)
                                        if (!started) {
                                            showOverlayDisclosure = true
                                        }
                                    } else {
                                        viewModel.toggleFloatingAssistant(false)
                                    }
                                },
                                onSpeakText = { viewModel.speakText(it) },
                                onStopSpeaking = { viewModel.stopSpeaking() },
                                onNavigate = { currentDestination = it },
                                onRequestScreenCapture = { showScreenCaptureDisclosure = true }
                            )

                            NexoraDestination.CONTROL -> AppControlScreen(
                                accessibilityState = accessibilityState,
                                accessibilityEnabled = permissions.accessibilityEnabled,
                                controlFeedback = controlFeedback,
                                capturedBitmap = capturedScreenBitmap,
                                captureError = captureError,
                                screenVisionState = screenVisionState,
                                onOpenAccessibilitySettings = { PermissionHelper.openAccessibilitySettings(context) },
                                onLaunchYouTube = { viewModel.launchYouTube(it) },
                                onLaunchWhatsApp = { viewModel.launchWhatsApp(it) },
                                onLaunchChrome = { viewModel.launchChrome(it) },
                                onLaunchSetting = { viewModel.launchSetting(it) },
                                onAccessibilityAction = { act, target -> viewModel.runAccessibilityControl(act, target) },
                                onRequestScreenCapture = { showScreenCaptureDisclosure = true },
                                onPickScreenshot = { launchPhotoPickerFor(ImagePickTarget.SCREEN_VISION) },
                                onAnalyzeScreen = { viewModel.analyzeCurrentScreen(it) },
                                onSpeakText = { viewModel.speakText(it) }
                            )

                            NexoraDestination.TRADING -> TradingAssistantScreen(
                                settings = settings,
                                chartBitmap = tradingChartBitmap,
                                tradingVisionState = tradingVisionState,
                                demoTrades = demoTrades,
                                brokerHealth = brokerHealth,
                                isTestingBroker = isTestingBroker,
                                onPickChartImage = { launchPhotoPickerFor(ImagePickTarget.TRADING_CHART) },
                                onCaptureChartScreen = { showScreenCaptureDisclosure = true },
                                onAnalyzeChart = { sym, tf, notes -> viewModel.analyzeTradingChart(sym, tf, notes) },
                                onAddDemoTrade = { sym, side, entry, target, sl, qty, notes, liveConfirmed ->
                                    viewModel.addDemoTrade(sym, side, entry, target, sl, qty, notes, liveConfirmed)
                                },
                                onCloseDemoTrade = { trade, exit -> viewModel.closeDemoTrade(trade, exit) },
                                onDeleteDemoTrade = { viewModel.deleteDemoTrade(it) },
                                onSaveAndTestBroker = { name, url -> viewModel.saveAndTestBroker(name, url) },
                                onSpeakText = { viewModel.speakText(it) }
                            )

                            NexoraDestination.GAMING -> GamingAssistantScreen(
                                settings = settings,
                                telemetry = gamingTelemetry,
                                gameBitmap = gamingScreenBitmap,
                                gamingVisionState = gamingVisionState,
                                onToggleVoiceCoach = { viewModel.setGamingVoiceCoachEnabled(it) },
                                onLaunchFreeFire = { viewModel.launchFreeFire() },
                                onPickGameScreenshot = { launchPhotoPickerFor(ImagePickTarget.GAMING_SCREEN) },
                                onCaptureGameScreen = { showScreenCaptureDisclosure = true },
                                onAnalyzeGaming = { name, scenario -> viewModel.analyzeGamingTactics(name, scenario) },
                                onSpeakText = { viewModel.speakText(it) }
                            )

                            NexoraDestination.SETTINGS -> SettingsPrivacyScreen(
                                settings = settings,
                                permissions = permissions,
                                activeVoiceLabel = activeVoiceLabel,
                                memories = memories,
                                onToggleMasterAssistant = { viewModel.setAssistantEnabled(it) },
                                onToggleVoiceAutoSpeak = { viewModel.setVoiceAutoSpeak(it) },
                                onSelectVoiceLanguage = { viewModel.setVoiceLanguage(it) },
                                onChangeVoiceSpeed = { viewModel.setVoiceSpeed(it) },
                                onChangeVoiceVolume = { viewModel.setVoiceVolume(it) },
                                onTestVoice = {
                                    val sample = if (settings.voiceLanguage.startsWith("bn")) {
                                        "নমস্কার! আমি নেক্সোরা এআই, আপনার ইন্টেলিজেন্ট ডিজিটাল কম্প্যানিয়ন। আমি ভয়েস, স্ক্রিন বিশ্লেষণ, ট্রেডিং এবং গেমিং কোচিংয়ে সাহায্য করতে প্রস্তুত।"
                                    } else {
                                        "Hello! I am Nexora AI, your intelligent digital companion. I am ready to assist you."
                                    }
                                    viewModel.speakText(sample)
                                },
                                onRequestMicPermission = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                                onRequestNotificationPermission = { requestNotificationsIfNeeded() },
                                onOpenOverlaySettings = { PermissionHelper.openOverlaySettings(context) },
                                onOpenAccessibilitySettings = { PermissionHelper.openAccessibilitySettings(context) },
                                onRequestScreenCapture = { showScreenCaptureDisclosure = true },
                                onToggleAiMemory = { viewModel.setAiMemoryEnabled(it) },
                                onAddMemory = { l, d, c -> viewModel.addMemory(l, d, c) },
                                onDeleteMemory = { viewModel.deleteMemory(it) },
                                onToggleSensitiveGuard = { viewModel.setConfirmSensitiveApps(it) },
                                onClearAllPrivacyData = { viewModel.clearAllPrivacyData() }
                            )
                        }
                    }
                }
            }
        }
    }

    // Transparent Screen Capture Disclosure Dialog (never silently captures)
    if (showScreenCaptureDisclosure) {
        AlertDialog(
            onDismissRequest = { showScreenCaptureDisclosure = false },
            title = { Text("Screen Capture Permission", color = ElectricCyan) },
            text = {
                Text(
                    "NEXORA AI শুধুমাত্র আপনার অনুমতির পর বর্তমান স্ক্রিনের ১টি ফ্রেম ক্যাপচার করে Gemini Vision AI দিয়ে বিশ্লেষণ করবে। কোনো ভিডিও ব্যাকগ্রাউন্ডে রেকর্ড বা সংরক্ষণ করা হবে না।"
                )
            },
            confirmButton = {
                Button(onClick = {
                    showScreenCaptureDisclosure = false
                    requestNotificationsIfNeeded()
                    val mpm = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    screenCaptureLauncher.launch(mpm.createScreenCaptureIntent())
                }) {
                    Text("Allow One-Time Capture")
                }
            },
            dismissButton = {
                TextButton(onClick = { showScreenCaptureDisclosure = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Transparent Overlay Disclosure Dialog
    if (showOverlayDisclosure) {
        AlertDialog(
            onDismissRequest = { showOverlayDisclosure = false },
            title = { Text("Display Over Other Apps Required", color = ElectricCyan) },
            text = {
                Text(
                    "অন্যান্য অ্যাপ (যেমন YouTube, Trading App বা Free Fire) ব্যবহার করার সময় ভাসমান NEXORA AI Bubble দেখানোর জন্য Android-এর 'Display Over Other Apps' পারমিশন চালু করা প্রয়োজন।"
                )
            },
            confirmButton = {
                Button(onClick = {
                    showOverlayDisclosure = false
                    PermissionHelper.openOverlaySettings(context)
                }) {
                    Text("Open Overlay Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverlayDisclosure = false }) {
                    Text("Not Now")
                }
            }
        )
    }

    // Sensitive App / Action Confirmation Dialog
    pendingSensitivePlan?.let { plan ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissPendingPlan() },
            title = { Text("Sensitive Action Confirmation", color = AmberWarning) },
            text = {
                Text(
                    "NEXORA AI নিচের কাজটি সম্পন্ন করতে চায়:\n\n" +
                        plan.steps.joinToString("\n") { "• ${it.detail.ifBlank { it.action }} (${it.target})" } +
                        "\n\nআপনি কি এটি অনুমোদন করছেন?"
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.confirmAndExecutePendingPlan() }) {
                    Text("Approve & Run")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPendingPlan() }) {
                    Text("Cancel", color = Color.LightGray)
                }
            }
        )
    }
}
