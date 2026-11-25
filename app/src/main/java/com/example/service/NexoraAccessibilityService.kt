package com.example.service

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AccessibilityScreenState(
    val isServiceConnected: Boolean = false,
    val foregroundPackage: String = "",
    val visibleElements: List<String> = emptyList(),
    val screenTextDump: String = "",
    val isSensitiveAppActive: Boolean = false,
    val lastUpdatedMs: Long = 0L
)

data class AccessibilityActionResult(
    val success: Boolean,
    val messageBengali: String
)

class NexoraAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _screenState.value = _screenState.value.copy(
            isServiceConnected = true,
            lastUpdatedMs = System.currentTimeMillis()
        )
        refreshScreenSnapshot()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString().orEmpty()
        if (pkg.isNotBlank() && pkg != packageName) {
            refreshScreenSnapshot(pkg)
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance === this) {
            instance = null
        }
        _screenState.value = _screenState.value.copy(isServiceConnected = false)
        super.onDestroy()
    }

    fun refreshScreenSnapshot(overridePackage: String? = null): AccessibilityScreenState {
        val root = rootInActiveWindow
        val pkg = overridePackage ?: root?.packageName?.toString().orEmpty()
        val elements = mutableListOf<String>()
        if (root != null) {
            collectVisibleText(root, elements, maxItems = 60)
        }
        val sensitive = isSensitivePackage(pkg)
        val snapshot = AccessibilityScreenState(
            isServiceConnected = true,
            foregroundPackage = pkg,
            visibleElements = elements,
            screenTextDump = elements.joinToString(" | "),
            isSensitiveAppActive = sensitive,
            lastUpdatedMs = System.currentTimeMillis()
        )
        _screenState.value = snapshot
        return snapshot
    }

    private fun collectVisibleText(
        node: AccessibilityNodeInfo,
        out: MutableList<String>,
        maxItems: Int
    ) {
        if (out.size >= maxItems) return
        val label = (node.text?.toString() ?: node.contentDescription?.toString()).orEmpty().trim()
        if (label.isNotBlank() && label.length <= 140) {
            val tag = if (node.isClickable) "[Btn] $label" else label
            if (!out.contains(tag)) {
                out.add(tag)
            }
        }
        for (i in 0 until node.childCount) {
            if (out.size >= maxItems) break
            val child = node.getChild(i) ?: continue
            collectVisibleText(child, out, maxItems)
        }
    }

    fun executeScrollForward(): AccessibilityActionResult {
        val root = rootInActiveWindow ?: return AccessibilityActionResult(
            false,
            "সক্রিয় উইন্ডো পাওয়া যায়নি। প্রথমে লক্ষ্য অ্যাপটি খুলুন।"
        )
        val scrollable = findFirstScrollable(root)
        val ok = scrollable?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) == true
        return if (ok) {
            AccessibilityActionResult(true, "স্ক্রিন নিচে স্ক্রল করা হয়েছে।")
        } else {
            AccessibilityActionResult(false, "বর্তমান স্ক্রিনে স্ক্রলযোগ্য অংশ খুঁজে পাওয়া যায়নি।")
        }
    }

    fun executeScrollBackward(): AccessibilityActionResult {
        val root = rootInActiveWindow ?: return AccessibilityActionResult(
            false,
            "সক্রিয় উইন্ডো পাওয়া যায়নি।"
        )
        val scrollable = findFirstScrollable(root)
        val ok = scrollable?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) == true
        return if (ok) {
            AccessibilityActionResult(true, "স্ক্রিন উপরে স্ক্রল করা হয়েছে।")
        } else {
            AccessibilityActionResult(false, "উপরে স্ক্রল করা সম্ভব হয়নি।")
        }
    }

    fun executeClickByText(query: String): AccessibilityActionResult {
        if (query.isBlank()) {
            return AccessibilityActionResult(false, "কোন বাটনে ক্লিক করতে হবে তা উল্লেখ করা হয়নি।")
        }
        val root = rootInActiveWindow ?: return AccessibilityActionResult(
            false,
            "সক্রিয় অ্যাপ স্ক্রিন পড়া যায়নি।"
        )
        val matches = root.findAccessibilityNodeInfosByText(query)
        val target = matches?.firstOrNull()
        if (target == null) {
            return AccessibilityActionResult(false, "'$query' লেখা কোনো UI বাটন স্ক্রিনে পাওয়া যায়নি।")
        }
        var clickableNode: AccessibilityNodeInfo? = target
        while (clickableNode != null && !clickableNode.isClickable) {
            clickableNode = clickableNode.parent
        }
        val ok = clickableNode?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
        return if (ok) {
            AccessibilityActionResult(true, "'$query' এ সফলভাবে ট্যাপ করা হয়েছে।")
        } else {
            AccessibilityActionResult(false, "'$query' উপাদানটি ক্লিক করা সম্ভব হয়নি।")
        }
    }

    fun executeInputText(textToType: String): AccessibilityActionResult {
        if (textToType.isBlank()) {
            return AccessibilityActionResult(false, "টাইপ করার জন্য কোনো টেক্সট দেওয়া হয়নি।")
        }
        val root = rootInActiveWindow ?: return AccessibilityActionResult(
            false,
            "সক্রিয় ইনপুট ফিল্ড পাওয়া যায়নি।"
        )
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: findFirstEditable(root)
        if (focused == null) {
            return AccessibilityActionResult(false, "বর্তমান স্ক্রিনে কোনো টেক্সট ইনপুট বক্স ফোকাসড নেই।")
        }
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToType)
        }
        val ok = focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        return if (ok) {
            AccessibilityActionResult(true, "টেক্সট বক্সে '$textToType' লেখা হয়েছে।")
        } else {
            AccessibilityActionResult(false, "এই অ্যাপের টেক্সট ফিল্ডে অটো-টাইপ অনুমোদিত নয়।")
        }
    }

    fun executeGlobalBack(): AccessibilityActionResult {
        val ok = performGlobalAction(GLOBAL_ACTION_BACK)
        return if (ok) {
            AccessibilityActionResult(true, "পূর্ববর্তী স্ক্রিনে (Back) ফিরে যাওয়া হয়েছে।")
        } else {
            AccessibilityActionResult(false, "Back অ্যাকশন সম্পন্ন করা যায়নি।")
        }
    }

    fun executeGlobalHome(): AccessibilityActionResult {
        val ok = performGlobalAction(GLOBAL_ACTION_HOME)
        return if (ok) {
            AccessibilityActionResult(true, "ফোনের Home স্ক্রিনে যাওয়া হয়েছে।")
        } else {
            AccessibilityActionResult(false, "Home অ্যাকশন সম্পন্ন করা যায়নি।")
        }
    }

    private fun findFirstScrollable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findFirstScrollable(child)
            if (found != null) return found
        }
        return null
    }

    private fun findFirstEditable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findFirstEditable(child)
            if (found != null) return found
        }
        return null
    }

    companion object {
        @Volatile
        var instance: NexoraAccessibilityService? = null
            private set

        private val _screenState = MutableStateFlow(AccessibilityScreenState())
        val screenState: StateFlow<AccessibilityScreenState> = _screenState.asStateFlow()

        private val sensitiveKeywords = listOf(
            "bank", "bkash", "nagad", "rocket", "pay", "wallet", "upi",
            "password", "authenticator", "binance", "zerodha", "kite", "upstox", "broker"
        )

        fun isSensitivePackage(pkg: String): Boolean {
            val lower = pkg.lowercase()
            return sensitiveKeywords.any { lower.contains(it) }
        }
    }
}
