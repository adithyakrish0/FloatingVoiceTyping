package com.adithyakrishnan.floatingvoicetyping

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.view.accessibility.AccessibilityServiceInfo
class TextInjectionService : AccessibilityService() {

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "INJECT_TEXT_ACTION") {
                val text = intent.getStringExtra("text") ?: return
                Log.d("TextInjection", "Received text to inject: $text")
                injectText(text)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        // Configure accessibility service
        val config = accessibilityServiceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_FOCUSED

            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS

            notificationTimeout = 100
        }
        this.serviceInfo = config

        // Register broadcast receiver
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(broadcastReceiver, IntentFilter("INJECT_TEXT_ACTION"))

        Log.d("TextInjection", "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Optional: You can track focus changes here if needed
        event?.let {
            Log.v("TextInjection", "Accessibility event: ${event.eventType}")
        }
    }

    override fun onInterrupt() {
        // Clean up when service is interrupted
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister receiver when service is destroyed
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
        Log.d("TextInjection", "Service destroyed")
    }

    fun injectText(text: String) {
        Log.d("TextInjection", "Attempting to inject text: $text")

        // Get root node and focused input field
        val rootNode = rootInActiveWindow ?: run {
            Log.e("TextInjection", "No root node available")
            return
        }

        val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: run {
            Log.e("TextInjection", "No focused input field found")
            return
        }

        // Try different methods to inject text
        if (trySetText(focusedNode, text)) return
        if (tryPasteText(focusedNode, text)) return

        Log.e("TextInjection", "All text injection methods failed")
    }

    private fun trySetText(node: AccessibilityNodeInfo, text: String): Boolean {
        return try {
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            Log.d("TextInjection", "Text set via ACTION_SET_TEXT")
            true
        } catch (e: Exception) {
            Log.e("TextInjection", "ACTION_SET_TEXT failed", e)
            false
        }
    }

    private fun tryPasteText(node: AccessibilityNodeInfo, text: String): Boolean {
        return try {
            // First set text to clipboard (simulated)
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }

            // Focus the field if not already focused
            if (!node.isFocused) {
                node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            }

            // Paste from clipboard
            node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
            Log.d("TextInjection", "Text pasted via ACTION_PASTE")
            true
        } catch (e: Exception) {
            Log.e("TextInjection", "ACTION_PASTE failed", e)
            false
        }
    }
}