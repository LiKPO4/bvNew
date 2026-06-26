package dev.aaa1115910.bv.tv.component.live

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.live.LiveRoomItem
import kotlinx.coroutines.delay

@Composable
fun LiveRoomListPanel(
    modifier: Modifier = Modifier,
    liveRooms: List<LiveRoomItem>,
    currentRoomId: Int,
    focusRequester: FocusRequester,
    onOpenLiveRoom: (LiveRoomItem) -> Unit,
) {
    val titleFontSize by animateFloatAsState(
        targetValue = 24f,
        label = "title font size",
        animationSpec = tween(durationMillis = 120)
    )

    // Find index of current room in the list
    val currentRoomIndex = remember(liveRooms, currentRoomId) {
        liveRooms.indexOfFirst { it.roomId == currentRoomId }
    }

    val listState = rememberLazyListState()
    val currentItemFocusRequester = remember { FocusRequester() }
    LaunchedEffect(currentRoomIndex) {
        if (currentRoomIndex >= 0) {
            listState.scrollToItem(currentRoomIndex)
            delay(200)
            currentItemFocusRequester.requestFocus()
        }
    }

    Column(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.7f)
                    )
                )
            )
    ) {
        Text(
            modifier = Modifier.padding(start = 36.dp, top = 3.dp, bottom = 3.dp),
            text = "直播列表",
            fontSize = titleFontSize.sp
        )
        LazyRow(
            modifier = Modifier
                .padding(vertical = 15.dp),
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(horizontal = 36.dp)
        ) {
            itemsIndexed(
                items = liveRooms,
                key = { index, room -> "live-panel-${index}-${room.roomId}" }
            ) { index, room ->
                val isCurrentRoom = index == currentRoomIndex
                LiveRoomCard(
                    modifier = Modifier.width(200.dp)
                        .then(
                            when {
                                isCurrentRoom -> Modifier.focusRequester(currentItemFocusRequester)
                                index == 0 -> Modifier.focusRequester(focusRequester)
                                else -> Modifier
                            }
                        ),
                    data = room,
                    onClick = { onOpenLiveRoom(room) },
                )
            }
        }
    }
}
