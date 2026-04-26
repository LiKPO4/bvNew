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
        PartAndEpisode -> "合集/分P"
        PartAndEpisodeReverse -> "合集/分P-逆序"
        PreloadedVideoList -> "UGC列表"
        PreloadedVideoListReverse -> "UGC列表-逆序"
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
