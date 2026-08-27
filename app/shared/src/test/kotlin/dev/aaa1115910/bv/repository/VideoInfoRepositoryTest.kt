package dev.aaa1115910.bv.repository

import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.player.entity.PlayMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

    @Test
    fun `unknown season episode aid keeps the outer list cursor`() {
        val repository = VideoInfoRepository()
        repository.setPreloadedVideoList(listOf(video(10), video(20), video(30)), currentAvid = 20)

        assertEquals(1, repository.resolveLastPreloadedVideoIndex(avid = 200_001))
        assertEquals(2, repository.resolveLastPreloadedVideoIndex(avid = 30))
        assertEquals(2, repository.resolveLastPreloadedVideoIndex(avid = 300_001))
    }

    @Test
    fun `watch later list requests outer list playback`() {
        val repository = VideoInfoRepository()

        repository.setPreloadedVideoList(
            items = listOf(video(10), video(20)),
            currentAvid = 10,
            preferListPlayback = true,
        )

        assertTrue(repository.preferPreloadedVideoListPlayback)
        assertEquals(
            PlayMode.ListOrder,
            repository.resolvePlayModeForPreloadedList(PlayMode.PartAndEpisode),
        )
        assertEquals(
            PlayMode.ListOrderReverse,
            repository.resolvePlayModeForPreloadedList(PlayMode.PartAndEpisodeReverse),
        )
        assertEquals(
            PlayMode.Custom,
            repository.resolvePlayModeForPreloadedList(PlayMode.Custom),
        )
        repository.finishPreloadedVideoListPlayback()
        assertFalse(repository.preferPreloadedVideoListPlayback)
        assertEquals(
            PlayMode.PartAndEpisode,
            repository.resolvePlayModeForPreloadedList(PlayMode.PartAndEpisode),
        )
    }

    @Test
    fun `ordinary list replaces watch later playback request`() {
        val repository = VideoInfoRepository()
        repository.setPreloadedVideoList(
            items = listOf(video(10), video(20)),
            currentAvid = 10,
            preferListPlayback = true,
        )

        repository.setPreloadedVideoList(listOf(video(30), video(40)), currentAvid = 30)

        assertFalse(repository.preferPreloadedVideoListPlayback)
    }

    private fun video(avid: Long) = VideoCardData(
        avid = avid,
        title = "video-$avid",
        cover = "",
        upName = "up"
    )
}
