package dev.aaa1115910.biliapi.entity.ugc

import dev.aaa1115910.biliapi.http.entity.home.RcmdIndexData
import dev.aaa1115910.biliapi.http.entity.home.RcmdTopData
import dev.aaa1115910.biliapi.util.convertStringTimeToSeconds
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

data class UgcItem(
    val aid: Long,
    val bvid: String = "",
    val title: String,
    val cover: String,
    val author: String,
    val authorId: Long = 0,
    val authorFace: String = "",
    val play: Long,
    val danmaku: Int,
    val duration: Int,
    val idx: Int = -1,
    val isInteractive: Boolean = false,
    val pubTime: String? = null,
) {
    companion object {
        fun fromRcmdItem(rcmdItem: RcmdIndexData.RcmdItem) =
            UgcItem(
                aid = rcmdItem.args.aid ?: 0,
                title = rcmdItem.title!!,
                cover = rcmdItem.cover!!,
                author = rcmdItem.args.upName ?: "",
                authorId = rcmdItem.args.upId ?: 0,
//                authorFace = rcmdItem.args.upFace ?: "",
                play = with(rcmdItem.coverLeftText1) {
                    runCatching {
                        if (this!!.endsWith("万")) {
                            (this.substring(0, this.length - 1).toDouble() * 10000).toLong()
                        } else {
                            this.toLong()
                        }
                    }.getOrDefault(-1)
                },
                danmaku = with(rcmdItem.coverLeftText2) {
                    if (this == null) return@with -1
                    runCatching {
                        if (this.endsWith("万")) {
                            (this.substring(0, this.length - 1).toDouble() * 10000).toInt()
                        } else {
                            this.toInt()
                        }
                    }.getOrDefault(-1)
                },
                duration = rcmdItem.coverRightText?.convertStringTimeToSeconds() ?: 0,
                idx = rcmdItem.idx
            )

        fun fromRcmdItem(rcmdItem: RcmdTopData.RcmdItem) =
            UgcItem(
                aid = rcmdItem.id,
                bvid = rcmdItem.bvid,
                title = rcmdItem.title,
                cover = rcmdItem.pic,
                author = rcmdItem.owner?.name ?: "",
                authorId = rcmdItem.owner?.mid ?: 0,
                authorFace = rcmdItem.owner?.face ?: "",
                play = rcmdItem.stat?.view ?: -1L,
                danmaku = rcmdItem.stat?.danmaku ?: -1,
                duration = rcmdItem.duration,
                isInteractive = rcmdItem.avFeature.resolveInteractiveFlag(),
                pubTime = rcmdItem.pubdate.smartDate
            )

        fun fromVideoInfo(videoInfo: dev.aaa1115910.biliapi.http.entity.video.VideoInfo) =
            UgcItem(
                aid = videoInfo.aid,
                title = videoInfo.title,
                duration = videoInfo.duration,
                author = videoInfo.owner.name,
                authorId = videoInfo.owner.mid,
                authorFace = videoInfo.owner.face,
                cover = videoInfo.pic,
                play = videoInfo.stat.view,
                danmaku = videoInfo.stat.danmaku,
                isInteractive = videoInfo.rights.isSteinGate == 1,
                pubTime = videoInfo.pubdate.smartDate
            )

        fun fromSmallCoverV5(card: bilibili.app.card.v1.SmallCoverV5): UgcItem {
            // 格式："n.n万观看 · n天前"
            val playAndPubTime = card.rightDesc2.split(" · ")
            val play = playAndPubTime.getOrNull(0)?.let { convertPlayStringToLong(it) } ?: -1
            val pubTime = playAndPubTime.getOrNull(1)

            return UgcItem(
                aid = card.base.param.toLong(),
                title = card.base.title,
                duration = convertStringTimeToSeconds(card.coverRightText1),
                author = card.rightDesc1,
//                authorId = card.base.upId,
//                authorFace = card.base.upFace,
                cover = card.base.cover,
                play = play,
                pubTime = pubTime,
                danmaku = -1,
                idx = card.base.idx.toInt()
            )
        }

        fun fromRegionDynamicListItem(item: dev.aaa1115910.biliapi.http.entity.region.RegionDynamicList.Item) =
            UgcItem(
                aid = item.param.toLong(),
                title = item.title,
                duration = item.duration,
                author = item.name,
//                authorId = item.mid,
                authorFace = item.face,
                cover = item.cover,
                play = item.play ?: -1,
                danmaku = item.danmaku ?: -1,
                pubTime = item.pubDate.smartDate
            )

        fun fromRegionRcmdArchive(archive: dev.aaa1115910.biliapi.http.entity.region.RegionFeedRcmd.Archive) =
            UgcItem(
                aid = archive.aid,
                title = archive.title,
                duration = archive.duration,
                author = archive.author.name,
                authorId = archive.author.mid,
                cover = archive.cover,
                play = archive.stat.view,
                danmaku = archive.stat.danmaku,
                pubTime = archive.pubdate.toSmartDate()
            )
    }
}

