package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

private const val TAG = "NexoraVoiceEngine"

class NexoraVoiceEngine(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null

    private val _isTtsReady = MutableStateFlow(false)
    val isTtsReady: StateFlow<Boolean> = _isTtsReady.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _activeVoiceLabel = MutableStateFlow("Initializing Bengali Voice...")
    val activeVoiceLabel: StateFlow<String> = _activeVoiceLabel.asStateFlow()

    private var speechRate: Float = 1.0f
    private var speechVolume: Float = 0.9f
    private var languageTag: String = "bn-BD"

    init {
        initTextToSpeech()
    }

    private fun initTextToSpeech() {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                _isTtsReady.value = true
                configureVoice(languageTag, speechRate, speechVolume)
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                })
            } else {
                Log.e(TAG, "TextToSpeech init failed with status=$status")
                _activeVoiceLabel.value = "TTS Unavailable on Device"
            }
        }
    }

    fun configureVoice(language: String, speed: Float, volume: Float) {
        languageTag = language
        speechRate = speed.coerceIn(0.5f, 2.0f)
        speechVolume = volume.coerceIn(0.1f, 1.0f)
        val engine = tts ?: return
        if (!_isTtsReady.value) return

        val desiredLocale = if (language.startsWith("bn")) {
            Locale.forLanguageTag("bn-BD")
        } else {
            Locale.US
        }

        val availability = engine.isLanguageAvailable(desiredLocale)
        if (availability >= TextToSpeech.LANG_AVAILABLE) {
            engine.language = desiredLocale
        } else if (language.startsWith("bn")) {
            val bnIn = Locale.forLanguageTag("bn-IN")
            if (engine.isLanguageAvailable(bnIn) >= TextToSpeech.LANG_AVAILABLE) {
                engine.language = bnIn
            } else {
                engine.language = Locale.US
            }
        } else {
            engine.language = Locale.US
        }

        // Prefer a clear female Bengali / English voice when available on the device TTS engine
        try {
            val voices = engine.voices
            if (!voices.isNullOrEmpty()) {
                val targetLangPrefix = if (language.startsWith("bn")) "bn" else "en"
                val matchingLang = voices.filter {
                    it.locale.language.equals(targetLangPrefix, ignoreCase = true) && !it.isNetworkConnectionRequired
                }.ifEmpty {
                    voices.filter { it.locale.language.equals(targetLangPrefix, ignoreCase = true) }
                }

                val preferredFemale = matchingLang.firstOrNull { v ->
                    val n = v.name.lowercase()
                    n.contains("female") || n.contains("bn-bd-x-ban") || n.contains("bn-in-x-bnf") ||
                        n.contains("sfg") || n.contains("tpf") || n.contains("f00")
                } ?: matchingLang.firstOrNull()

                if (preferredFemale != null) {
                    engine.voice = preferredFemale
                    _activeVoiceLabel.value = "${preferredFemale.locale.displayName} (Female AI)"
                } else {
                    _activeVoiceLabel.value = "${engine.voice?.locale?.displayName ?: desiredLocale.displayName}"
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Voice selection fallback used", e)
            _activeVoiceLabel.value = desiredLocale.displayName
        }

        engine.setSpeechRate(speechRate)
        // Warm, natural female pitch tuning
        engine.setPitch(1.08f)
    }

    fun speak(text: String, flush: Boolean = true) {
        val cleanText = text
            .replace("*", "")
            .replace("#", "")
            .replace("`", "")
            .trim()
        if (cleanText.isBlank()) return
        val engine = tts ?: return
        if (!_isTtsReady.value) return

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, speechVolume)
        }
        val queueMode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        engine.speak(cleanText.take(1200), queueMode, params, UUID.randomUUID().toString())
    }

    fun stopSpeaking() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun startListening(
        preferredLanguage: String,
        onFinalResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("এই ডিভাইসে Google Speech Recognition সার্ভিস পাওয়া যায়নি। টেক্সট কমান্ড ব্যবহার করুন।")
            return
        }

        stopSpeaking()
        stopListening()

        try {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer = recognizer

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                    _partialTranscript.value = ""
                }

                override fun onBeginningOfSpeech() {
                    _isListening.value = true
                }

                override fun onRmsChanged(rmsdB: Float) {
                    val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                    _rmsLevel.value = normalized
                }

                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() {
                    _isListening.value = false
                    _rmsLevel.value = 0f
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    _rmsLevel.value = 0f
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "অডিও রেকর্ড করতে সমস্যা হয়েছে।"
                        SpeechRecognizer.ERROR_NO_MATCH -> "ভয়েস বোঝা যায়নি, অনুগ্রহ করে আবার বলুন।"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "কোনো ভয়েস শোনা যায়নি। আবার মাইক চাপুন।"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "মাইক্রোফোন পারমিশন অনুমোদিত নয়।"
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "ভয়েস রিকগনিশনের জন্য নেটওয়ার্ক সংযোগ প্রয়োজন।"
                        else -> "ভয়েস ইনপুট সম্পন্ন করা যায়নি (Code $error)।"
                    }
                    onError(msg)
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    _rmsLevel.value = 0f
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spoken = matches?.firstOrNull().orEmpty().trim()
                    _partialTranscript.value = spoken
                    if (spoken.isNotBlank()) {
                        onFinalResult(spoken)
                    } else {
                        onError("ভয়েস টেক্সট খালি ছিল। আবার চেষ্টা করুন।")
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    if (partial.isNotBlank()) {
                        _partialTranscript.value = partial
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, preferredLanguage)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, preferredLanguage)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            recognizer.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start SpeechRecognizer", e)
            _isListening.value = false
            onError("ভয়েস লিসেনিং শুরু করা যায়নি: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping SpeechRecognizer", e)
        } finally {
            speechRecognizer = null
            _isListening.value = false
            _rmsLevel.value = 0f
        }
    }

    fun release() {
        stopListening()
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
