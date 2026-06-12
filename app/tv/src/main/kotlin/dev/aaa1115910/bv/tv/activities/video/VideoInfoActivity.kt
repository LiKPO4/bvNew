package dev.aaa1115910.bv.tv.activities.video

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.tv.screens.VideoInfoScreen
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.util.Prefs
import java.lang.ref.WeakReference
import java.util.LinkedList

class VideoInfoActivity : ComponentActivity() {
    companion object {
        // 使用WeakReference防止内存泄漏，避免持有已销毁Activity的强引用
        private val activityQueue = LinkedList<WeakReference<VideoInfoActivity>>()

        fun actionStart(
            context: Context,
            aid: Long,
            cid: Long? = null,
            fromSeason: Boolean = false,
            fromPlayer: Boolean = false,
            forceShowDetail: Boolean = false,
            proxyArea: ProxyArea = ProxyArea.MainLand
        ) {
            context.startActivity(
                Intent(context, VideoInfoActivity::class.java).apply {
                    putExtra("aid", aid)
                    putExtra("cid", cid)
                    putExtra("fromSeason", fromSeason)
                    putExtra("fromPlayer", fromPlayer)
                    putExtra("forceShowDetail", forceShowDetail)
                    putExtra("proxy_area", proxyArea.ordinal)
                }
            )
        }
    }

    // 以 aid + cid 作为唯一键标识一个视频页面
    private val videoKey: String by lazy {
        val aid = intent.getLongExtra("aid", 0L)
        val cid = intent.getLongExtra("cid", 0L)
        "${aid}_${cid}"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val fromPlayer = intent.getBooleanExtra("fromPlayer", false)
        val shouldRecordInHistoryQueue = !fromPlayer || Prefs.videoInfoHistoryIncludeFromPlayer

        // 将当前活动加入队列
        if (shouldRecordInHistoryQueue) {
            synchronized(activityQueue) {
                val maxVideoInfoScreens = if (Prefs.showUGCVideoInfo) {
                    Prefs.ugcVideoInfoHistoryCount.coerceAtLeast(1)
                } else {
                    Prefs.ugcVideoInfoHistoryCount.coerceAtLeast(1) + 1
                }

                // 清理队列中的无效引用 - 这步是必要的
                // 1. 确保队列大小计算准确，防止误判是否达到历史留存上限
                // 2. 处理可能未正常触发onDestroy的情况（如系统回收、应用崩溃等）
                // 3. 防止队列中累积无效引用导致内存泄漏
                val iterator = activityQueue.iterator()
                while (iterator.hasNext()) {
                    val activityRef = iterator.next()
                    val activity = activityRef.get()
                    if (activity == null || activity.isFinishing) {
                        iterator.remove()
                    }
                }

                // 如果队列中已存在相同 aid+cid 的活动，移除旧页面
                val dupIterator = activityQueue.iterator()
                while (dupIterator.hasNext()) {
                    val activityRef = dupIterator.next()
                    val activity = activityRef.get()
                    if (activity != null && activity.videoKey == this.videoKey) {
                        dupIterator.remove()
                        activity.runOnUiThread { activity.finish() }
                        break
                    }
                }

                // 添加当前活动到队列
                activityQueue.add(WeakReference(this))

                // 如果队列超过了最大限制，关闭最早的活动
                if (activityQueue.size > maxVideoInfoScreens) {
                    // 移除最早的活动引用
                    val oldestActivityRef = activityQueue.removeFirst()
                    val oldestActivity = oldestActivityRef.get()
                    // 确保在主线程调用finish()
                    oldestActivity?.runOnUiThread {
                        oldestActivity.finish()
                    }
                }
            }
        }

        setContent {
            BVTheme(
                forceDark = true
            ) {
                VideoInfoScreen()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // 当活动被销毁时，从队列中移除该Activity的引用
        synchronized(activityQueue) {
            val iterator = activityQueue.iterator()
            while (iterator.hasNext()) {
                val ref = iterator.next()
                if (ref.get() == this || ref.get() == null) {
                    iterator.remove()
                }
            }
        }
    }
}
