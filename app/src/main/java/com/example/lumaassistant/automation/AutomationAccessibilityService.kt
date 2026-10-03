package com.example.lumaassistant.automation

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Powers voice-driven screen automation: "open WhatsApp", "type hello",
 * "tap send", "scroll down". Every action here is triggered from
 * AssistantViewModel in direct response to something the user said or
 * typed in this session - this service never decides on its own to open
 * an app, type text, or tap anything.
 *
 * It also tracks whether an incoming-call screen is currently showing, so
 * the app can announce the caller's name (read-only) and optionally tap
 * Answer/Decline if you say "answer" or "decline" - again, only after you
 * speak the command, never automatically.
 */
class AutomationAccessibilityService : AccessibilityService() {

    companion object {
        var instance: AutomationAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intentionally does nothing automatically on events; this service
        // is polled/queried on-demand (see below) rather than reacting to
        // every screen change, so it can't silently act in the background.
    }

    override fun onInterrupt() {}

    /** Opens an app by its visible label, using the launcher intent. Returns true if found. */
    fun openApp(appLabel: String): Boolean {
        val pm = packageManager
        val apps = pm.getInstalledApplications(0)
        val match = apps.firstOrNull {
            pm.getApplicationLabel(it).toString().equals(appLabel, ignoreCase = true)
        } ?: return false
        val launchIntent = pm.getLaunchIntentForPackage(match.packageName) ?: return false
        launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(launchIntent)
        return true
    }

    /** Types text into whatever editable field is currently focused on screen. */
    fun typeIntoFocusedField(text: String): Boolean {
        val node = findFocusedEditable(rootInActiveWindow) ?: return false
        val arguments = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                text
            )
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    /** Taps the first on-screen element whose visible text/description matches. */
    fun tapByText(label: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val target = findNodeByText(root, label) ?: return false
        val rect = Rect()
        target.getBoundsInScreen(rect)
        return target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    /** Scrolls the first scrollable container up or down. */
    fun scroll(down: Boolean): Boolean {
        val root = rootInActiveWindow ?: return false
        val scrollable = findScrollable(root) ?: return false
        val action = if (down) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        return scrollable.performAction(action)
    }

    /** True if the current foreground screen looks like an incoming-call UI. */
    fun isIncomingCallScreenVisible(): Boolean {
        val root = rootInActiveWindow ?: return false
        return findNodeByText(root, "Answer") != null || findNodeByText(root, "Decline") != null
    }

    fun answerCall(): Boolean = tapByText("Answer")
    fun declineCall(): Boolean = tapByText("Decline") || tapByText("Reject")

    // --- node-tree helpers ---

    private fun findFocusedEditable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isEditable && node.isFocused) return node
        for (i in 0 until node.childCount) {
            val result = findFocusedEditable(node.getChild(i))
            if (result != null) return result
        }
        return null
    }

    private fun findNodeByText(node: AccessibilityNodeInfo?, text: String): AccessibilityNodeInfo? {
        if (node == null) return null
        val nodeText = node.text?.toString() ?: node.contentDescription?.toString()
        if (nodeText != null && nodeText.contains(text, ignoreCase = true)) return node
        for (i in 0 until node.childCount) {
            val result = findNodeByText(node.getChild(i), text)
            if (result != null) return result
        }
        return null
    }

    private fun findScrollable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val result = findScrollable(node.getChild(i))
            if (result != null) return result
        }
        return null
    }
}
