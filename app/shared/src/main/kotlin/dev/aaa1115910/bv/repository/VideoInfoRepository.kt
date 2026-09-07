package dev.aaa1115910.bv.repository

import dev.aaa1115910.biliapi.entity.live.LiveRoomItem
import dev.aaa1115910.biliapi.entity.video.Tag
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.player.entity.PlayMode
import dev.aaa1115910.bv.player.entity.VideoListItem
import org.koin.core.annotation.Single

data class InteractivePlaybackContext(
    val bvid: String,
    val graphVersion: Int,
)

@Single
class VideoInfoRepository {
    val videoList = mutableListOf<VideoListItem>()
    val relatedVideos = mutableListOf<VideoCardData>()
    var preloadedVideoList: List<VideoCardData> = emptyList()
        private set
    var description: String = ""
    var tags: List<Tag> = emptyList()
    var lastPreloadedVideoIndex = 0
        private set
    var preferPreloadedVideoListPlayback = false
        private set
    var interactivePlaybackContext: InteractivePlaybackContext? = null

    val preloadedLiveRoomList = mutableListOf<LiveRoomItem>()
    var lastPreloadedRoomIndex = 0

    // 视频列表入口默认沿来源列表连播，必须提供当前卡片以定位游标。
    fun setPreloadedVideoList(
        items: List<VideoCardData>,
        currentAvid: Long,
        preferListPlayback: Boolean = true,
    ) {
        preloadedVideoList = items.distinctBy { it.avid }
        preferPreloadedVideoListPlayback = preferListPlayback && preloadedVideoList.isNotEmpty()
        lastPreloadedVideoIndex = preloadedVideoList.indexOfFirst { it.avid == currentAvid }
            .takeIf { it >= 0 }
            ?: 0
    }

    fun clearPreloadedVideoList() {
        preloadedVideoList = emptyList()
        lastPreloadedVideoIndex = 0
        preferPreloadedVideoListPlayback = false
    }

    fun finishPreloadedVideoListPlayback() {
        preferPreloadedVideoListPlayback = false
    }

    fun resolvePlayModeForPreloadedList(currentPlayMode: PlayMode): PlayMode {
        if (!preferPreloadedVideoListPlayback || preloadedVideoList.isEmpty()) return currentPlayMode

        return when (currentPlayMode) {
            PlayMode.PartAndEpisode -> PlayMode.ListOrder
            PlayMode.PartAndEpisodeReverse -> PlayMode.ListOrderReverse
            else -> currentPlayMode
        }
    }

    fun resolveLastPreloadedVideoIndex(avid: Long): Int {
        val currentIndex = preloadedVideoList.indexOfFirst { it.avid == avid }
        if (currentIndex >= 0) {
            lastPreloadedVideoIndex = currentIndex
        }
        return lastPreloadedVideoIndex
    }

    fun updateInteractivePlaybackContext(bvid: String, graphVersion: Int?) {
        interactivePlaybackContext = if (bvid.isNotBlank() && graphVersion != null) {
            InteractivePlaybackContext(
                bvid = bvid,
                graphVersion = graphVersion,
            )
        } else {
            null
        }
    }

    fun clearInteractivePlaybackContext() {
        interactivePlaybackContext = null
    }
}
