package com.kuaishou.akdanmaku.ecs.component.filter

import android.util.Log
import com.kuaishou.akdanmaku.DanmakuConfig
import com.kuaishou.akdanmaku.data.DanmakuItem
import com.kuaishou.akdanmaku.ecs.DanmakuEngine
import com.kuaishou.akdanmaku.utils.DanmakuTimer

/**
 * 弹幕等级过滤器，过滤等级低于 [minLevel] 的弹幕
 */
class LevelFilter(var minLevel: Int = 0) : DanmakuDataFilter(DanmakuFilters.FILTER_TYPE_LEVEL) {
  override fun filter(item: DanmakuItem, timer: DanmakuTimer, config: DanmakuConfig): Boolean {
    val filtered = minLevel > 0 && item.data.level < minLevel
//    if (filtered) {
//      Log.d(DanmakuEngine.TAG, "[LevelFilter] filtered danmaku: level=${item.data.level}, minLevel=$minLevel, text=${item.data.content}")
//    }
    return filtered
  }
}
