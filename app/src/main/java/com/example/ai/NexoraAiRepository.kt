package com.example.ai

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "NexoraAiRepository"
private const val MODEL_DEFAULT = "gemini-3.8-flash"
private const val AI_TIMEOUT_MS = 60_000L

data class TaskPlanStep(
    val action: String, // OPEN_YOUTUBE, OPEN_WHATSAPP, OPEN_CHROME, OPEN_SETTINGS, SCROLL_DOWN, SCROLL_UP, CLICK_TEXT, TYPE_TEXT, NAVIGATE_BACK, NAVIGATE_HOME, ANALYZE_SCREEN, REPLY_ONLY
    val target: String,
    val detail: String
)

data class TaskPlanResult(
    val intentSummary: String,
    val bengaliReply: String,
    val englishSummary: String,
    val isSensitive: Boolean,
    val requiresConfirmation: Boolean,
    val steps: List<TaskPlanStep>
)

data class ScreenAnalysisResult(
    val screenTitle: String,
    val summaryBengali: String,
    val detectedElements: List<String>,
    val suggestedActions: List<String>
)

data class TradingAnalysisResult(
    val assetName: String,
    val candlestickPattern: String,
    val marketTrend: String, // BULLISH, BEARISH, SIDEWAYS
    val signalBias: String, // BUY_ZONE, SELL_ZONE, WAIT
    val signalExplanation: String,
    val entryZone: String,
    val stopLossAdvice: String,
    val targetAdvice: String,
    val riskRewardNote: String,
    val bengaliVoiceSummary: String
)

data class GamingTacticalResult(
    val enemyStatus: String,
    val enemyDirection: String,
    val hpAndMedkitAlert: String,
    val glooWallAdvice: String,
    val survivalStrategy: String,
    val performanceTip: String,
    val bengaliVoiceCallout: String
)

class NexoraAiRepository {

