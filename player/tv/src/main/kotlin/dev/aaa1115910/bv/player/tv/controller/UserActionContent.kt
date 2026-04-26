package dev.aaa1115910.bv.player.tv.controller

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester

enum class UserActionKey {
    Like,
    Favorite,
    Coin,
    ToView
}

typealias UserActionContent = @Composable (
    modifier: Modifier,
    focusMap: Map<UserActionKey, FocusRequester>,
    onFocus: (UserActionKey) -> Unit,
    onPauseAutoHide: (Boolean) -> Unit
) -> Unit

val EmptyUserActionContent: UserActionContent = { _, _, _, _ -> }