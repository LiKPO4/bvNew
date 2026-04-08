package dev.aaa1115910.bv.player.entity

import android.content.Context
import dev.aaa1115910.bv.player.shared.R

enum class PlayMode(private val strRes: Int) {
    //默认（按设置中的策略顺序）
    Default(R.string.play_mode_default),

    //单视频循环
    SingleLoop(R.string.play_mode_single_loop),

    //合集/分P
    PartAndEpisode(R.string.play_mode_part_episode),

    //UGC视频列表顺序播放
    ListOrder(R.string.play_mode_list_order),

    //推荐视频
    RelatedVideo(R.string.play_mode_related_video);

    fun getDisplayName(context: Context) = context.getString(strRes)
}