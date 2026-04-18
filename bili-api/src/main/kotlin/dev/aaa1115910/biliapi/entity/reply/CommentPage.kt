package dev.aaa1115910.biliapi.entity.reply

data class CommentPage(
    val nextWebPage: String = "",
    val nextAppPage: String = ""
)

data class CommentReplyPage(
    val nextWebPage: Int = 1,
    val nextAppPage: String = ""
)
