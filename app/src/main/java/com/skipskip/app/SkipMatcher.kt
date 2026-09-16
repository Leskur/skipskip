package com.skipskip.app

/**
 * 判断一段控件文案是否是「跳过广告」按钮。
 *
 * 思路：先把倒计时、分隔符、空白这些装饰去掉，再和固定词表做**精确**比较，
 * 而不是子串包含。这样「跳过 5s」「5 | 跳过」能命中，
 * 「跳过本章」「跳过片头」「不跳过」不会。
 */
object SkipMatcher {

    /** 交给系统 findAccessibilityNodeInfosByText 做粗筛的关键字 */
    val SEARCH_KEYS = listOf("跳过", "关闭广告")

    /** 去掉装饰后必须完全等于其中之一 */
    private val EXACT = setOf(
        "跳过",
        "跳过广告",
        "关闭广告",
    )

    /** 数字、空白、以及倒计时常见的单位和分隔符 */
    private val NOISE = Regex("""[\d\s]|[sS秒]|[|丨·:：()（）>»→]""")

    fun matches(text: CharSequence?): Boolean {
        if (text.isNullOrBlank()) return false
        val normalized = NOISE.replace(text, "")
        return normalized in EXACT
    }
}
