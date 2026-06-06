package dev.aaa1115910.bv.player.entity

import android.content.Context

enum class NextVideoStrategy(val ordinalValue: Int) {
    SingleVideo(1),
    PartAndEpisode(2),
    PartAndEpisodeReverse(3),
    PreloadedVideoList(4),
    PreloadedVideoListReverse(5),
    RelatedVideo(6);

    fun displayName(context: Context): String = when (this) {
        SingleVideo -> "单视频"
        PartAndEpisode -> "合集/分P(顺)"
        PartAndEpisodeReverse -> "合集/分P(逆)"
        PreloadedVideoList -> "列表视频(顺)"
        PreloadedVideoListReverse -> "列表视频(逆)"
        RelatedVideo -> "UGC推荐-随机"
    }

    companion object {
        fun fromOrdinal(ordinal: Int): NextVideoStrategy = entries.find { it.ordinalValue == ordinal } ?: SingleVideo
    }
}

data class NextVideoStrategyConfig(
    val strategy: NextVideoStrategy,
    val hidden: Boolean,
    val ordinal: Int
)
