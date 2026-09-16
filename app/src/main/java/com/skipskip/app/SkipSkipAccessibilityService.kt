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
 *
 * 窗口切换后开启约 5 秒的「开屏观察期」，仅在此期间响应内容变化，
 * 避免日常刷列表时反复扫节点。
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
        splashWatchPackage = null
        splashWatchAt = 0L
        Log.i(TAG, "service unbound")
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (!SkipPrefs.isAutoClickEnabled(this)) return

        val eventType = event.eventType
        when (eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> Unit
            else -> return
        }

        val now = System.currentTimeMillis()
        val eventPkg = event.packageName?.toString()

        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            // 覆盖上一次观察期；连续切 App 只会留下最后一次
            if (eventPkg != null) {
                splashWatchPackage = eventPkg
                splashWatchAt = now
            }
        } else {
            // 内容变化：只在观察期内、且仍是刚切到前台的那个包
            val watchPkg = splashWatchPackage
            if (watchPkg == null ||
                now - splashWatchAt > SPLASH_WATCH_MS ||
                (eventPkg != null && eventPkg != watchPkg)
            ) {
                return
            }
        }

        if (now - lastClickAt < CLICK_COOLDOWN_MS) return

        val root = rootInActiveWindow ?: return

        // 应用范围：自己和系统 UI 永不处理；用户排除的也跳过
        val pkg = root.packageName?.toString() ?: eventPkg
        if (pkg == null || pkg == packageName || pkg in ALWAYS_EXCLUDED) return
        if (pkg in SkipPrefs.excludedPackages(this)) return

        // 内容变化时再核对一次：前台根节点仍须是观察中的包
        if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            if (pkg != splashWatchPackage || now - splashWatchAt > SPLASH_WATCH_MS) return
        }

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

    /**
     * 两步找目标：
     * 1. 让系统按关键字粗筛（比自己遍历整棵树快得多）
     * 2. 用 SkipMatcher 精确判断文案，再向上找到真正可点击的祖先
     */
    private fun findSkipNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        for (key in SkipMatcher.SEARCH_KEYS) {
            val candidates = root.findAccessibilityNodeInfosByText(key) ?: continue
            for (node in candidates) {
                val hit = SkipMatcher.matches(node.text) ||
                    SkipMatcher.matches(node.contentDescription)
                if (!hit) continue
                findClickableAncestor(node)?.let { return it }
            }
        }
        return null
    }

    private fun findClickableAncestor(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) return current
            current = current.parent
        }
        return null
    }

    companion object {
        private const val TAG = "SkipSkipService"
        private const val CLICK_COOLDOWN_MS = 1500L
        /** 切到前台后，允许 CONTENT_CHANGED 继续找跳过的时长 */
        private const val SPLASH_WATCH_MS = 5000L

        private val ALWAYS_EXCLUDED = setOf(
            "com.android.systemui",
            "com.android.settings",
        )

        @Volatile
        private var lastClickAt: Long = 0L

        /** 最近一次窗口切换对应的包名；内容变化只服务这个包 */
        @Volatile
        private var splashWatchPackage: String? = null

        @Volatile
        private var splashWatchAt: Long = 0L

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
