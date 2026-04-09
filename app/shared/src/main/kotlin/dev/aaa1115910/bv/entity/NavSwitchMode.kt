package dev.aaa1115910.bv.entity

import android.content.Context
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.util.stringRes

enum class NavSwitchMode(private val strRes: Int) {
    Auto(R.string.nav_switch_mode_auto),
    Confirm(R.string.nav_switch_mode_confirm);

    fun getDisplayName(context: Context): String = strRes.stringRes(context)
}
