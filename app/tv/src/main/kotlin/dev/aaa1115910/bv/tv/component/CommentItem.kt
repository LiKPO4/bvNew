package dev.aaa1115910.bv.tv.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import dev.aaa1115910.biliapi.entity.Picture
import dev.aaa1115910.biliapi.entity.reply.Comment
import dev.aaa1115910.biliapi.entity.reply.EmoteSize
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.util.focusedBorder

/**
 * 评论列表项组件
 *
 * @param comment 评论数据
 * @param modifier 修饰符
 * @param onClick 点击回调
 * @param onLongClick 长按回调
 */
@Composable
fun CommentItem(
    comment: Comment,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .focusedBorder(MaterialTheme.shapes.small),
        onClick = onClick,
        onLongClick = onLongClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            pressedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        scale = ClickableSurfaceDefaults.scale(
            focusedScale = 1f,
            pressedScale = 1f
        ),
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 主评论
            CommentMainContent(comment = comment)
        }
    }
}

/**
 * 主评论内容
 */
@Composable
private fun CommentMainContent(
    comment: Comment
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 用户头像
        AsyncImage(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            model = comment.member.avatar,
            contentDescription = null,
        )

        // 评论内容
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 用户名 + 置顶标识
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (comment.isPinned) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color(0xfffb7299),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "置顶",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp
                        )
                    }
                }
                Text(
                    text = comment.member.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 评论内容（支持表情）
            CommentContent(
                content = comment.content,
                emotes = comment.emotes,
                modifier = Modifier.padding(top = 4.dp)
            )

            // 评论图片
            if (comment.pictures.isNotEmpty()) {
                CommentPictures(
                    pictures = comment.pictures,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // 底部信息：时间和点赞数
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 时间
                Text(
                    text = comment.timeDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f)
                )

                // 点赞数
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        modifier = Modifier.size(14.dp),
                        imageVector = androidx.compose.material.icons.Icons.Rounded.ThumbUp,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f)
                    )
                    Text(
                        text = formatLikeCount(comment.like),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }

                // 回复数
                if (comment.repliesCount > 0) {
                    Text(
                        text = "${comment.repliesCount} 回复",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

/**
 * 评论内容组件，支持表情显示（富文本）
 */
@Composable
fun CommentContent(
    content: List<String>,
    emotes: List<Comment.Emote>,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val emoteNameList = emotes.map { it.text }
    val inlineContentMap = emotes.associateWith { emote ->
        InlineTextContent(
            Placeholder(
                width = emote.size.fontSize.sp,
                height = emote.size.fontSize.sp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
            )
        ) {
            AsyncImage(
                model = emote.url,
                contentDescription = null
            )
        }
    }.mapKeys { it.key.text }

    Text(
        modifier = modifier,
        text = buildAnnotatedString {
            content.forEach { text ->
                if (emoteNameList.contains(text)) {
                    appendInlineContent(text)
                } else {
                    append(text)
                }
            }
        },
        inlineContent = inlineContentMap,
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White,
        maxLines = maxLines,
        overflow = overflow
    )
}

/**
 * 格式化点赞数
 */
private fun formatLikeCount(count: Long): String {
    return when {
        count >= 10000 -> "${count / 10000}万"
        else -> count.toString()
    }
}

/**
 * 生成 B 站图片缩略图 URL
 *
 * 参数格式：@{w}w_{h}h_{flags}.webp
 * dpr 固定为 2，格式固定 webp
 */
private fun buildThumbnailUrl(
    url: String,
    w: Int = 0,
    h: Int = 0,
    crop: Boolean = false,
    progressive: Boolean = true,
    dpr: Int = 2
): String {
    val baseUrl = url.split("@")[0].replace("//pre-", "//")
    val parts = mutableListOf<String>()
    if (w > 0) parts.add("${w * dpr}w")
    if (h > 0) parts.add("${h * dpr}h")
    if (crop) parts.add("1c")
    if (progressive) parts.add("1s")
    return if (parts.isNotEmpty()) "$baseUrl@${parts.joinToString("_")}.webp" else baseUrl
}

private data class CommentPictureItem(
    val width: Int,
    val height: Int,
    val thumbnailUrl: String,
    val original: Picture
)

/**
 * 计算评论图片的展示尺寸和缩略图 URL
 *
 * 与 bilibili PC 评论区 bili-comment-pictures-renderer 逻辑一致：
 * - 单图：横图框 240×135，竖图框 135×180，超长图裁剪
 * - 多图：统一 88×88 裁剪
 */
private fun calculatePictureItems(pictures: List<Picture>): List<CommentPictureItem> {
    val isSingle = pictures.size == 1
    val multipleSize = 88

    return pictures.map { pic ->
        val imgW = pic.width
        val imgH = pic.height
        val isHorizontal = imgW > imgH
        val ratio = if (isHorizontal) imgW.toFloat() / imgH else imgH.toFloat() / imgW
        val isLong = kotlin.math.floor(ratio.toDouble()).toInt() >= 3

        if (isSingle) {
            val singleHorizontal = 240 to 135
            val singleVertical = 135 to 180
            val targetRatio = imgW.toFloat() / imgH
            var w: Int
            var h: Int

            if (isLong) {
                val frame = if (isHorizontal) singleHorizontal else singleVertical
                w = frame.first
                h = frame.second
            } else if (!isHorizontal && imgW > singleVertical.first && imgH > singleVertical.second) {
                w = singleVertical.first
                h = singleVertical.second
            } else if (isHorizontal) {
                val frameRatio = singleHorizontal.first.toFloat() / singleHorizontal.second
                if (targetRatio > frameRatio) {
                    w = singleHorizontal.first
                    h = (w / targetRatio).toInt()
                } else {
                    h = singleHorizontal.second
                    w = (h * targetRatio).toInt()
                }
                if (w > imgW) { w = imgW; h = imgH }
            } else {
                val frameRatio = singleVertical.first.toFloat() / singleVertical.second
                if (targetRatio > frameRatio) {
                    w = singleVertical.first
                    h = (w / targetRatio).toInt()
                } else {
                    h = singleVertical.second
                    w = (h * targetRatio).toInt()
                }
                if (w > imgW) { w = imgW; h = imgH }
            }

            val thumbUrl = buildThumbnailUrl(
                pic.url, w = w, h = h,
                crop = isLong, progressive = true
            )
            CommentPictureItem(width = w, height = h, thumbnailUrl = thumbUrl, original = pic)
        } else {
            val thumbUrl = buildThumbnailUrl(
                pic.url, w = multipleSize, h = multipleSize,
                crop = true, progressive = true
            )
            CommentPictureItem(
                width = multipleSize, height = multipleSize,
                thumbnailUrl = thumbUrl, original = pic
            )
        }
    }
}

/**
 * 评论图片组件
 *
 * - 单图：保持比例，宽度不超过内容区
 * - 多图：一行最多 2 张，正方形裁剪
 */
@Composable
fun CommentPictures(
    pictures: List<Picture>,
    modifier: Modifier = Modifier
) {
    val validPictures = remember(pictures) {
        pictures.filter { it.url.isNotBlank() && it.width > 0 && it.height > 0 }
    }
    if (validPictures.isEmpty()) return

    val items = remember(validPictures) { calculatePictureItems(validPictures) }
    val isSingle = validPictures.size == 1

    if (isSingle) {
        val item = items.first()
        val ratio = if (item.height > 0) item.width.toFloat() / item.height else 1f
        AsyncImage(
            model = item.thumbnailUrl,
            contentDescription = null,
            modifier = modifier
                .widthIn(max = item.width.dp)
                .aspectRatio(ratio)
                .clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        val rows = items.chunked(2)
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            rows.forEach { rowItems ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rowItems.forEach { item ->
                        AsyncImage(
                            model = item.thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(item.width.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}

@Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CommentItemPreview() {
    BVTheme {
        CommentItem(
            comment = Comment(
                rpid = 123456,
                mid = 789,
                oid = 12345,
                type = 1,
                parent = 0,
                content = listOf("这是一条测试评论", "[2333]", "后面还有内容"),
                member = Comment.Member(
                    mid = 789,
                    avatar = "",
                    name = "测试用户"
                ),
                timeDesc = "2小时前",
                emotes = listOf(
                    Comment.Emote(
                        text = "[2333]",
                        url = "https://i0.hdslb.com/bfs/emote/4352e2396c13e4150786d48e464d517174845b9c.png",
                        size = EmoteSize.Small
                    )
                ),
                pictures = emptyList(),
                replies = emptyList(),
                repliesCount = 5,
                like = 12345L
            )
        )
    }
}
