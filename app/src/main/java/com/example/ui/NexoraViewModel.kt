package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GamingTacticalResult
import com.example.ai.NexoraAiRepository
import com.example.ai.ScreenAnalysisResult
import com.example.ai.TaskPlanResult
import com.example.ai.TradingAnalysisResult
import com.example.data.BrokerHealthResult
import com.example.data.NexoraRepository
import com.example.data.local.ActivityLogEntity
import com.example.data.local.AiMemoryEntity
import com.example.data.local.DemoTradeEntity
import com.example.data.local.NexoraSettings
import com.example.service.AccessibilityScreenState
import com.example.service.AppControlExecutor
import com.example.service.FloatingAssistantService
import com.example.service.NexoraAccessibilityService
import com.example.service.ScreenCaptureService
import com.example.service.StepExecutionReport
import com.example.util.DeviceGamingTelemetry
import com.example.util.PermissionHelper
import com.example.util.PermissionStatusSnapshot
import com.example.voice.NexoraVoiceEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "NexoraViewModel"

enum class NexoraOrbState {
    IDLE,
    LISTENING,
    THINKING,
    WORKING,
    SPEAKING,
    PAUSED
}

sealed interface AssistantUiState {
    data object Idle : AssistantUiState
    data object Loading : AssistantUiState
    data class Streaming(val text: String) : AssistantUiState
    data class Success(
        val replyText: String,
        val plan: TaskPlanResult? = null,
        val executionReports: List<StepExecutionReport> = emptyList()
    ) : AssistantUiState
    data class Error(val message: String) : AssistantUiState
}

sealed interface VisionUiState<out T> {
    data object Idle : VisionUiState<Nothing>
    data object Loading : VisionUiState<Nothing>
    data class Success<T>(val data: T) : VisionUiState<T>
    data class Error(val message: String) : VisionUiState<Nothing>
}

enum class NexoraDestination(val route: String) {
    HOME("home"),
    CONTROL("control"),
    TRADING("trading"),
    GAMING("gaming"),
    SETTINGS("settings")
}

class NexoraViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val aiRepository = NexoraAiRepository()
    private val repository = NexoraRepository(appContext)
    private val appControlExecutor = AppControlExecutor(appContext)
    val voiceEngine = NexoraVoiceEngine(appContext)

    val settings: StateFlow<NexoraSettings> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NexoraSettings()
    )

    val recentActivities: StateFlow<List<ActivityLogEntity>> = repository.recentActivities.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val memories: StateFlow<List<AiMemoryEntity>> = repository.memories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val demoTrades: StateFlow<List<DemoTradeEntity>> = repository.demoTrades.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val accessibilityState: StateFlow<AccessibilityScreenState> = NexoraAccessibilityService.screenState
    val floatingRunning: StateFlow<Boolean> = FloatingAssistantService.isRunning
    val capturedScreenBitmap: StateFlow<Bitmap?> = ScreenCaptureService.capturedBitmap
    val captureError: StateFlow<String?> = ScreenCaptureService.captureError

    private val _permissions = MutableStateFlow(PermissionHelper.checkPermissions(appContext))
    val permissions: StateFlow<PermissionStatusSnapshot> = _permissions.asStateFlow()

    private val _orbState = MutableStateFlow(NexoraOrbState.IDLE)
    val orbState: StateFlow<NexoraOrbState> = _orbState.asStateFlow()

    private val _assistantState = MutableStateFlow<AssistantUiState>(AssistantUiState.Idle)
    val assistantState: StateFlow<AssistantUiState> = _assistantState.asStateFlow()

    private val _pendingSensitivePlan = MutableStateFlow<TaskPlanResult?>(null)
    val pendingSensitivePlan: StateFlow<TaskPlanResult?> = _pendingSensitivePlan.asStateFlow()

    private val _screenVisionState = MutableStateFlow<VisionUiState<ScreenAnalysisResult>>(VisionUiState.Idle)
    val screenVisionState: StateFlow<VisionUiState<ScreenAnalysisResult>> = _screenVisionState.asStateFlow()

    private val _tradingVisionState = MutableStateFlow<VisionUiState<TradingAnalysisResult>>(VisionUiState.Idle)
    val tradingVisionState: StateFlow<VisionUiState<TradingAnalysisResult>> = _tradingVisionState.asStateFlow()

    private val _tradingChartBitmap = MutableStateFlow<Bitmap?>(null)
    val tradingChartBitmap: StateFlow<Bitmap?> = _tradingChartBitmap.asStateFlow()

    private val _gamingVisionState = MutableStateFlow<VisionUiState<GamingTacticalResult>>(VisionUiState.Idle)
    val gamingVisionState: StateFlow<VisionUiState<GamingTacticalResult>> = _gamingVisionState.asStateFlow()

    private val _gamingScreenBitmap = MutableStateFlow<Bitmap?>(null)
    val gamingScreenBitmap: StateFlow<Bitmap?> = _gamingScreenBitmap.asStateFlow()

    private val _gamingTelemetry = MutableStateFlow(PermissionHelper.readDeviceGamingTelemetry(appContext))
    val gamingTelemetry: StateFlow<DeviceGamingTelemetry> = _gamingTelemetry.asStateFlow()

    private val _brokerHealth = MutableStateFlow<BrokerHealthResult?>(null)
    val brokerHealth: StateFlow<BrokerHealthResult?> = _brokerHealth.asStateFlow()

    private val _isTestingBroker = MutableStateFlow(false)
    val isTestingBroker: StateFlow<Boolean> = _isTestingBroker.asStateFlow()

    private val _controlFeedback = MutableStateFlow<String?>(null)
    val controlFeedback: StateFlow<String?> = _controlFeedback.asStateFlow()

    private val _voiceError = MutableStateFlow<String?>(null)
    val voiceError: StateFlow<String?> = _voiceError.asStateFlow()

    init {
        viewModelScope.launch {
            settings.collect { s ->
                voiceEngine.configureVoice(s.voiceLanguage, s.voiceSpeed, s.voiceVolume)
                if (!s.assistantEnabled) {
                    _orbState.value = NexoraOrbState.PAUSED
                } else if (_orbState.value == NexoraOrbState.PAUSED) {
                    _orbState.value = NexoraOrbState.IDLE
                }
            }
        }
        viewModelScope.launch {
            voiceEngine.isListening.collect { listening ->
                if (listening) {
                    _orbState.value = NexoraOrbState.LISTENING
                } else if (_orbState.value == NexoraOrbState.LISTENING) {
                    _orbState.value = if (settings.value.assistantEnabled) NexoraOrbState.IDLE else NexoraOrbState.PAUSED
                }
            }
        }
        viewModelScope.launch {
            voiceEngine.isSpeaking.collect { speaking ->
                if (speaking) {
                    _orbState.value = NexoraOrbState.SPEAKING
                } else if (_orbState.value == NexoraOrbState.SPEAKING) {
                    _orbState.value = if (settings.value.assistantEnabled) NexoraOrbState.IDLE else NexoraOrbState.PAUSED
                }
            }
        }
    }

    fun refreshSystemStatus() {
        _permissions.value = PermissionHelper.checkPermissions(appContext)
        _gamingTelemetry.value = PermissionHelper.readDeviceGamingTelemetry(appContext)
    }

    fun submitCommand(command: String, useTaskPlanner: Boolean = true) {
        val trimmed = command.trim()
        if (trimmed.isBlank()) return

        val currentSettings = settings.value
        if (!currentSettings.assistantEnabled) {
            _assistantState.value = AssistantUiState.Error(
                "NEXORA AI বর্তমানে সম্পূর্ণ বন্ধ (Kill Switch) আছে। Settings থেকে চালু করুন।"
            )
            return
        }

        viewModelScope.launch {
            _voiceError.value = null
            _orbState.value = NexoraOrbState.THINKING
            _assistantState.value = AssistantUiState.Loading

            try {
                val memoriesList = if (currentSettings.aiMemoryEnabled) {
                    repository.getMemoryContextStrings()
                } else {
                    emptyList()
                }
                val screenSnapshot = NexoraAccessibilityService.screenState.value

                if (useTaskPlanner && shouldUseTaskPlanner(trimmed)) {
                    val plan = aiRepository.planTask(
                        command = trimmed,
                        memories = memoriesList,
                        currentForegroundApp = screenSnapshot.foregroundPackage,
                        visibleScreenText = screenSnapshot.screenTextDump
                    )

                    val needsGuard = (plan.isSensitive || plan.requiresConfirmation || screenSnapshot.isSensitiveAppActive) &&
                        currentSettings.confirmSensitiveApps &&
                        plan.steps.any { it.action != "REPLY_ONLY" }

                    if (needsGuard) {
                        _pendingSensitivePlan.value = plan
                        _assistantState.value = AssistantUiState.Success(
                            replyText = "${plan.bengaliReply}\n\nনিরাপত্তার জন্য এই কাজটি চালানোর আগে আপনার অনুমোদন প্রয়োজন।",
                            plan = plan
                        )
                        _orbState.value = NexoraOrbState.IDLE
                        repository.logActivity("ASSISTANT", trimmed, plan.bengaliReply, "REQUIRES_PERMISSION")
                        return@launch
                    }

                    executePlanSteps(trimmed, plan)
                } else {
                    var finalReply = ""
                    aiRepository.streamAssistantResponse(
                        userPrompt = trimmed,
                        memories = memoriesList,
                        screenContext = screenSnapshot.screenTextDump,
                        preferredLanguage = currentSettings.voiceLanguage
                    ).collect { streamed ->
                        finalReply = streamed
                        _assistantState.value = AssistantUiState.Streaming(streamed)
                    }
                    _assistantState.value = AssistantUiState.Success(replyText = finalReply)
                    _orbState.value = NexoraOrbState.IDLE
                    repository.logActivity("ASSISTANT", trimmed, finalReply)
                    if (currentSettings.voiceAutoSpeak) {
                        voiceEngine.speak(finalReply)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Assistant timeout", e)
                _orbState.value = NexoraOrbState.IDLE
                _assistantState.value = AssistantUiState.Error("AI উত্তর দিতে সময়সীমা অতিক্রম করেছে। আবার চেষ্টা করুন।")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Assistant command failed", e)
                _orbState.value = NexoraOrbState.IDLE
                val msg = e.localizedMessage ?: "AI অনুরোধ সম্পন্ন করা যায়নি।"
                _assistantState.value = AssistantUiState.Error(msg)
                repository.logActivity("ASSISTANT", trimmed, msg, "ERROR")
            }
        }
    }

    fun confirmAndExecutePendingPlan() {
        val plan = _pendingSensitivePlan.value ?: return
        _pendingSensitivePlan.value = null
        viewModelScope.launch {
            executePlanSteps(plan.intentSummary, plan)
        }
    }

    fun dismissPendingPlan() {
        _pendingSensitivePlan.value = null
    }

    private suspend fun executePlanSteps(command: String, plan: TaskPlanResult) {
        _orbState.value = NexoraOrbState.WORKING
        val reports = mutableListOf<StepExecutionReport>()
        for (step in plan.steps) {
            if (step.action != "REPLY_ONLY") {
                reports.add(appControlExecutor.executeStep(step))
            }
        }
        val combinedText = buildString {
            append(plan.bengaliReply)
            if (reports.isNotEmpty()) {
                append("\n\nধাপসমূহের ফলাফল:\n")
                reports.forEach { r ->
                    val prefix = if (r.success) "✓" else "!"
                    append("$prefix ${r.message}\n")
                }
            }
        }.trim()

        _assistantState.value = AssistantUiState.Success(
            replyText = combinedText,
            plan = plan,
            executionReports = reports
        )
        _orbState.value = NexoraOrbState.IDLE
        val status = if (reports.any { !it.success }) "REQUIRES_PERMISSION" else "COMPLETED"
        repository.logActivity("APP_CONTROL", command, combinedText, status)
        if (settings.value.voiceAutoSpeak) {
            voiceEngine.speak(plan.bengaliReply)
        }
    }

    private fun shouldUseTaskPlanner(command: String): Boolean {
        val lower = command.lowercase()
        val actionKeywords = listOf(
            "youtube", "ইউটিউব", "whatsapp", "হোয়াটসঅ্যাপ", "chrome", "ক্রোম", "website", "ওয়েবসাইট",
            "settings", "সেটিংস", "wifi", "ওয়াইফাই", "bluetooth", "scroll", "স্ক্রল",
            "open", "খোল", "চালু", "click", "ক্লিক", "type", "টাইপ", "back", "home", "মেসেজ"
        )
        return actionKeywords.any { lower.contains(it) }
    }

    fun toggleVoiceListening() {
        if (voiceEngine.isListening.value) {
            voiceEngine.stopListening()
            return
        }
        if (!settings.value.assistantEnabled) {
            _voiceError.value = "AI Assistant বর্তমানে বন্ধ আছে।"
            return
        }
        _voiceError.value = null
        voiceEngine.startListening(
            preferredLanguage = settings.value.voiceLanguage,
            onFinalResult = { transcript ->
                submitCommand(transcript)
            },
            onError = { err ->
                _voiceError.value = err
            }
        )
    }

    fun speakText(text: String) {
        voiceEngine.speak(text)
    }

    fun stopSpeaking() {
        voiceEngine.stopSpeaking()
    }

    // --- Universal App Control Direct Helpers ---
    fun launchYouTube(query: String) {
        val report = appControlExecutor.openYouTubeQuery(query)
        _controlFeedback.value = report.message
        logControlAction("YouTube: $query", report)
    }

    fun launchWhatsApp(draftMessage: String) {
        val report = appControlExecutor.openWhatsAppDraft(draftMessage)
        _controlFeedback.value = report.message
        logControlAction("WhatsApp Draft", report)
    }

    fun launchChrome(urlOrQuery: String) {
        val report = appControlExecutor.openChromeUrl(urlOrQuery)
        _controlFeedback.value = report.message
        logControlAction("Chrome: $urlOrQuery", report)
    }

    fun launchSetting(section: String) {
        val report = appControlExecutor.openSettingsSection(section)
        _controlFeedback.value = report.message
        logControlAction("Settings: $section", report)
    }

    fun runAccessibilityControl(action: String, target: String = "") {
        val step = com.example.ai.TaskPlanStep(action, target, action)
        val report = appControlExecutor.executeStep(step)
        _controlFeedback.value = report.message
        logControlAction("Accessibility $action", report)
    }

    private fun logControlAction(title: String, report: StepExecutionReport) {
        viewModelScope.launch {
            repository.logActivity(
                module = "APP_CONTROL",
                userCommand = title,
                aiResponse = report.message,
                status = if (report.success) "COMPLETED" else "REQUIRES_PERMISSION"
            )
        }
    }

    // --- Screen Understanding Module ---
    fun setScreenBitmap(bitmap: Bitmap?) {
        ScreenCaptureService.setPickedBitmap(bitmap)
    }

    fun analyzeCurrentScreen(question: String) {
        val bitmap = capturedScreenBitmap.value
        val accText = NexoraAccessibilityService.instance?.refreshScreenSnapshot()?.screenTextDump
            ?: accessibilityState.value.screenTextDump

        if (bitmap == null && accText.isBlank()) {
            _screenVisionState.value = VisionUiState.Error(
                "বিশ্লেষণের জন্য প্রথমে 'Capture Screen' চাপুন, অথবা গ্যালারি থেকে একটি স্ক্রিনশট বেছে নিন, অথবা Accessibility Service চালু করুন।"
            )
            return
        }

        viewModelScope.launch {
            _screenVisionState.value = VisionUiState.Loading
            _orbState.value = NexoraOrbState.WORKING
            try {
                val result = aiRepository.analyzeScreen(bitmap, accText, question)
                _screenVisionState.value = VisionUiState.Success(result)
                _orbState.value = NexoraOrbState.IDLE
                repository.logActivity(
                    "SCREEN",
                    question.ifBlank { "Screen Analysis" },
                    "${result.screenTitle}: ${result.summaryBengali}"
                )
                if (settings.value.voiceAutoSpeak) {
                    voiceEngine.speak(result.summaryBengali)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _orbState.value = NexoraOrbState.IDLE
                _screenVisionState.value = VisionUiState.Error(
                    e.localizedMessage ?: "স্ক্রিন বিশ্লেষণ ব্যর্থ হয়েছে।"
                )
            }
        }
    }

    // --- Trading Assistant Module ---
    fun setTradingChartBitmap(bitmap: Bitmap?) {
        _tradingChartBitmap.value = bitmap
    }

    fun analyzeTradingChart(symbol: String, timeframe: String, notes: String) {
        val bitmap = _tradingChartBitmap.value ?: capturedScreenBitmap.value
        if (bitmap == null && symbol.isBlank() && notes.isBlank()) {
            _tradingVisionState.value = VisionUiState.Error(
                "ট্রেডিং চার্ট বিশ্লেষণ করতে একটি চার্ট স্ক্রিনশট আপলোড/ক্যাপচার করুন অথবা পেয়ার ও প্রাইস অ্যাকশন তথ্য লিখুন।"
            )
            return
        }
        viewModelScope.launch {
            _tradingVisionState.value = VisionUiState.Loading
            _orbState.value = NexoraOrbState.THINKING
            try {
                val result = aiRepository.analyzeTradingChart(bitmap, symbol, timeframe, notes)
                _tradingVisionState.value = VisionUiState.Success(result)
                _orbState.value = NexoraOrbState.IDLE
                repository.logActivity(
                    "TRADING",
                    "Chart Analysis: ${result.assetName} ($timeframe)",
                    "${result.marketTrend} | ${result.candlestickPattern} — ${result.bengaliVoiceSummary}"
                )
                if (settings.value.voiceAutoSpeak && result.bengaliVoiceSummary.isNotBlank()) {
                    voiceEngine.speak(result.bengaliVoiceSummary)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _orbState.value = NexoraOrbState.IDLE
                _tradingVisionState.value = VisionUiState.Error(
                    e.localizedMessage ?: "ট্রেডিং চার্ট বিশ্লেষণ ব্যর্থ হয়েছে।"
                )
            }
        }
    }

    fun addDemoTrade(
        symbol: String,
        side: String,
        entryPrice: Double,
        targetPrice: Double,
        stopLoss: Double,
        quantity: Double,
        notes: String,
        isLiveConfirmed: Boolean = false
    ) {
        viewModelScope.launch {
            repository.addDemoTrade(
                symbol = symbol,
                side = side,
                entryPrice = entryPrice,
                targetPrice = targetPrice,
                stopLoss = stopLoss,
                quantity = quantity,
                notes = notes,
                isLiveConfirmed = isLiveConfirmed
            )
            repository.logActivity(
                "TRADING",
                "${if (isLiveConfirmed) "Confirmed Order" else "Demo Trade"} $side $symbol",
                "Entry: $entryPrice | SL: $stopLoss | Target: $targetPrice"
            )
        }
    }

    fun closeDemoTrade(trade: DemoTradeEntity, exitPrice: Double) {
        viewModelScope.launch {
            repository.closeDemoTrade(trade, exitPrice)
        }
    }

    fun deleteDemoTrade(id: Long) {
        viewModelScope.launch {
            repository.deleteDemoTrade(id)
        }
    }

    fun saveAndTestBroker(name: String, endpointUrl: String) {
        viewModelScope.launch {
            repository.preferences.setBrokerConfig(name, endpointUrl)
            if (endpointUrl.isBlank()) {
                _brokerHealth.value = BrokerHealthResult(false, null, "Broker Endpoint সংরক্ষণ সরানো হয়েছে।")
                return@launch
            }
            _isTestingBroker.value = true
            _brokerHealth.value = repository.testBrokerEndpoint(endpointUrl)
            _isTestingBroker.value = false
        }
    }

    // --- Gaming Assistant Module ---
    fun setGamingScreenBitmap(bitmap: Bitmap?) {
        _gamingScreenBitmap.value = bitmap
    }

    fun analyzeGamingTactics(gameName: String, scenario: String) {
        val bitmap = _gamingScreenBitmap.value ?: capturedScreenBitmap.value
        viewModelScope.launch {
            _gamingVisionState.value = VisionUiState.Loading
            _orbState.value = NexoraOrbState.WORKING
            try {
                val result = aiRepository.analyzeGameScreen(bitmap, gameName, scenario)
                _gamingVisionState.value = VisionUiState.Success(result)
                _orbState.value = NexoraOrbState.IDLE
                repository.logActivity(
                    "GAMING",
                    "$gameName Tactical Coach",
                    result.bengaliVoiceCallout
                )
                if (settings.value.gamingVoiceCoachEnabled && result.bengaliVoiceCallout.isNotBlank()) {
                    voiceEngine.speak(result.bengaliVoiceCallout)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _orbState.value = NexoraOrbState.IDLE
                _gamingVisionState.value = VisionUiState.Error(
                    e.localizedMessage ?: "গেমিং বিশ্লেষণ ব্যর্থ হয়েছে।"
                )
            }
        }
    }

    fun launchFreeFire() {
        val primary = appControlExecutor.launchGameApp("com.dts.freefireth", "Free Fire")
        if (primary.success) {
            _controlFeedback.value = primary.message
        } else {
            val max = appControlExecutor.launchGameApp("com.dts.freefiremax", "Free Fire MAX")
            _controlFeedback.value = max.message
        }
    }

    // --- Floating Service & Settings ---
    fun toggleFloatingAssistant(enable: Boolean): Boolean {
        if (enable) {
            if (!PermissionHelper.checkPermissions(appContext).overlayGranted) {
                return false
            }
            FloatingAssistantService.start(appContext)
        } else {
            FloatingAssistantService.stop(appContext)
        }
        viewModelScope.launch {
            repository.preferences.setFloatingEnabled(enable)
        }
        return true
    }

    fun setAssistantEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferences.setAssistantEnabled(enabled)
            if (!enabled) {
                FloatingAssistantService.stop(appContext)
                voiceEngine.stopListening()
                voiceEngine.stopSpeaking()
                repository.preferences.setFloatingEnabled(false)
            }
        }
    }

    fun setVoiceAutoSpeak(enabled: Boolean) {
        viewModelScope.launch { repository.preferences.setVoiceAutoSpeak(enabled) }
    }

    fun setVoiceLanguage(languageTag: String) {
        viewModelScope.launch { repository.preferences.setVoiceLanguage(languageTag) }
    }

    fun setVoiceSpeed(speed: Float) {
        viewModelScope.launch { repository.preferences.setVoiceSpeed(speed) }
    }

    fun setVoiceVolume(volume: Float) {
        viewModelScope.launch { repository.preferences.setVoiceVolume(volume) }
    }

    fun setAiMemoryEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.preferences.setAiMemoryEnabled(enabled) }
    }

    fun setConfirmSensitiveApps(enabled: Boolean) {
        viewModelScope.launch { repository.preferences.setConfirmSensitiveApps(enabled) }
    }

    fun setGamingVoiceCoachEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.preferences.setGamingVoiceCoachEnabled(enabled) }
    }

    fun addMemory(label: String, detail: String, category: String) {
        viewModelScope.launch { repository.addMemory(label, detail, category) }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch { repository.deleteMemory(id) }
    }

    fun clearAllPrivacyData() {
        viewModelScope.launch {
            repository.clearAllActivities()
            repository.clearAllMemories()
            ScreenCaptureService.setPickedBitmap(null)
            _tradingChartBitmap.value = null
            _gamingScreenBitmap.value = null
            _assistantState.value = AssistantUiState.Idle
            _screenVisionState.value = VisionUiState.Idle
        }
    }

    fun clearControlFeedback() {
        _controlFeedback.value = null
    }

    override fun onCleared() {
        voiceEngine.release()
        super.onCleared()
    }
}
