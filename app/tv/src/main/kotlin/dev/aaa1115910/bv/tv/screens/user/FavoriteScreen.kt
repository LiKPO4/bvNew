package dev.aaa1115910.bv.tv.screens.user

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.FavoriteFolderMetadata
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.entity.NavSwitchMode
import dev.aaa1115910.bv.tv.activities.video.UpInfoActivity
import dev.aaa1115910.bv.tv.component.videocard.SmallVideoCard
import dev.aaa1115910.bv.tv.activities.video.VideoInfoActivity
import dev.aaa1115910.bv.tv.component.TopNav
import dev.aaa1115910.bv.tv.component.TopNavItem
import dev.aaa1115910.bv.tv.util.blockDownFocusExitAtGridEnd
import dev.aaa1115910.bv.tv.util.ProvideListBringIntoViewSpec
import dev.aaa1115910.bv.tv.util.rememberTvLazyListFocusRestorer
import dev.aaa1115910.bv.tv.util.stableItemKey
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.onDelayFocusChanged
import dev.aaa1115910.bv.viewmodel.user.FavoriteViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun FavoriteScreen(
    modifier: Modifier = Modifier,
    favoriteViewModel: FavoriteViewModel = koinViewModel(),
    showPageTitle: Boolean = true
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navSwitchMode by Prefs.navSwitchModeFlow.collectAsState(Prefs.navSwitchMode)
    var currentIndex by remember { mutableIntStateOf(0) }
    val showLargeTitle by remember { derivedStateOf { currentIndex < 4 } }
    val titleFontSize by animateFloatAsState(
        targetValue = if (showLargeTitle) 48f else 24f,
        label = "title font size"
    )
    val focusRequester = remember { FocusRequester() }
    val defaultFocusRequester = remember { FocusRequester() }
    val gridFocusRestorer = rememberTvLazyListFocusRestorer(defaultFocusRequester)
    var focusOnTabs by remember { mutableStateOf(true) }
    var focusOnGrid by remember { mutableStateOf(false) }
    val lazyGridState = rememberLazyGridState()
    val favoriteTopNavItems = favoriteViewModel.favoriteFolderMetadataList.map(::FavoriteFolderTopNavItem)

    val updateCurrentFavoriteFolder: (folderMetadata: FavoriteFolderMetadata) -> Unit =
        { folderMetadata ->
            favoriteViewModel.currentFavoriteFolderMetadata = folderMetadata
            favoriteViewModel.favorites.clear()
            favoriteViewModel.resetPageNumber()
            favoriteViewModel.updateFolderItems(force = true)
        }

    BackHandler(
        enabled = focusOnGrid
    ) {
        scope.launch(Dispatchers.Main) {
            lazyGridState.scrollToItem(0)
            defaultFocusRequester.requestFocus()
            focusOnGrid = false
        }
    }

    LaunchedEffect(Unit) {
        if (favoriteViewModel.favoriteFolderMetadataList.isEmpty()) {
            favoriteViewModel.clearData()
            favoriteViewModel.updateFoldersInfo()
            if (showPageTitle) {
                delay(100)
                defaultFocusRequester.requestFocus()
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            if (showPageTitle) {
                Box(
                    modifier = Modifier.padding(
                        start = 48.dp,
                        top = 24.dp,
                        bottom = 8.dp,
                        end = 48.dp
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${stringResource(R.string.user_homepage_favorite)} - ${favoriteViewModel.currentFavoriteFolderMetadata?.title}",
                            fontSize = titleFontSize.sp
                        )
                        Text(
                            text = stringResource(
                                R.string.load_data_count,
                                favoriteViewModel.favorites.size
                            ),
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        ProvideListBringIntoViewSpec(padding = 24.dp) {
            LazyVerticalGrid(
                modifier = gridFocusRestorer.containerModifier(
                    Modifier
                        .padding(innerPadding)
                        .blockDownFocusExitAtGridEnd(
                            currentIndex = currentIndex,
                            itemCount = favoriteViewModel.favorites.size,
                            columnCount = 4
                        )
                ),
                state = lazyGridState,
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(
                    top = if (showPageTitle) 20.dp else 4.dp,
                    bottom = 20.dp,
                    start = 20.dp,
                    end = 20.dp
                ),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item(
                    span = { GridItemSpan(4) }
                ) {
                    TopNav(
                        modifier = Modifier
                            .focusRequester(defaultFocusRequester)
                            .onFocusChanged { focusOnTabs = it.hasFocus }
                            .onDelayFocusChanged(50) {
                                if (focusOnTabs) {
                                    focusRequester.requestFocus()
                                }
                            },
                        paddingTop = 0.dp,
                        items = favoriteTopNavItems,
                        isLargePadding = false,
                        initialSelectedItem = favoriteTopNavItems.firstOrNull {
                            it.folderMetadata == favoriteViewModel.currentFavoriteFolderMetadata
                        },
                        navSwitchMode = navSwitchMode,
                        tabFocusRequester = focusRequester,
                        onSelectedChanged = { selectedItem ->
                            val folderMetadata = (selectedItem as FavoriteFolderTopNavItem).folderMetadata
                            if (favoriteViewModel.currentFavoriteFolderMetadata != folderMetadata) {
                                updateCurrentFavoriteFolder(folderMetadata)
                            }
                        }
                    )
                }
                itemsIndexed(
                    items = favoriteViewModel.favorites,
                    key = { index, history -> "$index-${history.stableItemKey()}" }
                ) { index, history ->
                    SmallVideoCard(
                        modifier = gridFocusRestorer.firstItemModifier(index),
                        data = history,
                        onClick = { VideoInfoActivity.actionStart(context, history.avid) },
                        onLongClick = {
                            UpInfoActivity.actionStart(
                                context,
                                mid = history.upId,
                                name = history.upName,
                                face = history.upFace
                            )
                        },
                        onFocus = {
                            focusOnGrid = true
                            currentIndex = index
                            //预加载
                            if (index + 12 > favoriteViewModel.favorites.size) {
                                favoriteViewModel.updateFolderItems()
                            }
                        }
                    )
                }
            }
        }
    }
}

private data class FavoriteFolderTopNavItem(
    val folderMetadata: FavoriteFolderMetadata
) : TopNavItem {
    override fun getDisplayName(context: Context): String {
        return folderMetadata.title
    }
}