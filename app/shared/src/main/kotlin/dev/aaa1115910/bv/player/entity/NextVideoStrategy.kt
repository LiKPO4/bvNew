package dev.aaa1115910.bv.player.entity

import android.content.Context

enum class NextVideoStrategy(val ordinalValue: Int) {
    PartAndEpisode(0),
    PreloadedVideoList(1),
    RelatedVideo(2);

    fun displayName(context: Context): String = when (this) {
        PartAndEpisode -> "合集/分P"
        PreloadedVideoList -> "视频列表（UGC）"
        RelatedVideo -> "推荐视频（UGC）"
    }

    companion object {
        fun fromOrdinal(ordinal: Int): NextVideoStrategy = entries.find { it.ordinalValue == ordinal } ?: PartAndEpisode
    }
}

data class NextVideoStrategyConfig(
    val strategy: NextVideoStrategy,
    val hidden: Boolean,
    val ordinal: Int
)
