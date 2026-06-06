package dev.aaa1115910.bv.tv.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.SuggestionChip
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.video.Tag
import dev.aaa1115910.bv.util.isDpadDown
import dev.aaa1115910.bv.util.isDpadUp
import dev.aaa1115910.bv.util.isKeyDown
import dev.aaa1115910.bv.util.onBackPressed
import dev.aaa1115910.bv.util.requestFocus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 视频简介浮层组件
 *
 * @param show 是否显示浮层
 * @param description 视频简介
 * @param tags 视频标签列表
 * @param onHide 关闭浮层回调
 * @param onClickTag 标签点击回调
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DescriptionPanel(
    show: Boolean,
    description: String,
    tags: List<Tag> = emptyList(),
    onHide: () -> Unit,
    onClickTag: (Tag) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val contentFocusRequester = remember { FocusRequester() }
    val firstTagFocusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    var hasRequestedFocus by remember { mutableStateOf(false) }

    // 显示时请求焦点：优先聚焦第一个标签，没有标签则聚焦简介内容
    LaunchedEffect(show) {
        if (show) {
            delay(300)
            if (tags.isNotEmpty()) {
                firstTagFocusRequester.requestFocus(scope)
            } else {
                contentFocusRequester.requestFocus(scope)
            }
            hasRequestedFocus = true
        } else {
            hasRequestedFocus = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onHide),
        contentAlignment = Alignment.CenterEnd
    ) {
        AnimatedVisibility(
            visible = show,
            enter = expandHorizontally(expandFrom = Alignment.End),
            exit = shrinkHorizontally(shrinkTowards = Alignment.End)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .widthIn(min = 300.dp, max = 400.dp)
                    .fillMaxWidth(0.3f)
                    .clickable(enabled = true, onClick = {}) // 阻止点击穿透
                    .onBackPressed { onHide() },
                colors = SurfaceDefaults.colors(
                    containerColor = Color.Black.copy(alpha = 0.85f)
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 标题栏（固定不滚动）
                    Text(
                        text = "视频简介",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    // 标签行（固定不滚动，可横向滚动）
                    if (tags.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            itemsIndexed(
                                items = tags,
                                key = { index, tag -> "$index-desc-tag-${tag.name}" }
                            ) { index, tag ->
                                SuggestionChip(
                                    modifier = if (index == 0) Modifier.focusRequester(firstTagFocusRequester) else Modifier,
                                    onClick = { onClickTag(tag) }
                                ) {
                                    Text(text = tag.name)
                                }
                            }
                        }
                    }

                    // 简介内容（可滚动）
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .focusRequester(contentFocusRequester)
                            .focusable()
                            .onPreviewKeyEvent { event ->
                                when {
                                    event.isKeyDown() && event.isDpadDown() -> {
                                        // 仅在还有可向下滚动空间时拦截事件，
                                        // 否则让事件继续冒泡到焦点系统（焦点会移出内容区）
                                        if (scrollState.value < scrollState.maxValue) {
                                            scope.launch {
                                                val scrollAmount =
                                                    with(density) { 100.dp.toPx() }
                                                scrollState.animateScrollBy(scrollAmount)
                                            }
                                            true
                                        } else {
                                            false
                                        }
                                    }

                                    event.isKeyDown() && event.isDpadUp() -> {
                                        // 仅在还有可向上滚动空间时拦截事件，
                                        // 否则让事件继续冒泡到焦点系统，由 focusProperties.up
                                        // 跳回第一个标签
                                        if (scrollState.value > 0) {
                                            scope.launch {
                                                val scrollAmount =
                                                    with(density) { 100.dp.toPx() }
                                                scrollState.animateScrollBy(-scrollAmount)
                                            }
                                            true
                                        } else {
                                            false
                                        }
                                    }

                                    else -> false
                                }
                            }
                    ) {
                        Text(
                            modifier = Modifier
                                .verticalScroll(scrollState)
                                .padding(top = 4.dp),
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}
