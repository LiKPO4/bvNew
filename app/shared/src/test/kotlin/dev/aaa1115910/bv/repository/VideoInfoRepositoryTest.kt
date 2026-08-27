package dev.aaa1115910.bv.repository

import dev.aaa1115910.bv.entity.carddata.VideoCardData
import kotlin.test.Test
import kotlin.test.assertEquals

class VideoInfoRepositoryTest {
    @Test
    fun `set preloaded list anchors cursor to selected video`() {
        val repository = VideoInfoRepository()
        val videos = listOf(video(1), video(2), video(3))

        repository.setPreloadedVideoList(videos, currentAvid = 2)

        assertEquals(1, repository.resolveLastPreloadedVideoIndex(avid = 999))
    }

    @Test
    fun `replacing preloaded list does not retain cursor from previous list`() {
        val repository = VideoInfoRepository()
        repository.setPreloadedVideoList(listOf(video(1), video(2), video(3)), currentAvid = 3)

        repository.setPreloadedVideoList(listOf(video(10), video(11)), currentAvid = 10)

        assertEquals(0, repository.resolveLastPreloadedVideoIndex(avid = 999))
    }

    private fun video(avid: Long) = VideoCardData(
        avid = avid,
        title = "video-$avid",
        cover = "",
        upName = "up"
    )
}
