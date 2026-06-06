package dev.aaa1115910.bv.player.entity

import android.content.Context
import dev.aaa1115910.bv.player.shared.R

enum class PlayMode(private val strRes: Int) {
    //单视频（播完即停）
    SingleVideo(R.string.play_mode_single_video),

    //单视频循环
    SingleLoop(R.string.play_mode_single_loop),

    //合集/分P
    PartAndEpisode(R.string.play_mode_part_episode),

    //合集/分P-逆序
    PartAndEpisodeReverse(R.string.play_mode_part_episode_reverse),

    //UGC列表视频顺序播放
    ListOrder(R.string.play_mode_list_order),

    //UGC列表视频逆序播放
    ListOrderReverse(R.string.play_mode_list_order_reverse),

    //推荐视频
    RelatedVideo(R.string.play_mode_related_video),

    //自定义（按设置中的策略顺序）
    Custom(R.string.play_mode_custom);

    fun getDisplayName(context: Context) = context.getString(strRes)
}