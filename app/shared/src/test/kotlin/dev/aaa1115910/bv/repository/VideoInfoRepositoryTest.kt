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
    fun `explicit opt out replaces previous list playback request`() {
        val repository = VideoInfoRepository()
        repository.setPreloadedVideoList(
            items = listOf(video(10), video(20)),
            currentAvid = 10,
            preferListPlayback = true,
        )

        repository.setPreloadedVideoList(
            listOf(video(30), video(40)),
            currentAvid = 30,
            preferListPlayback = false,
        )

        assertFalse(repository.preferPreloadedVideoListPlayback)
    }

    @Test
    fun `video list entry enables source list playback by default`() {
        val repository = VideoInfoRepository()
        repository.setPreloadedVideoList(listOf(video(10), video(20), video(30)), currentAvid = 20)

        assertTrue(repository.preferPreloadedVideoListPlayback)
        assertEquals(1, repository.resolveLastPreloadedVideoIndex(avid = 200_001))
        assertEquals(
            PlayMode.ListOrder,
            repository.resolvePlayModeForPreloadedList(PlayMode.PartAndEpisode),
        )
        assertEquals(
            PlayMode.ListOrderReverse,
            repository.resolvePlayModeForPreloadedList(PlayMode.PartAndEpisodeReverse),
        )
    }

    @Test
    fun `empty list removes previous playback request and cursor`() {
        val repository = VideoInfoRepository()
        repository.setPreloadedVideoList(listOf(video(10), video(20)), currentAvid = 20)

        repository.setPreloadedVideoList(emptyList(), currentAvid = 20)

        assertFalse(repository.preferPreloadedVideoListPlayback)
        assertEquals(0, repository.resolveLastPreloadedVideoIndex(avid = 20))
        assertEquals(
            PlayMode.PartAndEpisode,
            repository.resolvePlayModeForPreloadedList(PlayMode.PartAndEpisode),
        )
    }

    @Test
    fun `clearing source list resets list cursor and playback request together`() {
        val repository = VideoInfoRepository()
        repository.setPreloadedVideoList(listOf(video(10), video(20)), currentAvid = 20)

        repository.clearPreloadedVideoList()

        assertTrue(repository.preloadedVideoList.isEmpty())
        assertFalse(repository.preferPreloadedVideoListPlayback)
        assertEquals(0, repository.resolveLastPreloadedVideoIndex(avid = 20))
        assertEquals(
            PlayMode.PartAndEpisodeReverse,
            repository.resolvePlayModeForPreloadedList(PlayMode.PartAndEpisodeReverse),
        )
    }

    @Test
    fun `source list snapshot survives replacement from itself and changes to original list`() {
        val repository = VideoInfoRepository()
        val items = mutableListOf(video(10), video(20), video(30))
        repository.setPreloadedVideoList(items, currentAvid = 20)
        items.clear()

        repository.setPreloadedVideoList(repository.preloadedVideoList, currentAvid = 30)

        assertEquals(listOf(10L, 20L, 30L), repository.preloadedVideoList.map { it.avid })
        assertEquals(2, repository.resolveLastPreloadedVideoIndex(avid = 999))
        assertTrue(repository.preferPreloadedVideoListPlayback)
    }

    @Test
    fun `feed playback keeps selected position after duplicate cards are removed`() {
        val repository = VideoInfoRepository()
        repository.setPreloadedVideoList(
            items = listOf(video(10), video(10), video(20), video(30)),
            currentAvid = 20,
            preferListPlayback = true,
        )

        val index = repository.resolveLastPreloadedVideoIndex(avid = 20)
        assertEquals(1, index)
        assertEquals(30L, repository.preloadedVideoList[index + 1].avid)
        assertEquals(10L, repository.preloadedVideoList[index - 1].avid)
        // 跨稿件会新建播放器，重新从默认的合集/分P模式解析，仍应沿用来源列表。
        repeat(2) {
            assertEquals(
                PlayMode.ListOrder,
                repository.resolvePlayModeForPreloadedList(PlayMode.PartAndEpisode),
            )
        }
    }

    @Test
    fun `feed playback respects explicitly selected non episode modes`() {
        val repository = VideoInfoRepository()
        repository.setPreloadedVideoList(
            items = listOf(video(10), video(20)),
            currentAvid = 10,
            preferListPlayback = true,
        )

        listOf(
            PlayMode.SingleVideo,
            PlayMode.SingleLoop,
            PlayMode.ListOrder,
            PlayMode.ListOrderReverse,
            PlayMode.RelatedVideo,
            PlayMode.Custom,
        ).forEach { mode ->
            assertEquals(mode, repository.resolvePlayModeForPreloadedList(mode))
        }
    }

    private fun video(avid: Long) = VideoCardData(
        avid = avid,
        title = "video-$avid",
        cover = "",
        upName = "up"
    )
}
