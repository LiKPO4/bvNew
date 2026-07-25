package dev.aaa1115910.bv.tv.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.WatchLater
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.aaa1115910.bv.tv.activities.video.UpInfoActivity
import dev.aaa1115910.bv.tv.activities.video.VideoInfoActivity
import dev.aaa1115910.bv.tv.manager.VideoUserActionManager
import dev.aaa1115910.bv.util.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * UGC 视频卡片长按操作菜单
 *
 * @param show 是否显示
 * @param aid 视频 aid
 * @param upId UP 主 mid
 * @param upName UP 主昵称
 * @param upFace UP 主头像
 * @param onDismiss 关闭回调
 * @param onDelete 删除操作回调，不为 null 时显示删除菜单项
 * @param deleteLabel 删除菜单项文案，默认"删除"
 */
@Composable
fun VideoActionMenu(
    show: Boolean,
    aid: Long,
    upId: Long,
    upName: String,
    upFace: String,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    deleteLabel: String = "删除"
) {
    if (!show) return

    val context = LocalContext.current
    val scope = remember { CoroutineScope(SupervisorJob()) }
    val firstItemFocusRequester = remember { FocusRequester() }

    // 菜单打开时对登录态取快照，不响应运行时登出
    val isLogin = remember { Prefs.isLogin }

    // 惰性加载操作状态（仅首次打开菜单时触发）
    LaunchedEffect(aid) {
        if (isLogin) {
            VideoUserActionManager.ensureStateLoaded(aid)
        }
    }

    // 获取当前操作状态
    val actionState by VideoUserActionManager.getStateFlow(aid).collectAsState()

    // 收藏夹选择对话框状态
    var showFavoriteFolderDialog by remember { mutableStateOf(false) }
    val favoriteFolders by VideoUserActionManager.getFavoriteFoldersFlow().collectAsState()

    // 弹窗打开后第一个 KEY_UP 是松开长按时触发的，吞掉它防止误触菜单项
    var hasConsumedFirstKeyUp by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        firstItemFocusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyUp && !hasConsumedFirstKeyUp) {
                        hasConsumedFirstKeyUp = true
                        return@onPreviewKeyEvent true
                    }
                    false
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .widthIn(300.dp, 450.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MenuItem(
                        focusRequester = firstItemFocusRequester,
                        icon = Icons.Rounded.Person,
                        text = "UP 主页",
                        onClick = {
                            if (upId > 0) {
                                UpInfoActivity.actionStart(context, mid = upId, name = upName, face = upFace)
                            }
                            onDismiss()
                        }
                    )
                    MenuItem(
                        icon = Icons.Rounded.Info,
                        text = "详情",
                        onClick = {
                            VideoInfoActivity.actionStart(context, aid = aid, forceShowDetail = true)
                            onDismiss()
                        }
                    )
                    if (isLogin) {
                        MenuItem(
                            icon = if (actionState.liked) Icons.Rounded.ThumbUp else Icons.Outlined.ThumbUp,
                            text = if (actionState.liked) "已点赞" else "点赞",
                            tint = if (actionState.liked) Color(0xfffb7299) else MaterialTheme.colorScheme.onSurface,
                            onClick = {
                                scope.launch {
                                    if (actionState.liked) {
                                        VideoUserActionManager.delLike(aid)
                                    } else {
                                        VideoUserActionManager.addLike(aid)
                                    }
                                }
                            }
                        )
                        MenuItem(
                            icon = if (actionState.favorited) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            text = if (actionState.favorited) "已收藏" else "收藏",
                            tint = if (actionState.favorited) Color(0xfffb7299) else MaterialTheme.colorScheme.onSurface,
                            onClick = {
                                showFavoriteFolderDialog = true
                            }
                        )
                        MenuItem(
                            icon = if (actionState.coin) Icons.Rounded.Paid else Icons.Outlined.Paid,
                            text = if (actionState.coin) "已投币" else "投币",
                            tint = if (actionState.coin) Color(0xfffb7299) else MaterialTheme.colorScheme.onSurface,
                            onClick = {
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        VideoUserActionManager.addCoin(aid)
                                    }
                                }
                            }
                        )
                        MenuItem(
                            icon = Icons.Rounded.WatchLater,
                            text = "稍后再看",
                            onClick = {
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        VideoUserActionManager.addToView(aid)
                                    }
                                }
                            }
                        )
                    }
                    if (onDelete != null) {
                        MenuItem(
                            icon = Icons.Rounded.Delete,
                            text = deleteLabel,
                            textColor = Color(0xfffb7299),
                            onClick = {
                                onDelete()
                                onDismiss()
                            }
                        )
                    }
                }
            }

            // 收藏夹选择对话框（叠加在菜单之上，仅登录后可用）
            if (isLogin) {
                FavoriteFolderDialog(
                    show = showFavoriteFolderDialog,
                    onDismiss = { showFavoriteFolderDialog = false },
                    userFavoriteFolders = favoriteFolders,
                    favoriteFolderIds = actionState.favoriteFolderIds,
                    onUpdateFavoriteFolders = { folderIds ->
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                VideoUserActionManager.updateVideoFavoriteFolders(
                                    aid = aid,
                                    folderIds = folderIds
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    text: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            pressedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f, pressedScale = 1f),
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                modifier = Modifier.size(22.dp),
                imageVector = icon,
                contentDescription = null,
                tint = tint
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
        }
    }
}
