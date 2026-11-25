package com.example.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.example.ai.TaskPlanStep

data class StepExecutionReport(
    val step: TaskPlanStep,
    val success: Boolean,
    val message: String
)

class AppControlExecutor(private val context: Context) {

    fun executeStep(step: TaskPlanStep): StepExecutionReport {
        return when (step.action.uppercase()) {
            "OPEN_YOUTUBE" -> openYouTube(step)
            "OPEN_WHATSAPP" -> openWhatsApp(step)
            "OPEN_CHROME" -> openChrome(step)
            "OPEN_SETTINGS" -> openSystemSetting(step)
            "SCROLL_DOWN" -> runAccessibility(step) { it.executeScrollForward() }
            "SCROLL_UP" -> runAccessibility(step) { it.executeScrollBackward() }
            "CLICK_TEXT" -> runAccessibility(step) { it.executeClickByText(step.target) }
            "TYPE_TEXT" -> runAccessibility(step) { it.executeInputText(step.target) }
            "NAVIGATE_BACK" -> runAccessibility(step) { it.executeGlobalBack() }
            "NAVIGATE_HOME" -> runAccessibility(step) { it.executeGlobalHome() }
            "ANALYZE_SCREEN" -> StepExecutionReport(
                step = step,
                success = true,
                message = "স্ক্রিন বিশ্লেষণ মডিউলে পাঠানো হয়েছে।"
            )
            else -> StepExecutionReport(
                step = step,
                success = true,
                message = step.detail.ifBlank { "AI উত্তর প্রস্তুত করা হয়েছে।" }
            )
        }
    }

    fun openYouTubeQuery(query: String): StepExecutionReport {
        return openYouTube(TaskPlanStep("OPEN_YOUTUBE", query, "YouTube খোলা হচ্ছে"))
    }

    fun openWhatsAppDraft(message: String): StepExecutionReport {
        return openWhatsApp(TaskPlanStep("OPEN_WHATSAPP", message, "WhatsApp খোলা হচ্ছে"))
    }

    fun openChromeUrl(urlOrQuery: String): StepExecutionReport {
        return openChrome(TaskPlanStep("OPEN_CHROME", urlOrQuery, "Chrome খোলা হচ্ছে"))
    }

    fun openSettingsSection(section: String): StepExecutionReport {
        return openSystemSetting(TaskPlanStep("OPEN_SETTINGS", section, "Settings খোলা হচ্ছে"))
    }

    fun launchGameApp(packageName: String, displayName: String): StepExecutionReport {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            StepExecutionReport(
                TaskPlanStep("LAUNCH_GAME", packageName, displayName),
                true,
                "$displayName চালু করা হয়েছে। গেম চলাকালীন Floating AI Coach ব্যবহার করুন।"
            )
        } else {
            StepExecutionReport(
                TaskPlanStep("LAUNCH_GAME", packageName, displayName),
                false,
                "এই ডিভাইসে $displayName ইনস্টল করা নেই। আপনি যেকোনো গেম স্ক্রিনশট আপলোড করেও কোচিং নিতে পারবেন।"
            )
        }
    }

    private fun openYouTube(step: TaskPlanStep): StepExecutionReport {
        val query = step.target.trim()
        return try {
            val intent = if (query.isNotBlank()) {
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")
                )
            } else {
                context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                    ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com"))
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            StepExecutionReport(
                step,
                true,
                if (query.isNotBlank()) "YouTube-এ '$query' সার্চ খোলা হয়েছে।" else "YouTube চালু করা হয়েছে।"
            )
        } catch (e: Exception) {
            StepExecutionReport(step, false, "YouTube খোলা যায়নি: ${e.localizedMessage ?: "App not found"}")
        }
    }

    private fun openWhatsApp(step: TaskPlanStep): StepExecutionReport {
        val msg = step.target.trim()
        return try {
            val intent = if (msg.isNotBlank()) {
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, msg)
                    setPackage("com.whatsapp")
                }
            } else {
                context.packageManager.getLaunchIntentForPackage("com.whatsapp")
                    ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/"))
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            StepExecutionReport(
                step,
                true,
                if (msg.isNotBlank()) "WhatsApp-এ মেসেজ ড্রাফট প্রস্তুত করা হয়েছে — সেন্ড করার আগে প্রাপক বেছে নিন।" else "WhatsApp চালু করা হয়েছে।"
            )
        } catch (e: Exception) {
            // Fallback to web WhatsApp link when app isn't installed
            try {
                val fallback = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(msg)}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(fallback)
                StepExecutionReport(step, true, "WhatsApp ইনস্টল না থাকায় ওয়েব লিংকে মেসেজ ড্রাফট খোলা হয়েছে।")
            } catch (ex: Exception) {
                StepExecutionReport(step, false, "WhatsApp খোলা সম্ভব হয়নি: অ্যাপটি এই ডিভাইসে ইনস্টল নেই।")
            }
        }
    }

    private fun openChrome(step: TaskPlanStep): StepExecutionReport {
        val raw = step.target.trim()
        val url = when {
            raw.isBlank() -> "https://www.google.com"
            raw.startsWith("http://") || raw.startsWith("https://") -> raw
            raw.contains(".") && !raw.contains(" ") -> "https://$raw"
            else -> "https://www.google.com/search?q=${Uri.encode(raw)}"
        }
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            StepExecutionReport(step, true, "ব্রাউজারে '$url' খোলা হয়েছে।")
        } catch (e: Exception) {
            StepExecutionReport(step, false, "ওয়েব ব্রাউজার খোলা যায়নি: ${e.localizedMessage ?: "No browser"}")
        }
    }

    private fun openSystemSetting(step: TaskPlanStep): StepExecutionReport {
        val key = step.target.lowercase()
        val action = when {
            key.contains("wifi") || key.contains("wi-fi") -> Settings.ACTION_WIFI_SETTINGS
            key.contains("bluetooth") -> Settings.ACTION_BLUETOOTH_SETTINGS
            key.contains("display") || key.contains("brightness") -> Settings.ACTION_DISPLAY_SETTINGS
            key.contains("sound") || key.contains("volume") -> Settings.ACTION_SOUND_SETTINGS
            key.contains("access") -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            key.contains("battery") -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }
        return try {
            val intent = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            StepExecutionReport(step, true, "Android Settings (${step.target.ifBlank { "General" }}) খোলা হয়েছে।")
        } catch (e: Exception) {
            StepExecutionReport(step, false, "সেটিংস খোলা যায়নি: ${e.localizedMessage ?: "Unavailable"}")
        }
    }

    private fun runAccessibility(
        step: TaskPlanStep,
        block: (NexoraAccessibilityService) -> AccessibilityActionResult
    ): StepExecutionReport {
        val service = NexoraAccessibilityService.instance
            ?: return StepExecutionReport(
                step = step,
                success = false,
                message = "কাজটি করতে পারিনি কারণ NEXORA Accessibility Service চালু নেই। Settings > Permission Center থেকে এটি চালু করুন।"
            )
        val result = block(service)
        return StepExecutionReport(step, result.success, result.messageBengali)
    }
}
