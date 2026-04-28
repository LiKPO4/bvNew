package dev.aaa1115910.bv.player.entity

interface VideoListItem

open class VideoListItemData(
    open val aid: Long,
    open val cid: Long? = null,
    open val epid: Int? = null,
    open val seasonId: Int? = null,
    open val title: String,
    open val partTitle: String = "",
    open val index: Int,
    open val cover: String = "",
    open val duration: Int = 0,
    open val pubDate: Long = 0L,
) : VideoListItem

data class VideoListPart(
    override val aid: Long,
    override val cid: Long,
    override val epid: Int? = null,
    override val seasonId: Int? = null,
    override val title: String,
    override val partTitle: String = "",
    override val index: Int,
    override val cover: String = "",
    override val duration: Int = 0,
    override val pubDate: Long = 0L,
) : VideoListItemData(aid, cid, epid, seasonId, title, partTitle, index, cover, duration, pubDate)

data class VideoListUgcEpisode(
    override val aid: Long,
    override val cid: Long,
    override val epid: Int? = null,
    override val seasonId: Int? = null,
    override val title: String,
    override val partTitle: String = "",
    override val index: Int,
    override val cover: String = "",
    override val duration: Int = 0,
    override val pubDate: Long = 0L,
) : VideoListItemData(aid, cid, epid, seasonId, title, partTitle, index, cover, duration, pubDate)

data class VideoListUgcEpisodeTitle(
    val index: Int,
    val title: String
) : VideoListItem

data class VideoListInteractiveNode(
    override val aid: Long,
    override val cid: Long,
    override val epid: Int? = null,
    override val seasonId: Int? = null,
    override val title: String,
    override val partTitle: String = "",
    override val index: Int,
    val nodeId: Long,
    val edgeId: Long? = null,
    val startPos: Int? = null,
    val isCurrent: Boolean = false,
) : VideoListItemData(aid, cid, epid, seasonId, title, partTitle, index)

data class VideoListPgcEpisode(
    override val aid: Long,
    override val cid: Long,
    override val epid: Int? = null,
    override val seasonId: Int? = null,
    override val title: String,
    override val partTitle: String = "",
    override val index: Int,
    override val cover: String = "",
    override val duration: Int = 0,
    override val pubDate: Long = 0L,
) : VideoListItemData(aid, cid, epid, seasonId, title, partTitle, index, cover, duration, pubDate)

data class VideoListOtherVideo(
    override val aid: Long,
    override val cid: Long? = null,
    override val epid: Int? = null,
    override val seasonId: Int? = null,
    override val title: String,
    override val partTitle: String = "",
    override val index: Int,
    override val cover: String = "",
    val upName: String = ""
) : VideoListItemData(aid, cid, epid, seasonId, title, partTitle, index, cover)