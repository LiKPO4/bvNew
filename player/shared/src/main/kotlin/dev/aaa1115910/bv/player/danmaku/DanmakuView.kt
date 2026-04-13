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
import dev.aaa1115910.bv.player.danmaku.model.Danmaku

class DanmakuView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    private val player = DanmakuPlayer(this)

    private var positionProvider: (() -> Long)? = null
    private var isPlayingProvider: (() -> Boolean)? = null
    private var playbackSpeedProvider: (() -> Float)? = null
    private var configProvider: (() -> DanmakuConfig)? = null

    private var lastConfig: DanmakuConfig? = null
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

    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }

    fun setPositionProvider(provider: () -> Long) { positionProvider = provider }
    fun setIsPlayingProvider(provider: () -> Boolean) { isPlayingProvider = provider }
    fun setPlaybackSpeedProvider(provider: () -> Float) { playbackSpeedProvider = provider }
    fun setConfigProvider(provider: () -> DanmakuConfig) { configProvider = provider }

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
        val cfg = configProvider?.invoke() ?: defaultConfig()
        if (cfg != lastConfig) { lastConfig = cfg; player.updateConfig(cfg) }

        updateViewportIfNeeded()
        if (!cfg.enabled) {
            player.draw(canvas, 0L, false, 1f, cfg); return
        }

        val posProvider = positionProvider ?: return
        val rawPos = posProvider()
        val now = SystemClock.uptimeMillis()
        if (lastPositionChangeUptimeMs == 0L) lastPositionChangeUptimeMs = now
        if (rawPos != lastRawPositionMs) lastPositionChangeUptimeMs = now
        lastRawPositionMs = rawPos

        val isPlaying = runCatching { isPlayingProvider?.invoke() }.getOrNull()
            ?: (now - lastPositionChangeUptimeMs < STOP_WHEN_IDLE_MS)
        val speed = runCatching { playbackSpeedProvider?.invoke() }.getOrNull()
            ?.takeIf { it.isFinite() && it > 0f } ?: 1f

        // Apply mask: hardware layer provides offscreen compositing for DstIn blending
        val mask = maskFrame
        val maskBitmap = if (mask != null) getOrBuildMaskBitmap(mask) else null
        if (maskBitmap != null) {
            val areaRatio = cfg.area.takeIf { it > 0f } ?: 1f
            player.draw(canvas, rawPos, isPlaying, speed, cfg)
            drawMaskBitmap(canvas, maskBitmap, videoAspectRatio, areaRatio)
        } else {
            player.draw(canvas, rawPos, isPlaying, speed, cfg)
        }
    }

    private fun getOrBuildMaskBitmap(frame: DanmakuMaskFrame): Bitmap? {
        if (frame === cachedMaskFrame && cachedMaskBitmap != null) return cachedMaskBitmap
        cachedMaskBitmap?.recycle()
        cachedMaskFrame = frame
        cachedMaskBitmap = buildMaskBitmap(frame)
        return cachedMaskBitmap
    }

    private fun buildMaskBitmap(frame: DanmakuMaskFrame): Bitmap? {
        return runCatching {
            when (frame) {
                is DanmakuWebMaskFrame -> buildWebMaskBitmap(frame)
                is DanmakuMobMaskFrame -> buildMobMaskBitmap(frame)
            }
        }.getOrNull()
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
    private fun drawMaskBitmap(canvas: Canvas, bitmap: Bitmap, videoAspect: Float, areaRatio: Float) {
        val screenW = width.toFloat()
        val screenH = height.toFloat()
        if (screenW <= 0f || screenH <= 0f) return
        val screenAspect = screenW / screenH

        val dstW: Float
        val dstH: Float
        val offsetX: Float
        val offsetY: Float

        val ratio = videoAspect.takeIf { it > 0f } ?: (16f / 9f)
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

        val dst = Rect(offsetX.toInt(), offsetY.toInt(), (offsetX + dstW).toInt(), (offsetY + dstH).toInt())
        canvas.drawBitmap(bitmap, null, dst, maskPaint)
    }

    private fun updateViewportIfNeeded() {
        val w = width.coerceAtLeast(0); val h = height.coerceAtLeast(0)
        val top = viewportTopInsetPx; val bottom = viewportBottomInsetPx
        if (w == lastViewportW && h == lastViewportH && top == lastViewportTopInset && bottom == lastViewportBottomInset) return
        lastViewportW = w; lastViewportH = h; lastViewportTopInset = top; lastViewportBottomInset = bottom
        player.onViewportChanged(w, h, top, bottom)
    }

    private fun dp(v: Float): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics).toInt()

    private fun defaultConfig(): DanmakuConfig = DanmakuConfig()

    private companion object {
        const val STOP_WHEN_IDLE_MS = 700L
    }
}