private fun convertPlayStringToLong(text: String): Long {
    if (text.isBlank()) return -1

    val value = text.replace("观看", "").trim()

    return try {
        when {
            value.endsWith("万") -> {
                val num = value.removeSuffix("万").toDouble()
                (num * 10_000).toLong()
            }

            value.endsWith("亿") -> {
                val num = value.removeSuffix("亿").toDouble()
                (num * 100_000_000).toLong()
            }

            else -> {
                value.toLong()
            }
        }
    } catch (e: Exception) {
        -1
    }
}
private fun convertStringTimeToSeconds(time: String): Int {
    val parts = time.split(":")
    val hours = if (parts.size == 3) parts[0].toInt() else 0
    val minutes = parts[parts.size - 2].toInt()
    val seconds = parts[parts.size - 1].toInt()
    return (hours * 3600) + (minutes * 60) + seconds
}

/**
 * 智能日期格式化 (兼容低版本 Android)
 * @param timeZone 时区 (默认系统时区)
 */
fun Long.toSmartDate(timeZone: TimeZone = TimeZone.getDefault()): String? {
    if (this <= 0) return null
    try {
        // 自动识别秒级或毫秒级时间戳
        // 秒级时间戳通常小于等于10位数，目前直到2286年都是10位数
        // 毫秒级时间戳通常为13位数
        val timeInMillis = if (this < 10000000000L) this * 1000L else this

        // 创建日历实例
        val cal = Calendar.getInstance(timeZone).apply {
            this.timeInMillis = timeInMillis
        }

        // 获取当前年份
        val currentYear = Calendar.getInstance(timeZone).get(Calendar.YEAR)

        // 动态格式选择
        val pattern = if (cal.get(Calendar.YEAR) == currentYear) {
            "M-d H:mm"
        } else {
            "yyyy-M-d"
        }

        // 线程安全的日期格式化
        return SimpleDateFormat(pattern, Locale.CHINESE).apply {
            this.timeZone = timeZone
        }.format(cal.time)
    } catch (e: Exception) {
        return null
    }
}

/**
 * 智能日期格式化 (兼容低版本 Android)
 * @param timeZone 时区 (默认系统时区)
 */
fun Long.toSmartDateTime(timeZone: TimeZone = TimeZone.getDefault()): String? {
    if (this <= 0) return null

    try {
        // 自动识别秒级或毫秒级时间戳
        // 秒级时间戳通常小于等于10位数，目前直到2286年都是10位数
        // 毫秒级时间戳通常为13位数
        val timeInMillis = if (this < 10000000000L) this * 1000L else this
        val temp = System.currentTimeMillis() - timeInMillis
        return when {
            temp > 1000L * 60 * 60 * 24 -> SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINESE).apply {
                this.timeZone = timeZone
            }.format(
                timeInMillis
            )

            temp > 1000L * 60 * 60 -> "${temp / (1000 * 60 * 60)}小时前"
            temp > 1000L * 60 -> "${temp / (1000 * 60)}分钟前"
            else -> "刚刚"
        }
    } catch (e: Exception) {
        return null
    }
}

val Int.smartDate: String?
    get() = this.toLong().toSmartDate()

private fun JsonElement?.resolveInteractiveFlag(): Boolean {
    return when (this) {
        is JsonObject -> this.any { (key, value) ->
            if (key in interactiveFlagKeys) {
                value.isTruthy()
            } else {
                value.resolveInteractiveFlag()
            }
        }

        is JsonArray -> this.any { it.resolveInteractiveFlag() }
        else -> false
    }
}

private fun JsonElement.isTruthy(): Boolean {
    return when (this) {
        is JsonPrimitive -> booleanOrNull == true || intOrNull == 1 || contentOrNull == "1"
        else -> false
    }
}

private val interactiveFlagKeys = setOf(
    "is_story",
    "is_steins",
    "is_steins_gate",
    "isStory",
    "isSteins",
    "isSteinGate"
)