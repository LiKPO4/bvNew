package dev.aaa1115910.bv.tv.component.buttons

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonBorder
import androidx.tv.material3.ButtonColors
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.FavoriteFolderMetadata
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.tv.component.FavoriteFolderDialog
import dev.aaa1115910.bv.tv.manager.VideoUserActionManager
import dev.aaa1115910.bv.ui.theme.BVTheme

@Composable
fun FavoriteButton(
    modifier: Modifier = Modifier,
    isFavorite: Boolean,
    favoriteFolderIds: List<Long> = emptyList(),
    onAddToDefaultFavoriteFolder: () -> Unit,
    onUpdateFavoriteFolders: (List<Long>) -> Unit,
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 6.dp), // 减小内边距
    colors: ButtonColors = ButtonDefaults.colors(),
    border: ButtonBorder = ButtonDefaults.border(),
    onDialogVisibilityChanged: (Boolean) -> Unit = {},
    dialogContainerColor: Color = AlertDialogDefaults.containerColor
) {
    val userFavoriteFolders by VideoUserActionManager.getFavoriteFoldersFlow().collectAsState()
    var showFavoriteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(showFavoriteDialog) {
        onDialogVisibilityChanged(showFavoriteDialog)
    }

    Button(
        modifier = modifier,
        contentPadding = contentPadding,
        colors = colors,
        border = border,
        shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp)), // 设置为小圆角
        onClick = {
            if (showFavoriteDialog) return@Button
            if (isFavorite) {
                // 有收藏状态，显示收藏夹选择对话框
                showFavoriteDialog = true
            } else {
                // 无收藏状态
                if (userFavoriteFolders.size > 1) {
                    // 有多个收藏夹，显示收藏夹选择对话框
                    showFavoriteDialog = true
                } else {
                    // 否则使用默认收藏夹
                    onAddToDefaultFavoriteFolder()
                }
            }
        }
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp), // 减小间距
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier
                    .size(16.dp),
                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = null,
                tint = if (isFavorite) Color(0xfffb7299) else LocalContentColor.current
            )
            Text(
                text = stringResource(R.string.favorite_button_text)
            )
        }
    }

    FavoriteDialog(
        show = showFavoriteDialog,
        onHideDialog = { showFavoriteDialog = false },
        userFavoriteFolders = userFavoriteFolders,
        favoriteFolderIds = favoriteFolderIds,
        onUpdateFavoriteFolders = onUpdateFavoriteFolders,
        dialogContainerColor = dialogContainerColor
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FavoriteDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    onHideDialog: () -> Unit,
    userFavoriteFolders: List<FavoriteFolderMetadata> = emptyList(),
    favoriteFolderIds: List<Long> = emptyList(),
    onUpdateFavoriteFolders: (List<Long>) -> Unit,
    dialogContainerColor: Color = AlertDialogDefaults.containerColor
) {
    FavoriteFolderDialog(
        modifier = modifier,
        show = show,
        onDismiss = onHideDialog,
        userFavoriteFolders = userFavoriteFolders,
        favoriteFolderIds = favoriteFolderIds,
        onUpdateFavoriteFolders = onUpdateFavoriteFolders,
        containerColor = dialogContainerColor
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun FavoriteButtonEnablePreview() {
    BVTheme {
        FavoriteButton(
            isFavorite = true,
            onAddToDefaultFavoriteFolder = {},
            onUpdateFavoriteFolders = {}
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun FavoriteButtonDisablePreview() {
    BVTheme {
        FavoriteButton(
            isFavorite = false,
            onAddToDefaultFavoriteFolder = {},
            onUpdateFavoriteFolders = {}
        )
    }
}

@Preview(device = "id:tv_1080p")
@Preview(device = "id:tv_1080p", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FavoriteDialogPreview() {
    val userFavoriteFolders = listOf(
        FavoriteFolderMetadata(0, 0, 0, "收藏夹1", null, false, 0),
        FavoriteFolderMetadata(1, 1, 0, "收藏夹2", null, false, 0),
        FavoriteFolderMetadata(2, 2, 0, "收藏夹3", null, false, 0),
        FavoriteFolderMetadata(3, 3, 0, "收藏夹4", null, false, 0),
        FavoriteFolderMetadata(4, 4, 0, "收藏夹5", null, false, 0),
        FavoriteFolderMetadata(5, 5, 0, "收藏夹6", null, false, 0),
        FavoriteFolderMetadata(6, 6, 0, "收藏夹7", null, false, 0),
        FavoriteFolderMetadata(7, 7, 0, "收藏夹8", null, false, 0),
        FavoriteFolderMetadata(8, 8, 0, "收藏夹9", null, false, 0),
        FavoriteFolderMetadata(9, 9, 0, "收藏夹10", null, false, 0),
    )
    BVTheme {
        FavoriteDialog(
            show = true,
            onHideDialog = {},
            userFavoriteFolders = userFavoriteFolders,
            onUpdateFavoriteFolders = {}
        )
    }
}