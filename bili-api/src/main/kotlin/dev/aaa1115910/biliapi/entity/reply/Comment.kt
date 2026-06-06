package dev.aaa1115910.biliapi.entity.reply

import dev.aaa1115910.biliapi.entity.Picture
import dev.aaa1115910.biliapi.entity.ugc.toSmartDate
import dev.aaa1115910.biliapi.entity.ugc.toSmartDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray

data class CommentsData(
    val comments: List<Comment> = emptyList(),
    val topReplies: List<Comment> = emptyList(),
    val nextPage: CommentPage = CommentPage(),
    val hasNext: Boolean
) {
    companion object {
        fun fromCommentData(commentData: dev.aaa1115910.biliapi.http.entity.reply.CommentData): CommentsData {
            val nextOffset = commentData.cursor.paginationReply?.nextOffset
            return CommentsData(
                comments = commentData.replies.map { Comment.fromReply(it) },
                topReplies = commentData.topReplies.map { Comment.fromReply(it, isPinned = true) },
                nextPage = CommentPage(
                    nextWebPage = commentData.cursor.paginationReply?.nextOffset ?: ""
                ),
                hasNext = commentData.cursor.isEnd.not() && nextOffset != null
            )
        }

        fun fromMainListReply(mainListReply: bilibili.main.community.reply.v1.MainListReply): CommentsData {
            val topReplies = buildList {
                mainListReply.topRepliesList.forEach { add(Comment.fromReplyInfo(it, isPinned = true)) }
                if (mainListReply.hasUpTop()) add(Comment.fromReplyInfo(mainListReply.upTop, isPinned = true))
                if (mainListReply.hasAdminTop()) add(Comment.fromReplyInfo(mainListReply.adminTop, isPinned = true))
                if (mainListReply.hasVoteTop()) add(Comment.fromReplyInfo(mainListReply.voteTop, isPinned = true))
            }.distinctBy { it.rpid }
            return CommentsData(
                comments = mainListReply.repliesList.map { Comment.fromReplyInfo(it) },
                topReplies = topReplies,
                nextPage = CommentPage(
                    nextAppPage = mainListReply.paginationReply.nextOffset
                ),
                hasNext = mainListReply.cursor.isEnd.not()
            )
        }
    }
}

data class Comment(
    val rpid: Long,
    val mid: Long,
    val oid: Long,
    val type: Long,
    val parent: Long,
    val content: List<String>,
    val member: Member,
    val timeDesc: String,
    val emotes: List<Emote>,
    val pictures: List<Picture>,
    val replies: List<Comment>,
    val topReplies: List<Comment> = emptyList(),
    val repliesCount: Int,
    val like: Long = 0,
    val isPinned: Boolean = false,
) {
    companion object {
        fun fromReply(
            reply: dev.aaa1115910.biliapi.http.entity.reply.CommentData.Reply,
            isPinned: Boolean = false
        ): Comment {
            return Comment(
                rpid = reply.rpid,
                mid = reply.mid,
                oid = reply.oid,
                type = reply.type,
                parent = reply.parent,
                content = reply.content.message.splitWithEmotes(*reply.content.emote.keys.toTypedArray()),
                member = Member(
                    mid = reply.mid,
                    avatar = reply.member.avatar,
                    name = reply.member.uname
                ),
                timeDesc = reply.ctime.toLong().toSmartDateTime() ?: "",
                emotes = reply.content.emote.values.map { Emote.fromEmote(it) },
                pictures = reply.content.pictures.map { Picture.fromPicture(it) },
                replies = reply.replies.map { fromReply(it) },
                repliesCount = reply.rcount,
                like = reply.like.toLong(),
                isPinned = isPinned
            )
        }

        fun fromReplyInfo(
            reply: bilibili.main.community.reply.v1.ReplyInfo,
            isPinned: Boolean = false
        ): Comment {
            val allReplies = reply.repliesList.map { fromReplyInfo(it) }
            val pinnedReplies = allReplies.filter { it.isPinned }
            val regularReplies = allReplies.filter { !it.isPinned }
            return Comment(
                rpid = reply.id,
                mid = reply.mid,
                oid = reply.oid,
                type = reply.type,
                parent = reply.parent,
                content = reply.content.message.splitWithEmotes(*reply.content.emoteMap.keys.toTypedArray()),
                member = Member(
                    mid = reply.mid,
                    avatar = reply.member.face,
                    name = reply.member.name
                ),
                timeDesc = reply.ctime.toSmartDateTime() ?: "",
                emotes = reply.content.emoteMap.values.map { Emote.fromEmote(it) },
                pictures = reply.content.picturesList.map { Picture.fromPicture(it) },
                replies = regularReplies,
                topReplies = pinnedReplies,
                repliesCount = reply.count.toInt(),
                like = reply.like,
                isPinned = isPinned
            )
        }
    }

    data class Member(
        val mid: Long,
        val avatar: String,
        val name: String
    )

    data class Emote(
        val text: String,
        val url: String,
        val size: EmoteSize
    ) {
        companion object {
            fun fromEmote(emote: dev.aaa1115910.biliapi.http.entity.reply.CommentData.Reply.Content.Emote): Emote {
                return Emote(
                    text = emote.text,
                    url = emote.url,
                    size = if (emote.meta.size == 1) EmoteSize.Small else EmoteSize.Large
                )
            }

            fun fromEmote(emote: bilibili.main.community.reply.v1.Emote): Emote {
                return Emote(
                    text = emote.text,
                    url = emote.url,
                    size = if (emote.size == 1L) EmoteSize.Small else EmoteSize.Large
                )
            }
        }
    }
}

enum class EmoteSize(val fontSize: Int) {
    Small(20), Large(20)
}

private fun String.splitWithEmotes(vararg emotes: String): List<String> {
    val delimiter = emotes.joinToString("|").replace("[", "\\[").replace("]", "\\]")
    val regex = Regex("(?=$delimiter)|(?<=$delimiter)")
    return this.split(regex)
}