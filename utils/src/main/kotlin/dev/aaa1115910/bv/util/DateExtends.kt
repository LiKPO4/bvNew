package dev.aaa1115910.bv.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Date.formatPubTimeString(): String {
    val temp = System.currentTimeMillis() - time
    return when {
        temp > 1000L * 60 * 60 * 24 -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(this)
        temp > 1000L * 60 * 60 -> "${temp / (1000 * 60 * 60)}小时前"
        temp > 1000L * 60 -> "${temp / (1000 * 60)}分钟前"
        else -> "刚刚"
    }
}
