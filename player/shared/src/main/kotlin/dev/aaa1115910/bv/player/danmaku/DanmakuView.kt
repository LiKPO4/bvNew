package dev.aaa1115910.bv.player.danmaku

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.os.SystemClock
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.caverock.androidsvg.SVG
import dev.aaa1115910.biliapi.entity.danmaku.DanmakuMaskFrame
import dev.aaa1115910.biliapi.entity.danmaku.DanmakuMobMaskFrame
import dev.aaa1115910.biliapi.entity.danmaku.DanmakuWebMaskFrame
import dev.aaa1115910.bv.player.entity.VideoAspectRatio
import dev.aaa1115910.bv.player.danmaku.model.Danmaku

class DanmakuView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val player = DanmakuPlayer(this)
    private var currentLayerType: Int = LAYER_TYPE_NONE

    private var positionProvider: (() -> Long)? = null
    private var isPlayingProvider: (() -> Boolean)? = null
    private var playbackSpeedProvider: (() -> Float)? = null
    private var config: DanmakuConfig = DEFAULT_CONFIG
    private var lastRawPositionMs: Long = 0L
    private var lastPositionChangeUptimeMs: Long = 0L

    private val viewportTopInsetPx: Int = dp(2f)
    private val viewportBottomInsetPx: Int = dp(2f)
    private var lastViewportW: Int = 0
    private var lastViewportH: Int = 0
    private var lastViewportTopInset: Int = 0
    private var lastViewportBottomInset: Int = 0

    @Volatile private var maskFrame: DanmakuMaskFrame? = null
    private var cachedMaskFrame: DanmakuMaskFrame? = null
    private var cachedMaskBitmap: Bitmap? = null
    @Volatile private var videoAspectRatio: Float = 0f
    @Volatile private var videoAspectRatioType: VideoAspectRatio = VideoAspectRatio.Default

    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }
    private val maskDstRect = Rect()

    fun setPositionProvider(provider: () -> Long) { positionProvider = provider }
    fun setIsPlayingProvider(provider: () -> Boolean) { isPlayingProvider = provider }
    fun setPlaybackSpeedProvider(provider: () -> Float) { playbackSpeedProvider = provider }
    fun setConfig(config: DanmakuConfig) {
        if (this.config == config) return
        this.config = config
        player.updateConfig(config)
        postInvalidateOnAnimation()
    }

    fun setDanmakus(list: List<Danmaku>) { player.setDanmakus(list); invalidate() }
    fun appendDanmakus(list: List<Danmaku>, maxItems: Int = 0, alreadySorted: Boolean = false) {
        if (list.isEmpty()) return
        player.appendDanmakus(list, maxItems, alreadySorted); invalidate()
    }
    fun trimToTimeRange(minPositionMs: Long, maxPositionMs: Long) { player.trimToTimeRange(minPositionMs, maxPositionMs); invalidate() }
    fun notifySeek(positionMs: Long) {
        player.seekTo(positionMs)
        lastRawPositionMs = positionMs
        lastPositionChangeUptimeMs = SystemClock.uptimeMillis()
        invalidate()
    }

    fun play() {
        invalidate()
    }

    /** 显式释放资源。可多次调用，幂等。 */
    fun release() {
        player.release()
        cachedMaskBitmap?.recycle()
        cachedMaskBitmap = null
        cachedMaskFrame = null
    }

    fun setMaskFrame(frame: DanmakuMaskFrame?) { maskFrame = frame }
    fun setVideoAspectRatio(ratio: Float) { videoAspectRatio = ratio }
    fun setVideoAspectRatioType(type: VideoAspectRatio) { videoAspectRatioType = type }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateViewportIfNeeded()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        player.release()
        cachedMaskBitmap?.recycle()
        cachedMaskBitmap = null
        cachedMaskFrame = null
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateViewportIfNeeded()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        updateViewportIfNeeded()
        if (!config.enabled) {
            player.draw(canvas, 0L, false, 1f, config); return
        }

        val posProvider = positionProvider ?: return
        val rawPos = posProvider()
        val now = SystemClock.uptimeMillis()
        if (lastPositionChangeUptimeMs == 0L) lastPositionChangeUptimeMs = now
        if (rawPos != lastRawPositionMs) lastPositionChangeUptimeMs = now
        lastRawPositionMs = rawPos

        val fallbackPlaying = now - lastPositionChangeUptimeMs < STOP_WHEN_IDLE_MS
        val playingProvider = isPlayingProvider
        val isPlaying = if (playingProvider != null) {
            try { playingProvider() } catch (_: Exception) { fallbackPlaying }
        } else {
            fallbackPlaying
        }
        val speedProvider = playbackSpeedProvider
        val speed = if (speedProvider != null) {
            val candidate = try { speedProvider() } catch (_: Exception) { Float.NaN }
            if (candidate.isFinite() && candidate > 0f) candidate else 1f
        } else {
            1f
        }

        // DstIn blending for mask requires an offscreen buffer — use hardware layer only when needed.
        val mask = maskFrame
        val needsHwLayer = mask != null
        val desiredLayerType = if (needsHwLayer) LAYER_TYPE_HARDWARE else LAYER_TYPE_NONE
        if (desiredLayerType != currentLayerType) {
            setLayerType(desiredLayerType, null)
            currentLayerType = desiredLayerType
        }

        player.draw(canvas, rawPos, isPlaying, speed, config)

        if (mask != null) {
            val maskBitmap = getOrBuildMaskBitmap(mask)
            if (maskBitmap != null) drawMaskBitmap(canvas, maskBitmap, videoAspectRatio, videoAspectRatioType)
        }
    }

    private fun getOrBuildMaskBitmap(frame: DanmakuMaskFrame): Bitmap? {
        if (frame == cachedMaskFrame && cachedMaskBitmap != null) return cachedMaskBitmap
        cachedMaskBitmap?.recycle()
        cachedMaskFrame = frame
        cachedMaskBitmap = try {
            when (frame) {
                is DanmakuWebMaskFrame -> buildWebMaskBitmap(frame)
                is DanmakuMobMaskFrame -> buildMobMaskBitmap(frame)
            }
        } catch (_: Exception) { null }
        return cachedMaskBitmap
    }

    /** Web 蒙版：使用 androidsvg 库解析完整 SVG，渲染到 Bitmap */
    private fun buildWebMaskBitmap(frame: DanmakuWebMaskFrame): Bitmap? {
        val svg = frame.svg
        if (svg.isBlank()) return null
        val svgObj = SVG.getFromString(svg)
        val svgW = svgObj.documentWidth.toInt()
        val svgH = svgObj.documentHeight.toInt()
        if (svgW <= 0 || svgH <= 0) return null
        val bitmap = Bitmap.createBitmap(svgW, svgH, Bitmap.Config.ARGB_8888)
        svgObj.renderToCanvas(Canvas(bitmap))
        return bitmap
    }

    /** Mob 蒙版：40×180 1bpp 二值图，批量 setPixels 写入 */
    private fun buildMobMaskBitmap(frame: DanmakuMobMaskFrame): Bitmap? {
        val w = frame.width
        val h = frame.height
        if (w <= 0 || h <= 0) return null
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h) { i ->
            val byteIndex = i / 8
            val bitOffset = 7 - (i % 8)
            val bit = (frame.image[byteIndex].toInt() shr bitOffset) and 1
            if (bit == 0) Color.TRANSPARENT else Color.BLACK
        }
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        return bitmap
    }

    /**
     * 将蒙版 Bitmap 以 DstIn 模式绘制到 canvas 上，正确处理视频 letterbox/pillarbox。
     * 逻辑与 DanmakuMaskModifiers.bitmapMask 一致。
     */
    private fun drawMaskBitmap(
        canvas: Canvas,
        bitmap: Bitmap,
        videoAspect: Float,
        aspectType: VideoAspectRatio,
    ) {
        val screenW = width.toFloat()
        val screenH = height.toFloat()
        if (screenW <= 0f || screenH <= 0f) return
        val screenAspect = screenW / screenH

        val dstW: Float
        val dstH: Float
        val offsetX: Float
        val offsetY: Float

        val ratio = if (videoAspect > 0f) videoAspect else 16f / 9f
        when (aspectType) {
            VideoAspectRatio.Stretch -> {
                dstW = screenW
                dstH = screenH
                offsetX = 0f
                offsetY = 0f
            }

            VideoAspectRatio.EqualWidth -> {
                dstW = screenW
                dstH = dstW / ratio
                offsetX = 0f
                offsetY = (screenH - dstH) / 2f
            }

            VideoAspectRatio.EqualHeight -> {
                dstH = screenH
                dstW = dstH * ratio
                offsetY = 0f
                offsetX = (screenW - dstW) / 2f
            }

            else -> {
                if (ratio > screenAspect) {
                    dstW = screenW
                    dstH = dstW / ratio
                    offsetX = 0f
                    offsetY = (screenH - dstH) / 2f
                } else {
                    dstH = screenH
                    dstW = dstH * ratio
                    offsetY = 0f
                    offsetX = (screenW - dstW) / 2f
                }
            }
        }

        maskDstRect.set(offsetX.toInt(), offsetY.toInt(), (offsetX + dstW).toInt(), (offsetY + dstH).toInt())
        canvas.drawBitmap(bitmap, null, maskDstRect, maskPaint)
    }

    private fun updateViewportIfNeeded() {
        val w = width.coerceAtLeast(0); val h = height.coerceAtLeast(0)
        val top = viewportTopInsetPx; val bottom = viewportBottomInsetPx
        if (w == lastViewportW && h == lastViewportH && top == lastViewportTopInset && bottom == lastViewportBottomInset) return
        lastViewportW = w; lastViewportH = h; lastViewportTopInset = top; lastViewportBottomInset = bottom
        player.onViewportChanged(w, h, top, bottom)
    }

    private fun dp(v: Float): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics).toInt()

    private companion object {
        private val DEFAULT_CONFIG = DanmakuConfig()
        const val STOP_WHEN_IDLE_MS = 700L
    }
}
