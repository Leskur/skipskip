package com.skipskip.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo

/**
 * 学习向实现：界面变化时查找含「跳过」等文案的可点击节点并点击。
 */
class SkipSkipAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        isConnected = true
        Log.i(TAG, "service connected")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        isConnected = false
        instance = null
        Log.i(TAG, "service unbound")
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (!SkipPrefs.isAutoClickEnabled(this)) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> Unit
            else -> return
        }

        val now = System.currentTimeMillis()
        if (now - lastClickAt < CLICK_COOLDOWN_MS) return

        val root = rootInActiveWindow ?: return
        val target = findSkipNode(root) ?: return
        if (target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            lastClickAt = now
            SkipPrefs.incrementClickCount(this)
            Log.i(TAG, "clicked skip: text=${target.text} desc=${target.contentDescription}")
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "service interrupted")
    }

    private fun findSkipNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val label = buildString {
            append(node.text ?: "")
            append(' ')
            append(node.contentDescription ?: "")
        }

        if (SKIP_KEYWORDS.any { label.contains(it) }) {
            var clickableNode: AccessibilityNodeInfo? = node
            while (clickableNode != null) {
                if (clickableNode.isClickable) return clickableNode
                clickableNode = clickableNode.parent
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findSkipNode(child)?.let { return it }
        }
        return null
    }

    companion object {
        private const val TAG = "SkipSkipService"
        private const val CLICK_COOLDOWN_MS = 1500L

        private val SKIP_KEYWORDS = listOf(
            "跳过",
            "跳过广告",
            "关闭广告",
        )

        @Volatile
        private var lastClickAt: Long = 0L

        @Volatile
        var isConnected: Boolean = false
            private set

        @Volatile
        private var instance: SkipSkipAccessibilityService? = null

        /** 打开系统最近任务页；仅在服务已连接时有效。返回是否成功发出。 */
        fun openRecents(): Boolean =
            instance?.performGlobalAction(GLOBAL_ACTION_RECENTS) == true

        fun isEnabled(context: Context): Boolean {
            if (isConnected) return true

            val am = context.getSystemService(AccessibilityManager::class.java) ?: return false
            val enabled = am.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK,
            )
            val myClass = SkipSkipAccessibilityService::class.java.name
            return enabled.any { info ->
                val service = info.resolveInfo?.serviceInfo ?: return@any false
                service.packageName == context.packageName && service.name == myClass
            }
        }
    }
}