    private val conversationalModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_DEFAULT,
            systemInstruction = content {
                text(
                    """
                    You are NEXORA AI — "Your Intelligent Digital Companion", a premium futuristic Bengali & English female AI assistant for Android.
                    - Respond naturally, warmly, concisely, and intelligently in Bengali (বাংলা) by default, or match English if the user writes purely in English.
                    - Never invent fake system actions; be transparent if an Android permission (Accessibility, Overlay, Screen Capture, Microphone) is needed.
                    - For trading questions, NEVER guarantee profit and always remind the user of market risk.
                    - For gaming questions (like Free Fire), provide fair-play tactical coaching only; strictly refuse cheats, aimbots, auto-headshot, or anti-cheat bypass.
                    """.trimIndent()
                )
            }
        )
    }

    private val taskPlannerModel by lazy {
        val schema = Schema.obj(
            properties = mapOf(
                "intentSummary" to Schema.string(description = "Short intent summary"),
                "bengaliReply" to Schema.string(description = "Natural friendly response in Bengali (or English if requested)"),
                "englishSummary" to Schema.string(description = "1-line English summary"),
                "isSensitive" to Schema.boolean(description = "True if command involves banking, passwords, payments, sending messages, or live financial trading"),
                "requiresConfirmation" to Schema.boolean(description = "True if user confirmation is needed before executing"),
                "steps" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "action" to Schema.enumeration(
                                values = listOf(
                                    "OPEN_YOUTUBE",
                                    "OPEN_WHATSAPP",
                                    "OPEN_CHROME",
                                    "OPEN_SETTINGS",
                                    "SCROLL_DOWN",
                                    "SCROLL_UP",
                                    "CLICK_TEXT",
                                    "TYPE_TEXT",
                                    "NAVIGATE_BACK",
                                    "NAVIGATE_HOME",
                                    "ANALYZE_SCREEN",
                                    "REPLY_ONLY"
                                ),
                                description = "Safe Android action type"
                            ),
                            "target" to Schema.string(description = "Search query, URL, setting name (wifi/bluetooth/display/accessibility/sound), button text, or message text"),
                            "detail" to Schema.string(description = "Human-readable explanation of this step in Bengali")
                        )
                    ),
                    description = "Ordered multi-step plan to complete the user request"
                )
            )
        )
        Firebase.ai.generativeModel(
            modelName = MODEL_DEFAULT,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    private val screenVisionModel by lazy {
        val schema = Schema.obj(
            properties = mapOf(
                "screenTitle" to Schema.string(description = "Detected app or screen title"),
                "summaryBengali" to Schema.string(description = "Clear explanation in Bengali of what is happening on screen and direct answer to the user's question"),
                "detectedElements" to Schema.array(
                    items = Schema.string(),
                    description = "Key visible texts, buttons, prices, or UI elements detected"
                ),
                "suggestedActions" to Schema.array(
                    items = Schema.string(),
                    description = "2 to 4 helpful next actions the user can take"
                )
            )
        )
        Firebase.ai.generativeModel(
            modelName = MODEL_DEFAULT,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    private val tradingVisionModel by lazy {
        val schema = Schema.obj(
            properties = mapOf(
                "assetName" to Schema.string(description = "Detected asset/pair/ticker or 'Chart Asset'"),
                "candlestickPattern" to Schema.string(description = "Recognized candlestick or chart pattern"),
                "marketTrend" to Schema.enumeration(
                    values = listOf("BULLISH", "BEARISH", "SIDEWAYS"),
                    description = "Overall observed trend"
                ),
                "signalBias" to Schema.enumeration(
                    values = listOf("BUY_ZONE", "SELL_ZONE", "WAIT"),
                    description = "Educational directional bias based on structure"
                ),
                "signalExplanation" to Schema.string(description = "Detailed Bengali technical explanation with clear risk disclaimer that profit is never guaranteed"),
                "entryZone" to Schema.string(description = "Observed support/resistance or educational entry area"),
                "stopLossAdvice" to Schema.string(description = "Risk-management Stop Loss placement concept"),
                "targetAdvice" to Schema.string(description = "Educational Take Profit target zone"),
                "riskRewardNote" to Schema.string(description = "Risk-reward ratio guidance"),
                "bengaliVoiceSummary" to Schema.string(description = "Short 2-sentence natural Bengali voice summary including risk reminder")
            )
        )
        Firebase.ai.generativeModel(
            modelName = MODEL_DEFAULT,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    private val gamingVisionModel by lazy {
        val schema = Schema.obj(
            properties = mapOf(
                "enemyStatus" to Schema.string(description = "Whether enemies or danger indicators are visible on screen"),
                "enemyDirection" to Schema.string(description = "Estimated direction / cover angle (e.g., Left Ridge 10 o'clock, Front Window, Clear)"),
                "hpAndMedkitAlert" to Schema.string(description = "HP bar observation and Medkit/Inhaler recommendation in Bengali"),
                "glooWallAdvice" to Schema.string(description = "When and where to place Gloo Wall for cover in Bengali"),
                "survivalStrategy" to Schema.string(description = "Zone rotation and tactical positioning strategy in Bengali"),
                "performanceTip" to Schema.string(description = "Fair-play HUD, movement, or sensitivity tip in Bengali"),
                "bengaliVoiceCallout" to Schema.string(description = "Urgent, short 1-2 sentence Bengali gaming voice callout as a female tactical coach")
            )
        )
        Firebase.ai.generativeModel(
            modelName = MODEL_DEFAULT,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    fun streamAssistantResponse(
        userPrompt: String,
        memories: List<String>,
        screenContext: String,
        preferredLanguage: String
    ): Flow<String> = flow {
        val memorySection = if (memories.isNotEmpty()) {
            "User AI Memory:\n" + memories.joinToString("\n") { "- $it" }
        } else {
            "No saved memory entries."
        }
        val screenSection = if (screenContext.isNotBlank()) {
            "Current Accessibility Screen Context:\n${screenContext.take(1500)}"
        } else {
            "No active background screen context."
        }
        val langHint = if (preferredLanguage.startsWith("bn")) {
            "Reply primarily in natural, warm Bengali (বাংলা)."
        } else {
            "Reply clearly in English (or Bengali if user wrote in Bengali)."
        }

        val composedPrompt = """
            $langHint
            $memorySection
            $screenSection

            User Request: $userPrompt
        """.trimIndent()

        val accumulator = StringBuilder()
        withTimeout(AI_TIMEOUT_MS) {
            conversationalModel.generateContentStream(composedPrompt).collect { chunk ->
                val part = chunk.text.orEmpty()
                if (part.isNotEmpty()) {
                    accumulator.append(part)
                    emit(accumulator.toString())
                }
            }
        }
        if (accumulator.isBlank()) {
            throw IllegalStateException("AI থেকে কোনো উত্তর পাওয়া যায়নি। আবার চেষ্টা করুন।")
        }
    }

    suspend fun planTask(
        command: String,
        memories: List<String>,
        currentForegroundApp: String,
        visibleScreenText: String
    ): TaskPlanResult {
        return try {
            val prompt = """
                Plan a safe, transparent step-by-step execution for this Android user command.
                User Command: "$command"
                Current Foreground App: "$currentForegroundApp"
                Visible Screen Text Snippet: "${visibleScreenText.take(1000)}"
                Saved User Memories: ${memories.joinToString("; ")}

                Rules:
                - If the user asks to open YouTube and play/search something, use OPEN_YOUTUBE with target = search query.
                - If the user asks to open WhatsApp or write a WhatsApp message, use OPEN_WHATSAPP with target = message text, and mark isSensitive=true, requiresConfirmation=true.
                - If the user asks to open a website or Chrome, use OPEN_CHROME with target = full https URL or search query.
                - If the user asks to open Settings (wifi, bluetooth, display, accessibility, sound, battery), use OPEN_SETTINGS with target = setting keyword.
                - If the user asks to scroll, tap a button, type text, go back, or go home, use SCROLL_DOWN / SCROLL_UP / CLICK_TEXT / TYPE_TEXT / NAVIGATE_BACK / NAVIGATE_HOME.
                - If the user asks what is on screen, use ANALYZE_SCREEN.
                - If it's general conversation or Q&A, use REPLY_ONLY.
                - Never attempt cheats or unauthorized actions.
            """.trimIndent()

            val response = withTimeout(AI_TIMEOUT_MS) {
                taskPlannerModel.generateContent(prompt)
            }
            val raw = response.text ?: throw IllegalStateException("Empty task plan response.")
            val json = JSONObject(raw)
            val stepsArr = json.optJSONArray("steps")
            val steps = buildList {
                if (stepsArr != null) {
                    for (i in 0 until stepsArr.length()) {
                        val obj = stepsArr.getJSONObject(i)
                        add(
                            TaskPlanStep(
                                action = obj.optString("action", "REPLY_ONLY"),
                                target = obj.optString("target", ""),
                                detail = obj.optString("detail", "")
                            )
                        )
                    }
                }
            }
            TaskPlanResult(
                intentSummary = json.optString("intentSummary", "AI Command"),
                bengaliReply = json.optString("bengaliReply", "কমান্ড প্রস্তুত হয়েছে।"),
                englishSummary = json.optString("englishSummary", ""),
                isSensitive = json.optBoolean("isSensitive", false),
                requiresConfirmation = json.optBoolean("requiresConfirmation", false),
                steps = steps
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Task planning timed out", e)
            throw IllegalStateException("AI পরিকল্পনা সময়সীমা (60s) অতিক্রম করেছে। আবার চেষ্টা করুন।", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Task planning failed", e)
            throw e
        }
    }

    suspend fun analyzeScreen(
        bitmap: Bitmap?,
        accessibilityText: String,
        userQuestion: String
    ): ScreenAnalysisResult {
        return try {
            val scaledBitmap = bitmap?.let { scaleBitmapForAi(it, 1280) }
            val question = userQuestion.ifBlank { "এই স্ক্রিনে কী দেখা যাচ্ছে, মূল UI উপাদান এবং পরবর্তী করণীয় কী তা ব্যাখ্যা করো।" }
            val response = withTimeout(AI_TIMEOUT_MS) {
                screenVisionModel.generateContent(
                    content {
                        scaledBitmap?.let { image(it) }
                        text(
                            """
                            Analyze this Android screen (using both image OCR/Vision if provided and accessibility UI text).
                            Accessibility UI Tree Text: ${accessibilityText.take(2500).ifBlank { "Not available" }}
                            User Question: $question
                            Respond in clear, structured Bengali with detected UI elements and practical next actions.
                            """.trimIndent()
                        )
                    }
                )
            }
            val raw = response.text ?: throw IllegalStateException("Empty screen analysis response.")
            val json = JSONObject(raw)
            ScreenAnalysisResult(
                screenTitle = json.optString("screenTitle", "Screen Analysis"),
                summaryBengali = json.optString("summaryBengali", ""),
                detectedElements = json.optStringList("detectedElements"),
                suggestedActions = json.optStringList("suggestedActions")
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Screen analysis timed out", e)
            throw IllegalStateException("স্ক্রিন বিশ্লেষণ সময়সীমা অতিক্রম করেছে।", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Screen analysis failed", e)
            throw e
        }
    }

    suspend fun analyzeTradingChart(
        bitmap: Bitmap?,
        symbolHint: String,
        timeframeHint: String,
        extraNotes: String
    ): TradingAnalysisResult {
        return try {
            val scaledBitmap = bitmap?.let { scaleBitmapForAi(it, 1440) }
            val response = withTimeout(AI_TIMEOUT_MS) {
                tradingVisionModel.generateContent(
                    content {
                        scaledBitmap?.let { image(it) }
                        text(
                            """
                            You are NEXORA AI Trading Assistant. Analyze the provided trading chart image and/or market context.
                            Asset/Symbol Hint: ${symbolHint.ifBlank { "Detect from chart" }}
                            Timeframe Hint: ${timeframeHint.ifBlank { "Auto" }}
                            Trader Question / Context: ${extraNotes.ifBlank { "Analyze candlestick pattern, trend, support/resistance, educational buy/sell bias, Stop Loss, and Target." }}

                            MANDATORY SAFETY RULE:
                            - Never promise or guarantee profits.
                            - Explicitly mention market risk and that live execution requires manual trader confirmation.
                            - Provide educational technical analysis in clear Bengali.
                            """.trimIndent()
                        )
                    }
                )
            }
            val raw = response.text ?: throw IllegalStateException("Empty trading analysis response.")
            val json = JSONObject(raw)
            TradingAnalysisResult(
                assetName = json.optString("assetName", symbolHint.ifBlank { "Market Chart" }),
                candlestickPattern = json.optString("candlestickPattern", "Price Action Structure"),
                marketTrend = json.optString("marketTrend", "SIDEWAYS"),
                signalBias = json.optString("signalBias", "WAIT"),
                signalExplanation = json.optString("signalExplanation", ""),
                entryZone = json.optString("entryZone", "Confirm breakout / support"),
                stopLossAdvice = json.optString("stopLossAdvice", "Below recent swing structure"),
                targetAdvice = json.optString("targetAdvice", "Next key resistance/support"),
                riskRewardNote = json.optString("riskRewardNote", "Minimum 1:2 R:R"),
                bengaliVoiceSummary = json.optString("bengaliVoiceSummary", "")
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Trading analysis timed out", e)
            throw IllegalStateException("চার্ট বিশ্লেষণ সময়সীমা অতিক্রম করেছে।", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Trading analysis failed", e)
            throw e
        }
    }

    suspend fun analyzeGameScreen(
        bitmap: Bitmap?,
        gameName: String,
        scenarioPrompt: String
    ): GamingTacticalResult {
        return try {
            val scaledBitmap = bitmap?.let { scaleBitmapForAi(it, 1280) }
            val response = withTimeout(AI_TIMEOUT_MS) {
                gamingVisionModel.generateContent(
                    content {
                        scaledBitmap?.let { image(it) }
                        text(
                            """
                            You are NEXORA AI Fair-Play Bengali Female Gaming Coach for $gameName (e.g., Free Fire / Battle Royale).
                            Analyze the gameplay screenshot (if provided) and tactical situation:
                            Situation / User Query: ${scenarioPrompt.ifBlank { "Check visible enemies, enemy direction, HP bar, Medkit urgency, Gloo Wall cover timing, and survival strategy." }}

                            STRICT FAIR-PLAY POLICY:
                            - Provide legitimate human tactical coaching, HUD awareness, positioning, Medkit & Gloo Wall timing, and performance tips ONLY.
                            - Never provide cheats, hacks, aimbots, auto-headshot macros, or anti-cheat bypass.
                            - Keep bengaliVoiceCallout crisp, energetic, and natural in Bengali so TTS can speak it immediately!
                            """.trimIndent()
                        )
                    }
                )
            }
            val raw = response.text ?: throw IllegalStateException("Empty gaming analysis response.")
            val json = JSONObject(raw)
            GamingTacticalResult(
                enemyStatus = json.optString("enemyStatus", "স্ক্রিন পর্যবেক্ষণ সম্পন্ন"),
                enemyDirection = json.optString("enemyDirection", "কভার বজায় রাখুন"),
                hpAndMedkitAlert = json.optString("hpAndMedkitAlert", "HP চেক করুন ও নিরাপদ কভারে Medkit নিন"),
                glooWallAdvice = json.optString("glooWallAdvice", "খোলা জায়গায় ফায়ার এলে দ্রুত Gloo Wall দিন"),
                survivalStrategy = json.optString("survivalStrategy", "সেফ জোন এজ ধরে হাই-গ্রাউন্ড পজিশন নিন"),
                performanceTip = json.optString("performanceTip", "স্মুথ গ্রাফিক্স ও হাই FPS ব্যবহার করুন"),
                bengaliVoiceCallout = json.optString("bengaliVoiceCallout", "সতর্ক থাকো! কভার নিয়ে HP চেক করো এবং সেফ জোনের দিকে এগোও।")
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gaming analysis timed out", e)
            throw IllegalStateException("গেম স্ক্রিন বিশ্লেষণ সময়সীমা অতিক্রম করেছে।", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gaming analysis failed", e)
            throw e
        }
    }

    private fun JSONObject.optStringList(key: String): List<String> {
        val arr = optJSONArray(key) ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val item = arr.optString(i)
                if (item.isNotBlank()) add(item)
            }
        }
    }

    private fun scaleBitmapForAi(source: Bitmap, maxDimension: Int): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= maxDimension && height <= maxDimension) return source
        val ratio = maxDimension.toFloat() / maxOf(width, height).toFloat()
        val targetW = (width * ratio).toInt().coerceAtLeast(1)
        val targetH = (height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, targetW, targetH, true)
    }
}
