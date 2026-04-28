package dev.aaa1115910.bv.player.tv.controller

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.DenseListItem
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import dev.aaa1115910.bv.util.ImageSize
import dev.aaa1115910.bv.util.resizedImageUrl
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerConfigData
import dev.aaa1115910.bv.player.entity.VideoListItem
import dev.aaa1115910.bv.player.entity.VideoListItemData
import dev.aaa1115910.bv.player.entity.VideoListInteractiveNode
import dev.aaa1115910.bv.player.entity.VideoListPart
import dev.aaa1115910.bv.player.entity.VideoListPgcEpisode
import dev.aaa1115910.bv.player.entity.VideoListUgcEpisode
import dev.aaa1115910.bv.player.entity.VideoListUgcEpisodeTitle
import dev.aaa1115910.bv.util.formatHourMinSec
import dev.aaa1115910.bv.util.formatPubTimeString
import dev.aaa1115910.bv.util.requestFocus
import java.util.Date

@Composable
fun VideoListController(
    modifier: Modifier = Modifier,
    show: Boolean,
    onPlayNewVideo: (VideoListItem) -> Unit
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val videoPlayerConfigData = LocalVideoPlayerConfigData.current
    val availableVideoList = videoPlayerConfigData.availableVideoList
    val currentVideoCid = videoPlayerConfigData.currentVideoCid
    val focusRequester = remember { FocusRequester() }
    val videoListContainsUgcEpisode = availableVideoList.any { it is VideoListUgcEpisode }
    val hasCurrentInteractiveNode = availableVideoList.any {
        it is VideoListInteractiveNode && it.isCurrent
    }
    val currentIndex = availableVideoList.indexOfFirst {
        when (it) {
            is VideoListInteractiveNode -> it.isCurrent || (
                it.cid == currentVideoCid && !hasCurrentInteractiveNode
            )
            is VideoListItemData -> it.cid == currentVideoCid
            else -> false
        }
    }

    Box {
        AnimatedVisibility(
            visible = show,
            enter = expandHorizontally(),
            exit = shrinkHorizontally()
        ) {
            // 在动画内容中处理滚动和焦点请求
            LaunchedEffect(currentIndex, availableVideoList.size) {
                if (currentIndex >= 0 && currentIndex < availableVideoList.size) {
                    listState.scrollToItem(currentIndex)
                }
                focusRequester.requestFocus(scope)
            }
            Surface(
                modifier = modifier,
                colors = SurfaceDefaults.colors(
                    containerColor = Color.Black.copy(alpha = 0.5f)
                )
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .width(300.dp)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 80.dp)
                    ) {
                        items(items = availableVideoList) { video ->
                            when (video) {
                                is VideoListPart -> {
                                    val isSelected = video.cid == currentVideoCid
                                    val itemModifier = if (isSelected) {
                                        Modifier.focusRequester(focusRequester)
                                    } else {
                                        Modifier
                                    }
                                    VideoListCardItem(
                                        modifier = itemModifier.padding(start = if (videoListContainsUgcEpisode) 12.dp else 0.dp),
                                        title = (" - ".takeIf { videoListContainsUgcEpisode }
                                            ?: "") + "P${video.index + 1} ${if (video.partTitle.isNotEmpty()) video.partTitle else video.title}",
                                        cover = video.cover,
                                        duration = video.duration,
                                        pubDate = video.pubDate,
                                        isSelected = isSelected,
                                        onClick = { if (!isSelected) onPlayNewVideo(video) }
                                    )
                                }

                                is VideoListUgcEpisode -> {
                                    val isSelected = video.cid == currentVideoCid
                                    val itemModifier = if (isSelected) {
                                        Modifier.focusRequester(focusRequester)
                                    } else {
                                        Modifier
                                    }
                                    VideoListCardItem(
                                        modifier = itemModifier,
                                        title = "EP${video.index + 1} ${if (video.partTitle.isNotEmpty()) video.partTitle else video.title}",
                                        cover = video.cover,
                                        duration = video.duration,
                                        pubDate = video.pubDate,
                                        isSelected = isSelected,
                                        onClick = { if (!isSelected) onPlayNewVideo(video) }
                                    )
                                }

                                is VideoListPgcEpisode -> {
                                    val isSelected = video.cid == currentVideoCid
                                    val itemModifier = if (isSelected) {
                                        Modifier.focusRequester(focusRequester)
                                    } else {
                                        Modifier
                                    }
                                    VideoListCardItem(
                                        modifier = itemModifier,
                                        title = video.partTitle,
                                        cover = video.cover,
                                        duration = video.duration,
                                        pubDate = video.pubDate,
                                        isSelected = isSelected,
                                        onClick = { if (!isSelected) onPlayNewVideo(video) }
                                    )
                                }

                                is VideoListInteractiveNode -> {
                                    val isSelected = video.isCurrent || (
                                        video.cid == currentVideoCid && !hasCurrentInteractiveNode
                                    )
                                    val itemModifier = if (isSelected) {
                                        Modifier.fillMaxWidth().focusRequester(focusRequester)
                                    } else {
                                        Modifier.fillMaxWidth()
                                    }
                                    DenseListItem(
                                        modifier = itemModifier,
                                        headlineContent = {
                                            Text(
                                                text = "分支${video.index + 1} ${if (video.partTitle.isNotEmpty()) video.partTitle else video.title}"
                                            )
                                        },
                                        onClick = { if (!isSelected) onPlayNewVideo(video) },
                                        selected = isSelected
                                    )
                                }

                                is VideoListUgcEpisodeTitle -> {
                                    Text(
                                        modifier = Modifier.padding(
                                            top = 12.dp,
                                            bottom = 0.dp,
                                        ),
                                        text = "- EP${video.index + 1} ${video.title}",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoListCardItem(
    modifier: Modifier = Modifier,
    title: String,
    cover: String,
    duration: Int,
    pubDate: Long,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var hasFocus by remember { mutableStateOf(false) }
    val hasCover = cover.isNotBlank()

    Surface(
        modifier = modifier.onFocusChanged { hasFocus = it.hasFocus },
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.09f),
            focusedContainerColor = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.1f)
        ),
        scale = ClickableSurfaceDefaults.scale(scale = 1f, focusedScale = 1f),
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small),
        onClick = onClick
    ) {
        val borderStroke = when {
            hasFocus -> BorderStroke(2.dp, MaterialTheme.colorScheme.onBackground)
            isSelected -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
            else -> null
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .then(
                    if (borderStroke != null) Modifier.border(
                        border = borderStroke,
                        shape = MaterialTheme.shapes.small
                    ) else Modifier
                )
        ) {
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                if (hasCover) {
                    AsyncImage(
                        model = cover.resizedImageUrl(ImageSize.UgcEpisodeCover),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(4f / 3f)
                            .clip(RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (duration > 0) {
                            Text(
                                text = (duration * 1000L).formatHourMinSec(),
                                fontSize = 11.sp,
                                color = LocalContentColor.current.copy(alpha = 0.9f),
                                maxLines = 1
                            )
                        }
                        if (pubDate > 0L) {
                            Text(
                                text = Date(pubDate * 1000L).formatPubTimeString(),
                                fontSize = 11.sp,
                                color = LocalContentColor.current.copy(alpha = 0.9f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
