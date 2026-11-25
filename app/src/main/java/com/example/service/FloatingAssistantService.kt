package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.ai.NexoraAiRepository
import com.example.data.NexoraRepository
import com.example.voice.NexoraVoiceEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val TAG = "FloatingAssistantSvc"
private const val FLOATING_CHANNEL_ID = "nexora_floating_assistant_channel"
private const val FLOATING_NOTIFICATION_ID = 2001

class FloatingAssistantService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var windowManager: WindowManager
    private lateinit var aiRepository: NexoraAiRepository
    private lateinit var dataRepository: NexoraRepository
    private lateinit var voiceEngine: NexoraVoiceEngine
    private lateinit var appControlExecutor: AppControlExecutor

    private var bubbleView: View? = null
    private var bubbleParams: WindowManager.LayoutParams? = null

    private var panelView: View? = null
    private var panelParams: WindowManager.LayoutParams? = null
    private var isPanelExpanded = false
    private var isPanelLarge = false

    private var responseTextView: TextView? = null
    private var statusBadgeView: TextView? = null
    private var commandInput: EditText? = null
    private var lastAiText: String = "হ্যালো! আমি NEXORA AI। ভয়েস বা টেক্সটে নির্দেশ দিন অথবা স্ক্রিন বিশ্লেষণ করুন।"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        aiRepository = NexoraAiRepository()
        dataRepository = NexoraRepository(this)
        voiceEngine = NexoraVoiceEngine(this)
        appControlExecutor = AppControlExecutor(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "SYSTEM_ALERT_WINDOW permission not granted.")
            _isRunning.value = false
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundNotification()
        _isRunning.value = true

        if (bubbleView == null) {
            attachFloatingBubble()
        }
        return START_STICKY
    }

    private fun startForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FLOATING_CHANNEL_ID,
                getString(R.string.floating_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, FloatingAssistantService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, FLOATING_CHANNEL_ID)
            .setContentTitle(getString(R.string.floating_notification_title))
            .setContentText(getString(R.string.floating_notification_text))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openAppIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                FLOATING_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(FLOATING_NOTIFICATION_ID, notification)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachFloatingBubble() {
        val bubbleSizePx = dp(60)
        val container = FrameLayout(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.parseColor("#00E5FF"), Color.parseColor("#8B5CF6"))
            ).apply {
                shape = GradientDrawable.OVAL
                setStroke(dp(2), Color.parseColor("#F8FAFC"))
            }
            elevation = dp(10).toFloat()
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }

        val logoImage = ImageView(this).apply {
            setImageResource(R.drawable.img_nexora_logo_transparent)
            scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = "NEXORA AI Floating Bubble"
        }
        container.addView(
            logoImage,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            bubbleSizePx,
            bubbleSizePx,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(16)
            y = dp(240)
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isClick = true

        container.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isClick = true
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > 10 || abs(dy) > 10) {
                        isClick = false
                    }
                    params.x = initialX + dx
                    params.y = initialY + dy
                    windowManager.updateViewLayout(container, params)
                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        toggleFloatingPanel()
                    } else {
                        // Snap bubble cleanly to nearest horizontal screen edge
                        val screenWidth = resources.displayMetrics.widthPixels
                        params.x = if (params.x + bubbleSizePx / 2 < screenWidth / 2) {
                            dp(10)
                        } else {
                            screenWidth - bubbleSizePx - dp(10)
                        }
                        windowManager.updateViewLayout(container, params)
                    }
                    true
                }

                else -> false
            }
        }

        bubbleView = container
        bubbleParams = params
        windowManager.addView(container, params)
    }

    private fun toggleFloatingPanel() {
        if (isPanelExpanded) {
            removeFloatingPanel()
        } else {
            attachFloatingPanel()
        }
    }

    private fun attachFloatingPanel() {
        if (panelView != null) return
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F00B1426"))
                cornerRadius = dp(20).toFloat()
                setStroke(dp(1), Color.parseColor("#00E5FF"))
            }
            setPadding(dp(14), dp(12), dp(14), dp(12))
            elevation = dp(14).toFloat()
        }

        // Header Row
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleCol.addView(TextView(this).apply {
            text = "NEXORA AI"
            setTextColor(Color.parseColor("#00E5FF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = Typeface.DEFAULT_BOLD
        })
        val statusText = TextView(this).apply {
            text = "Ready • Floating Companion"
            setTextColor(Color.parseColor("#94A3B8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        }
        statusBadgeView = statusText
        titleCol.addView(statusText)
        header.addView(titleCol)

        val resizeBtn = createChipButton("Expand") {
            isPanelLarge = !isPanelLarge
            updatePanelDimensions()
        }
        val closeBtn = createChipButton("Hide") {
            removeFloatingPanel()
        }
        header.addView(resizeBtn)
        header.addView(closeBtn)
        root.addView(header)

        // Response Scroll Area
        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(130)
            ).apply { topMargin = dp(8); bottomMargin = dp(8) }
        }
        val responseView = TextView(this).apply {
            text = lastAiText
            setTextColor(Color.parseColor("#F8FAFC"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#111E36"))
                cornerRadius = dp(12).toFloat()
            }
        }
        responseTextView = responseView
        scroll.addView(responseView)
        root.addView(scroll)

        // Input Row
        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val editText = EditText(this).apply {
            hint = "বাংলা বা ইংরেজিতে কমান্ড লিখুন..."
            setHintTextColor(Color.parseColor("#64748B"))
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#172745"))
                cornerRadius = dp(12).toFloat()
            }
            setPadding(dp(10), dp(8), dp(10), dp(8))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        commandInput = editText
        inputRow.addView(editText)

        val sendBtn = createChipButton("Send", Color.parseColor("#00E5FF"), Color.parseColor("#001F26")) {
            val prompt = editText.text.toString().trim()
            if (prompt.isNotBlank()) {
                editText.setText("")
                handleFloatingCommand(prompt)
            }
        }
        inputRow.addView(sendBtn)
        root.addView(inputRow)

        // Quick Action Row
        val actionsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        }

        actionsRow.addView(createChipButton("Voice", Color.parseColor("#8B5CF6"), Color.WHITE) {
            triggerFloatingVoice()
        })
        actionsRow.addView(createChipButton("Screen AI", Color.parseColor("#172745"), Color.parseColor("#00E5FF")) {
            handleAnalyzeCurrentScreen()
        })
        actionsRow.addView(createChipButton("Speak", Color.parseColor("#172745"), Color.WHITE) {
            voiceEngine.speak(lastAiText)
        })
        actionsRow.addView(createChipButton("Off", Color.parseColor("#470D1B"), Color.parseColor("#F43F5E")) {
            serviceScope.launch {
                dataRepository.preferences.setFloatingEnabled(false)
                stopSelf()
            }
        })
        root.addView(actionsRow)

        val panelWidth = (resources.displayMetrics.widthPixels * 0.88f).toInt()
        val params = WindowManager.LayoutParams(
            panelWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
            y = dp(96)
        }

        panelView = root
        panelParams = params
        isPanelExpanded = true
        windowManager.addView(root, params)
    }

    private fun updatePanelDimensions() {
        val view = panelView ?: return
        val params = panelParams ?: return
        val screenW = resources.displayMetrics.widthPixels
        params.width = if (isPanelLarge) (screenW * 0.95f).toInt() else (screenW * 0.86f).toInt()
        windowManager.updateViewLayout(view, params)
    }

    private fun removeFloatingPanel() {
        panelView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                Log.w(TAG, "Error removing panel", e)
            }
        }
        panelView = null
        panelParams = null
        isPanelExpanded = false
    }

    private fun triggerFloatingVoice() {
        statusBadgeView?.text = "Listening..."
        serviceScope.launch {
            val settings = dataRepository.settings.first()
            voiceEngine.configureVoice(settings.voiceLanguage, settings.voiceSpeed, settings.voiceVolume)
            voiceEngine.startListening(
                preferredLanguage = settings.voiceLanguage,
                onFinalResult = { spoken ->
                    commandInput?.setText(spoken)
                    handleFloatingCommand(spoken)
                },
                onError = { err ->
                    statusBadgeView?.text = "Voice Notice"
                    responseTextView?.text = err
                }
            )
        }
    }

    private fun handleFloatingCommand(prompt: String) {
        statusBadgeView?.text = "Thinking..."
        responseTextView?.text = "NEXORA AI বিশ্লেষণ করছে..."
        serviceScope.launch {
            try {
                val settings = dataRepository.settings.first()
                if (!settings.assistantEnabled) {
                    val paused = "AI Assistant বর্তমানে বন্ধ (Paused) আছে। Settings থেকে চালু করুন।"
                    lastAiText = paused
                    responseTextView?.text = paused
                    statusBadgeView?.text = "Paused"
                    return@launch
                }
                voiceEngine.configureVoice(settings.voiceLanguage, settings.voiceSpeed, settings.voiceVolume)
                val memories = if (settings.aiMemoryEnabled) dataRepository.getMemoryContextStrings() else emptyList()
                val screenState = NexoraAccessibilityService.screenState.value

                val plan = aiRepository.planTask(
                    command = prompt,
                    memories = memories,
                    currentForegroundApp = screenState.foregroundPackage,
                    visibleScreenText = screenState.screenTextDump
                )

                val executionNotes = mutableListOf<String>()
                if (plan.requiresConfirmation && settings.confirmSensitiveApps) {
                    executionNotes.add("সতর্কতা: এটি একটি সেন্সিটিভ কাজ — মূল NEXORA অ্যাপে নিশ্চিত করে চালান।")
                } else {
                    for (step in plan.steps) {
                        if (step.action != "REPLY_ONLY") {
                            val report = appControlExecutor.executeStep(step)
                            executionNotes.add(report.message)
                        }
                    }
                }

                val combined = buildString {
                    append(plan.bengaliReply)
                    if (executionNotes.isNotEmpty()) {
                        append("\n\n")
                        append(executionNotes.joinToString("\n• ", prefix = "• "))
                    }
                }
                lastAiText = combined
                responseTextView?.text = combined
                statusBadgeView?.text = "Done"
                dataRepository.logActivity("ASSISTANT", prompt, combined)
                if (settings.voiceAutoSpeak) {
                    voiceEngine.speak(plan.bengaliReply)
                }
            } catch (e: Exception) {
                val err = "AI ত্রুটি: ${e.localizedMessage ?: "অনুরোধ ব্যর্থ হয়েছে"}"
                lastAiText = err
                responseTextView?.text = err
                statusBadgeView?.text = "Error"
            }
        }
    }

    private fun handleAnalyzeCurrentScreen() {
        statusBadgeView?.text = "Reading Screen..."
        serviceScope.launch {
            val accService = NexoraAccessibilityService.instance
            val snapshot = accService?.refreshScreenSnapshot()
            val textDump = snapshot?.screenTextDump.orEmpty()
            if (textDump.isBlank()) {
                val notice = "স্ক্রিন এলিমেন্ট পড়ার জন্য Accessibility Service চালু করুন বা মূল অ্যাপ থেকে Screen Capture ব্যবহার করুন।"
                lastAiText = notice
                responseTextView?.text = notice
                statusBadgeView?.text = "Permission Needed"
                return@launch
            }
            try {
                val result = aiRepository.analyzeScreen(
                    bitmap = null,
                    accessibilityText = textDump,
                    userQuestion = "এই স্ক্রিনে কী চলছে তা সংক্ষেপে বাংলায় বলো।"
                )
                lastAiText = "${result.screenTitle}\n\n${result.summaryBengali}"
                responseTextView?.text = lastAiText
                statusBadgeView?.text = "Analyzed"
                dataRepository.logActivity("SCREEN", "Floating Screen Scan", lastAiText)
                val settings = dataRepository.settings.first()
                if (settings.voiceAutoSpeak) {
                    voiceEngine.configureVoice(settings.voiceLanguage, settings.voiceSpeed, settings.voiceVolume)
                    voiceEngine.speak(result.summaryBengali)
                }
            } catch (e: Exception) {
                responseTextView?.text = "বিশ্লেষণ ব্যর্থ: ${e.localizedMessage ?: "Error"}"
                statusBadgeView?.text = "Error"
            }
        }
    }

    private fun createChipButton(
        label: String,
        bgColor: Int = Color.parseColor("#172745"),
        textColor: Int = Color.parseColor("#F8FAFC"),
        onClick: () -> Unit
    ): Button {
        return Button(this).apply {
            text = label
            isAllCaps = false
            setTextColor(textColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            background = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = dp(10).toFloat()
            }
            minWidth = 0
            minimumWidth = 0
            minHeight = dp(34)
            minimumHeight = dp(34)
            setPadding(dp(10), dp(4), dp(10), dp(4))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = dp(6)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()

    override fun onDestroy() {
        removeFloatingPanel()
        bubbleView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                Log.w(TAG, "Error removing bubble", e)
            }
        }
        bubbleView = null
        voiceEngine.release()
        serviceScope.cancel()
        _isRunning.value = false
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.example.service.ACTION_STOP_FLOATING"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, FloatingAssistantService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingAssistantService::class.java))
        }
    }
}
