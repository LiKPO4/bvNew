package dev.aaa1115910.bv.tv.component

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.TabRowScope
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.pgc.PgcType
import dev.aaa1115910.biliapi.entity.ugc.UgcTypeV2
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.entity.NavSwitchMode
import dev.aaa1115910.bv.util.getDisplayName
import dev.aaa1115910.bv.util.ifElse
import dev.aaa1115910.bv.util.isKeyDown
import kotlinx.coroutines.delay

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TopNav(
    modifier: Modifier = Modifier,
    paddingTop: Dp = 12.dp,
    items: List<TopNavItem>,
    useSmallSize: Boolean = false,
    initialSelectedItem: TopNavItem? = null,
    navSwitchMode: NavSwitchMode = NavSwitchMode.Auto,
    tabFocusRequester: FocusRequester? = null,
    itemFocusRequesterProvider: ((Int, TopNavItem) -> FocusRequester?)? = null,
    onSelectedChanged: (TopNavItem) -> Unit = {},
    onClick: (TopNavItem) -> Unit = {},
    onLeftKeyEvent: () -> Unit = {}
) {
    val sizeScale = if (useSmallSize) 0.8f else 1f
    val topPadding = paddingTop * sizeScale
    val horizontalPadding = 12.dp * (if (useSmallSize) 1 / sizeScale else 1f)
    val bottomPadding = 8.dp * sizeScale
    val separatorWidth = 12.dp * sizeScale

    if (items.isEmpty()) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(
                    top = topPadding,
                    bottom = bottomPadding,
                    start = horizontalPadding,
                    end = horizontalPadding
                ),
            horizontalArrangement = Arrangement.Center
        ) {}
        return
    }

    val defaultFocusRequester = tabFocusRequester ?: remember { FocusRequester() }

    var selectedNav by remember(initialSelectedItem) {
        mutableStateOf(initialSelectedItem ?: items.first())
    }

    var selectedTabIndex by remember(initialSelectedItem) {
        mutableIntStateOf(
            if (initialSelectedItem != null) {
                val index = items.indexOf(initialSelectedItem)
                if (index >= 0) index else 0
            } else 0
        )
    }

    // 在确认键切换模式下，focusedTabIndex 跟踪当前聚焦的 tab（视觉高亮）
    var focusedTabIndex by remember(initialSelectedItem) {
        mutableIntStateOf(
            if (initialSelectedItem != null) {
                val index = items.indexOf(initialSelectedItem)
                if (index >= 0) index else 0
            } else 0
        )
    }

    var tabMoved by remember { mutableStateOf(true) }

    val currentTabFocusRequester = run {
        val focusIndex = (if (navSwitchMode == NavSwitchMode.Confirm) focusedTabIndex else selectedTabIndex)
            .coerceIn(items.indices)
        itemFocusRequesterProvider?.invoke(focusIndex, items[focusIndex]) ?: defaultFocusRequester
    }

    LaunchedEffect(items, initialSelectedItem) {
        if (items.isEmpty()) return@LaunchedEffect

        val nextSelectedItem = initialSelectedItem?.takeIf { it in items } ?: items.first()
        selectedNav = nextSelectedItem
        val idx = items.indexOf(nextSelectedItem).takeIf { it >= 0 } ?: 0
        selectedTabIndex = idx
        focusedTabIndex = idx
        tabMoved = true
    }

    LaunchedEffect(selectedNav) {
        if (selectedNav !in items) return@LaunchedEffect
        delay(200)
        onSelectedChanged(selectedNav)
        // 别急着向下移动焦点，动画还没结束
        delay(400)
        tabMoved = true
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = topPadding,
                bottom = bottomPadding,
                start = horizontalPadding,
                end = horizontalPadding
            )
            .onFocusChanged {
                if (!it.hasFocus) {
                    focusedTabIndex = selectedTabIndex
                }
            },
        horizontalArrangement = Arrangement.Center
    ) {
        TabRow(
            modifier = Modifier
                .then(
                    if (navSwitchMode == NavSwitchMode.Auto) {
                        Modifier.focusRestorer(currentTabFocusRequester)
                    } else {
                        Modifier.focusProperties {
                            enter = { currentTabFocusRequester }
                        }
                    }
                )
                .onPreviewKeyEvent {
                    if (it.isKeyDown()) {
                        val currentFocusIndex = if (navSwitchMode == NavSwitchMode.Confirm) focusedTabIndex else selectedTabIndex
                        if (it.key == Key.DirectionLeft && currentFocusIndex == 0) {
                            onLeftKeyEvent()
                            return@onPreviewKeyEvent true
                        }
                        if (it.key == Key.DirectionDown) {
                            return@onPreviewKeyEvent !tabMoved
                        }
                    }
                    false
                },
            selectedTabIndex = selectedTabIndex,
            indicator = { _, _ -> },
            separator = { Spacer(modifier = Modifier.width(separatorWidth)) },
        ) {
            items.forEachIndexed { index, tab ->
                val itemFocusRequester = itemFocusRequesterProvider?.invoke(index, tab)
                val itemFocusModifier = itemFocusRequester?.let { Modifier.focusRequester(it) } ?: Modifier
                val useSharedFocusRequester = itemFocusRequester == null &&
                        if (navSwitchMode == NavSwitchMode.Confirm) index == focusedTabIndex else index == selectedTabIndex
                NavItemTab(
                    modifier = Modifier
                        .then(itemFocusModifier)
                        .ifElse(
                            useSharedFocusRequester,
                            Modifier.focusRequester(defaultFocusRequester)
                        ),
                    topNavItem = tab,
                    useSmallSize = useSmallSize,
                    selected = index == selectedTabIndex,
                    focused = navSwitchMode == NavSwitchMode.Confirm && index == focusedTabIndex && index != selectedTabIndex,
                    onFocus = {
                        if (navSwitchMode == NavSwitchMode.Auto) {
                            // 自动切换模式：聚焦即切换
                            val isSameTab = tab == selectedNav
                            selectedNav = tab
                            selectedTabIndex = index
                            if (!isSameTab) {
                                tabMoved = false
                            }
                        } else {
                            // 确认键切换模式：聚焦只更新视觉状态
                            focusedTabIndex = index
                        }
                    },
                    onClick = {
                        if (navSwitchMode == NavSwitchMode.Confirm) {
                            // 确认键切换模式：按确认键才切换
                            val isSameTab = tab == selectedNav
                            selectedNav = tab
                            selectedTabIndex = index
                            focusedTabIndex = index
                            if (!isSameTab) {
                                tabMoved = false
                            } else {
                                onClick(tab)
                            }
                        } else {
                            onClick(tab)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun TabRowScope.NavItemTab(
    modifier: Modifier = Modifier,
    topNavItem: TopNavItem,
    useSmallSize: Boolean = false,
    selected: Boolean,
    focused: Boolean = false,
    onClick: () -> Unit,
    onFocus: () -> Unit
) {
    val context = LocalContext.current
    var isFocused by remember { mutableStateOf(false) }
    val sizeScale = if (useSmallSize) 0.85f else 1f
    val tabHeight = 32.dp * sizeScale
    val tabHorizontalPadding = 16.dp * sizeScale
    val tabCornerRadius = 50.dp * sizeScale
    val baseTextStyle = MaterialTheme.typography.bodyLarge
    val textStyle = if (useSmallSize) {
        baseTextStyle.copy(
            fontSize = if (baseTextStyle.fontSize.isSpecified) baseTextStyle.fontSize * sizeScale else TextUnit.Unspecified,
            lineHeight = if (baseTextStyle.lineHeight.isSpecified) baseTextStyle.lineHeight * sizeScale else TextUnit.Unspecified,
            letterSpacing = if (baseTextStyle.letterSpacing.isSpecified) baseTextStyle.letterSpacing * sizeScale else TextUnit.Unspecified
        )
    } else {
        baseTextStyle
    }

    Tab(
        modifier = modifier.onFocusChanged { isFocused = it.hasFocus },
        selected = selected,
        onFocus = onFocus,
        onClick = onClick
    ) {
        val actualFocused = isFocused || focused
        Text(
            modifier = Modifier
                .height(tabHeight)
                .ifElse(
                    !actualFocused && selected,
                    Modifier.background(
                        color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(tabCornerRadius)
                    )
                )
                .ifElse(
                    actualFocused && !selected,
                    Modifier.background(
                        color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(tabCornerRadius)
                    )
                )
                .ifElse(
                    actualFocused && selected,
                    Modifier.background(
                        color = MaterialTheme.colorScheme.inverseSurface,
                        shape = RoundedCornerShape(tabCornerRadius)
                    )
                )
                .wrapContentHeight(Alignment.CenterVertically)
                .padding(horizontal = tabHorizontalPadding),
            text = topNavItem.getDisplayName(context),
            style = textStyle,
            color = if (!actualFocused && selected) MaterialTheme.colorScheme.surface 
                    else if (actualFocused && !selected) MaterialTheme.colorScheme.inverseSurface
                    else if (actualFocused && selected) MaterialTheme.colorScheme.surface
                    else MaterialTheme.colorScheme.inverseSurface
        )
    }
}

interface TopNavItem {
    fun getDisplayName(context: Context = BVApp.context): String
}

enum class HomeTopNavItem(private val displayName: String) : TopNavItem {
    Recommend("推荐"),
    Popular("热门"),
    Dynamics("动态"),
    History("历史"),
    Favorite("收藏"),
    FollowingSeason("追番"),
    ToView("稍后再看");

    override fun getDisplayName(context: Context): String {
        return displayName
    }
}

enum class UgcTopNavItem(private val ugcType: UgcTypeV2) : TopNavItem {
    Douga(UgcTypeV2.Douga),
    Game(UgcTypeV2.Game),
    Kichiku(UgcTypeV2.Kichiku),
    Music(UgcTypeV2.Music),
    Dance(UgcTypeV2.Dance),
    Cinephile(UgcTypeV2.Cinephile),
    Ent(UgcTypeV2.Ent),
    Knowledge(UgcTypeV2.Knowledge),
    Tech(UgcTypeV2.Tech),
    Information(UgcTypeV2.Information),
    Food(UgcTypeV2.Food),
    ShortPlay(UgcTypeV2.Shortplay),
    Car(UgcTypeV2.Car),
    Fashion(UgcTypeV2.Fashion),
    Sports(UgcTypeV2.Sports),
    Animal(UgcTypeV2.Animal),
    Vlog(UgcTypeV2.Vlog),
    Painting(UgcTypeV2.Painting),
    Ai(UgcTypeV2.Ai),
    Home(UgcTypeV2.Home),
    Outdoors(UgcTypeV2.Outdoors),
    Gym(UgcTypeV2.Gym),
    Handmake(UgcTypeV2.Handmake),
    Travel(UgcTypeV2.Travel),
    Rural(UgcTypeV2.Rural),
    Parenting(UgcTypeV2.Parenting),
    Health(UgcTypeV2.Health),
    Emotion(UgcTypeV2.Emotion),
    LifeJoy(UgcTypeV2.LifeJoy),
    LifeExperience(UgcTypeV2.LifeExperience),
    Mysticism(UgcTypeV2.Mysticism);

    override fun getDisplayName(context: Context): String {
        return ugcType.getDisplayName(context)
    }
}

enum class PgcTopNavItem(private val pgcType: PgcType) : TopNavItem {
    Anime(PgcType.Anime),
    GuoChuang(PgcType.GuoChuang),
    Movie(PgcType.Movie),
    Documentary(PgcType.Documentary),
    Tv(PgcType.Tv),
    Variety(PgcType.Variety);

    override fun getDisplayName(context: Context): String {
        return pgcType.getDisplayName(context)
    }
}