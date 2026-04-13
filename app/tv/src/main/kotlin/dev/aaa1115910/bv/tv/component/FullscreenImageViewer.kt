package dev.aaa1115910.bv.tv.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import dev.aaa1115910.biliapi.entity.Picture
import dev.aaa1115910.bv.util.isDpadLeft
import dev.aaa1115910.bv.util.isDpadRight
import dev.aaa1115910.bv.util.isKeyDown

/**
 * 全屏图片查看器
 *
 * @param pictures 图片列表
 * @param initialIndex 初始显示的图片索引
 * @param onDismiss 关闭回调
 */
@Composable
fun FullscreenImageViewer(
    pictures: List<Picture>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(initialIndex) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { event ->
                    if (!event.isKeyDown()) return@onPreviewKeyEvent false
                    when {
                        event.isDpadLeft() -> {
                            if (currentIndex > 0) currentIndex--
                            true
                        }
                        event.isDpadRight() -> {
                            if (currentIndex < pictures.size - 1) currentIndex++
                            true
                        }
                        event.key == Key.Back -> {
                            onDismiss()
                            true
                        }
                        else -> false
                    }
                },
            onClick = { /* 消费点击事件 */ },
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color.Black,
                focusedContainerColor = Color.Black,
                pressedContainerColor = Color.Black
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f, pressedScale = 1f),
            shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(0.dp))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // 图片
                val painter = rememberAsyncImagePainter(model = pictures[currentIndex].url)
                val painterState = painter.state

                if (painterState is AsyncImagePainter.State.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = Color.White
                    )
                }

                AsyncImage(
                    model = pictures[currentIndex].url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                // 页码指示器
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                        .background(
                            Color.Black.copy(alpha = 0.6f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "${currentIndex + 1}/${pictures.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            }
        }
    }
}
