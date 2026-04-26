package dev.aaa1115910.bv.repository

import dev.aaa1115910.biliapi.entity.video.Tag
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.player.entity.VideoListItem
import org.koin.core.annotation.Single

@Single
class VideoInfoRepository {
    val videoList = mutableListOf<VideoListItem>()
    val relatedVideos = mutableListOf<VideoCardData>()
    val preloadedVideoList = mutableListOf<VideoCardData>()
    var description: String = ""
    var tags: List<Tag> = emptyList()
    var lastPreloadedVideoIndex = 0

    fun resolveLastPreloadedVideoIndex(avid: Long): Int {
        val currentIndex = preloadedVideoList.indexOfFirst { it.avid == avid }
        if (currentIndex >= 0) {
            lastPreloadedVideoIndex = currentIndex
        }
        return lastPreloadedVideoIndex
    }
}
