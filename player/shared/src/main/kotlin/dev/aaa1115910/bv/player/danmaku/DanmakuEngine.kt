package dev.aaa1115910.bv.player.danmaku

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import dev.aaa1115910.bv.player.danmaku.model.Danmaku
import dev.aaa1115910.bv.player.danmaku.model.DanmakuCacheState
import dev.aaa1115910.bv.player.danmaku.model.DanmakuItem
import dev.aaa1115910.bv.player.danmaku.model.DanmakuKind
import dev.aaa1115910.bv.player.danmaku.model.RenderSnapshot
import java.util.Arrays
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal class DanmakuEngine(
    private val displayMetrics: DisplayMetrics,
    private val cacheManager: CacheManager,
) {
    private val density = displayMetrics.density.takeIf { it.isFinite() && it > 0f } ?: 1f

    // Data (action thread)
    private val actionStateLock = Any()
    private var allItems: MutableList<DanmakuItem> = mutableListOf()
    private var items: MutableList<DanmakuItem> = mutableListOf()
    private var index: Int = 0
    private val active = ArrayList<DanmakuItem>(64)
    private val pending = ArrayDeque<PendingSpawn>()
    private var lastNowMs: Double = 0.0

    // Viewport / Config
    @Volatile var viewportWidth: Int = 0; private set
    @Volatile var viewportHeight: Int = 0; private set
    @Volatile var viewportTopInsetPx: Int = 0; private set
    @Volatile var viewportBottomInsetPx: Int = 0; private set

    @Volatile var config: DanmakuConfig = DanmakuConfig()
    @Volatile private var textSizePx: Float = sp(18f)
    @Volatile private var strokeWidthPx: Float = 3f
    @Volatile private var outlinePadPx: Float = 1.5f
    @Volatile private var cacheStyleGeneration: Int = 0

    // Time (main → action)
    @Volatile private var currentPositionMs: Double = 0.0
    @Volatile private var currentUiFrameId: Int = 0

    // Snapshot double buffer
    private val snapshotA = RenderSnapshot()
    private val snapshotB = RenderSnapshot()
    @Volatile private var latestSnapshot: RenderSnapshot = snapshotA

    // FPS stats (action thread)
    private var actFrameCount: Int = 0
    private var actDroppedFrames: Int = 0
    private var actLastLogNanos: Long = System.nanoTime()
    private var actLastFrameNanos: Long = 0L
    private val actFrameDeadlineNanos: Long = 16_666_667L // ~60fps
    private var actStartNanos: Long = 0L
    private var actDurationTotalNanos: Long = 0L
    private var actDurationMaxNanos: Long = 0L
    private var actDurationSamples: LongArray = LongArray(128)
    private var actDurationSampleCount: Int = 0

    // Layout scratch (action thread)
    private val actionFontMetrics = Paint.FontMetrics()
    private val actionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.DEFAULT_BOLD }
    private var laneLastScroll: Array<DanmakuItem?> = emptyArray()
    private var laneLastScrollTail = FloatArray(0)
    private var laneLastTop: Array<DanmakuItem?> = emptyArray()
    private var laneLastBottom: Array<DanmakuItem?> = emptyArray()

    // Cached layout results (action thread, recomputed only when viewport/config changes)
    @Volatile private var layoutDirty: Boolean = true
    private var cachedLaneCount: Int = 1
    private var cachedTopFixedLaneCount: Int = 1
    private var cachedBottomFixedLaneCount: Int = 1
    private var cachedLaneHeight: Float = 18f
    private var cachedTextBoxHeight: Float = 18f
    private var cachedUsableHeight: Int = 0
    private var cachedTopFixedUsableHeight: Int = 0
    private var cachedBottomFixedUsableHeight: Int = 0
    private var cachedTopInset: Int = 0
    private var cachedMarginPx: Float = 12f

    // Draw paints (main thread)
    private val drawFontMetrics = Paint.FontMetrics()
    private val drawFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.DEFAULT_BOLD; isSubpixelText = true }
    private val drawStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.DEFAULT_BOLD; style = Paint.Style.STROKE; isSubpixelText = true }
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }

    fun updateViewport(width: Int, height: Int, topInsetPx: Int, bottomInsetPx: Int) {
        viewportWidth = width.coerceAtLeast(0)
        viewportHeight = height.coerceAtLeast(0)
        viewportTopInsetPx = topInsetPx.coerceAtLeast(0)
        viewportBottomInsetPx = bottomInsetPx.coerceAtLeast(0)
        layoutDirty = true
    }

    fun updateConfig(newConfig: DanmakuConfig) {
        synchronized(actionStateLock) {
            val old = config
            config = newConfig
            val tsPx = sp(newConfig.textSizeSp).coerceAtLeast(1f)
            val newStroke = newConfig.strokeWidthPx.coerceAtLeast(0).toFloat()
            val newTypeface = newConfig.fontWeight.typeface

            val styleChanged = textSizePx != tsPx || strokeWidthPx != newStroke || actionPaint.typeface != newTypeface || old.textSizeScale != newConfig.textSizeScale
            textSizePx = tsPx
            strokeWidthPx = newStroke
            outlinePadPx = max(1f, newStroke / 2f)
            actionPaint.textSize = tsPx
            if (actionPaint.typeface != newTypeface) actionPaint.typeface = newTypeface

            if (styleChanged) {
                cacheStyleGeneration++
                val releaseAt = currentUiFrameId + 1
                for (a in active) {
                    val bmp = a.cacheBitmap
                    if (bmp != null) { cacheManager.enqueueRelease(bmp, releaseAt); a.cacheBitmap = null }
                    a.cacheState = DanmakuCacheState.Init
                    a.cacheGeneration = -1
                }
            }

            // Rebuild filter if changed
            val filterChanged = old.allowScroll != newConfig.allowScroll || old.allowTop != newConfig.allowTop ||
                old.allowBottom != newConfig.allowBottom || old.minLevel != newConfig.minLevel
            if (filterChanged) {
                rebuildFilteredItems()
            }
            layoutDirty = true
        }
    }

    fun stepTime(positionMs: Double, uiFrameId: Int) {
        currentPositionMs = positionMs.coerceAtLeast(0.0)
        currentUiFrameId = uiFrameId
    }

    fun currentPositionMs(): Double = currentPositionMs

    fun drainReleasedBitmaps(uiFrameId: Int) {
        cacheManager.drainReleasedBitmaps(uiFrameId)
    }

    fun act() {
        try {
            synchronized(actionStateLock) {
                if (DanmakuLogStats.logEnabled) {
                    // FPS tracking
                    actStartNanos = System.nanoTime()
                    actFrameCount++
                    if (actLastFrameNanos > 0L) {
                        val frameDelta = actStartNanos - actLastFrameNanos
                        if (frameDelta > actFrameDeadlineNanos * 2) actDroppedFrames++
                    }
                    actLastFrameNanos = actStartNanos
                }

                val cfg = config
                if (!cfg.enabled) { clearActives(); publishEmptySnapshot(); return }
                val width = viewportWidth; val height = viewportHeight
                if (width <= 0 || height <= 0) { clearActives(); publishEmptySnapshot(); return }

                val outlinePad = outlinePadPx
                val rawNowMs = currentPositionMs
                val nowMs = if (rawNowMs >= lastNowMs) rawNowMs else lastNowMs
                lastNowMs = nowMs

                // Recompute layout only when viewport or config changed
                if (layoutDirty) {
                    val areaFraction = cfg.area.coerceIn(0f, 1f)
                    cachedTopInset = viewportTopInsetPx.coerceIn(0, height)
                    val bottomInset = ((1f - areaFraction) * viewportBottomInsetPx).toInt().coerceIn(0, height - cachedTopInset)
                    val availableHeight = (height - cachedTopInset - bottomInset).coerceAtLeast(0)

                    val scaleFactor = cfg.textSizeScale.coerceIn(25, 200) / 100f
                    val layoutTextSizePx = textSizePx * scaleFactor
                    actionPaint.textSize = layoutTextSizePx
                    actionPaint.getFontMetrics(actionFontMetrics)
                    cachedTextBoxHeight = (actionFontMetrics.descent - actionFontMetrics.ascent) + outlinePad * 2f
                    val baseLaneHeight = max(18f, cachedTextBoxHeight * 1.15f)
                    cachedLaneHeight = max(cachedTextBoxHeight, baseLaneHeight * cfg.laneDensity.laneHeightFactor)
                    cachedUsableHeight = (availableHeight * areaFraction).toInt().coerceAtLeast(0)
                    cachedLaneCount = max(1, (cachedUsableHeight / cachedLaneHeight).toInt())

                    // Top fixed: area capped at 0.8
                    val topFixedAreaFraction = min(areaFraction, 0.8f)
                    cachedTopFixedUsableHeight = (availableHeight * topFixedAreaFraction).toInt().coerceAtLeast(0)
                    cachedTopFixedLaneCount = max(1, (cachedTopFixedUsableHeight / cachedLaneHeight).toInt())

                    // Bottom fixed: fixed 20% of screen height, always at bottom
                    cachedBottomFixedUsableHeight = (height * 0.2f).toInt().coerceAtLeast(0)
                    cachedBottomFixedLaneCount = max(1, (cachedBottomFixedUsableHeight / cachedLaneHeight).toInt())

                    cachedMarginPx = max(12f, (layoutTextSizePx + outlinePad * 2f) * 0.6f)
                    layoutDirty = false
                }
                val topInset = cachedTopInset
                val textBoxHeight = cachedTextBoxHeight
                val laneHeight = cachedLaneHeight
                val usableHeight = cachedUsableHeight
                val laneCount = cachedLaneCount
                val topFixedLaneCount = cachedTopFixedLaneCount
                val topFixedUsableHeight = cachedTopFixedUsableHeight
                val bottomFixedLaneCount = cachedBottomFixedLaneCount
                val bottomFixedUsableHeight = cachedBottomFixedUsableHeight

                val durationMul = cfg.durationMultiplier.coerceIn(0.2f, 5f)
                val rollingDurationMs = (DEFAULT_ROLLING_DURATION_MS * durationMul).toInt().coerceIn(MIN_ROLLING_DURATION_MS, MAX_ROLLING_DURATION_MS)
                val fixedDurationMs = (FIXED_DURATION_MS * durationMul).toInt().coerceIn(MIN_ROLLING_DURATION_MS, MAX_ROLLING_DURATION_MS)

                pruneExpired(width, nowMs)
                skipOld(nowMs, rollingDurationMs)
                dropIfLagging(nowMs)

                val maxLaneCount = maxOf(laneCount, topFixedLaneCount, bottomFixedLaneCount)
                ensureLaneBuffers(maxLaneCount)
                Arrays.fill(laneLastScroll, 0, laneCount, null)
                Arrays.fill(laneLastScrollTail, 0, laneCount, Float.NEGATIVE_INFINITY)
                Arrays.fill(laneLastTop, 0, topFixedLaneCount, null)
                Arrays.fill(laneLastBottom, 0, bottomFixedLaneCount, null)

                for (a in active) {
                    when (a.kind) {
                        DanmakuKind.SCROLL -> { if (a.lane !in 0 until laneCount) continue; val cur = laneLastScroll[a.lane]; if (cur == null || a.startTimeMs > cur.startTimeMs) laneLastScroll[a.lane] = a }
                        DanmakuKind.TOP -> { if (a.lane !in 0 until topFixedLaneCount) continue; val cur = laneLastTop[a.lane]; if (cur == null || a.startTimeMs > cur.startTimeMs) laneLastTop[a.lane] = a }
                        DanmakuKind.BOTTOM -> { if (a.lane !in 0 until bottomFixedLaneCount) continue; val cur = laneLastBottom[a.lane]; if (cur == null || a.startTimeMs > cur.startTimeMs) laneLastBottom[a.lane] = a }
                    }
                }
                for (lane in 0 until laneCount) {
                    val a = laneLastScroll[lane] ?: continue
                    laneLastScrollTail[lane] = scrollX(width, nowMs, a.startTimeMs, a.pxPerMs) + a.textWidthPx
                }

                val marginPx = cachedMarginPx

                // Retry pending
                if (pending.isNotEmpty()) {
                    val pendingCount = pending.size
                    var processed = 0
                    var i = 0
                    while (i < pendingCount && pending.isNotEmpty()) {
                        val p = pending.removeFirst(); i++
                        if (p.nextTryMs > nowMs) { pending.addLast(p); continue }
                        if (processed >= MAX_PENDING_RETRY_PER_FRAME) { pending.addLast(p); continue }
                        processed++
                        val ok = when (p.kind) {
                            DanmakuKind.SCROLL -> trySpawnScroll(p.item, p.textWidthPx, width, laneCount, rollingDurationMs, marginPx, nowMs)
                            DanmakuKind.TOP -> trySpawnFixed(p.kind, p.item, p.textWidthPx, topFixedLaneCount, fixedDurationMs, nowMs)
                            DanmakuKind.BOTTOM -> trySpawnFixed(p.kind, p.item, p.textWidthPx, bottomFixedLaneCount, fixedDurationMs, nowMs)
                        }
                        if (ok) continue
                        if (nowMs - p.firstTryMs <= MAX_DELAY_MS) { p.nextTryMs = (nowMs + DELAY_STEP_MS).toInt(); pending.addLast(p) }
                    }
                }

                // Spawn new
                var spawnAttempts = 0
                while (index < items.size && items[index].timeMs() <= nowMs) {
                    if (spawnAttempts >= MAX_SPAWN_PER_FRAME) break
                    val item = items[index]; index++; spawnAttempts++
                    if (item.data.text.isBlank()) continue
                    val textWidth = measureTextWidth(item, outlinePad, cfg)
                    val kind = kindOf(item.data)
                    val ok = when (kind) {
                        DanmakuKind.SCROLL -> trySpawnScroll(item, textWidth, width, laneCount, rollingDurationMs, marginPx, nowMs)
                        DanmakuKind.TOP -> trySpawnFixed(kind, item, textWidth, topFixedLaneCount, fixedDurationMs, nowMs)
                        DanmakuKind.BOTTOM -> trySpawnFixed(kind, item, textWidth, bottomFixedLaneCount, fixedDurationMs, nowMs)
                    }
                    if (!ok) enqueuePending(kind, item, textWidth, nowMs)
                }

                // Request cache builds
                val style = CacheStyle(textSizePx, cfg.textSizeScale, cfg.fontWeight, strokeWidthPx, outlinePad, cacheStyleGeneration)
                val releaseAtFrameId = currentUiFrameId + 1
                var requested = 0
                if (cacheManager.queueDepth() < MAX_CACHE_QUEUE_DEPTH) {
                    for (a in active) {
                        if (requested >= MAX_CACHE_REQUESTS_PER_FRAME) break
                        val bmp = a.cacheBitmap
                        if (bmp != null && !bmp.isRecycled && a.cacheGeneration == style.generation) continue
                        if (a.cacheState == DanmakuCacheState.Rendering) continue
                        a.cacheState = DanmakuCacheState.Rendering
                        cacheManager.requestBuildCache(a, a.textWidthPx, style, releaseAtFrameId)
                        requested++
                        if (cacheManager.queueDepth() >= MAX_CACHE_QUEUE_DEPTH) break
                    }
                }

                // Publish snapshot
                val maxYTop = (topInset + usableHeight - textBoxHeight).toFloat().coerceAtLeast(topInset.toFloat())
                val topFixedMaxYTop = (topInset + topFixedUsableHeight - textBoxHeight).toFloat().coerceAtLeast(topInset.toFloat())
                val bottomFixedBaseYTop = (height - viewportBottomInsetPx - textBoxHeight).toFloat()
                val bottomFixedMinYTop = (height - viewportBottomInsetPx - bottomFixedUsableHeight).toFloat().coerceAtLeast(0f)
                val out = writableSnapshot()
                out.ensureCapacity(active.size)
                out.positionMs = nowMs
                out.count = 0
                out.pendingCount = pending.size
                for (a in active) {
                    val iOut = out.count
                    val x = when (a.kind) {
                        DanmakuKind.SCROLL -> scrollX(width, nowMs, a.startTimeMs, a.pxPerMs)
                        else -> centerX(width, a.textWidthPx)
                    }
                    val yTop = when (a.kind) {
                        DanmakuKind.SCROLL -> (topInset.toFloat() + laneHeight * a.lane).coerceAtMost(maxYTop)
                        DanmakuKind.TOP -> (topInset.toFloat() + laneHeight * a.lane).coerceAtMost(topFixedMaxYTop)
                        DanmakuKind.BOTTOM -> (bottomFixedBaseYTop - laneHeight * a.lane).coerceAtLeast(bottomFixedMinYTop)
                    }
                    out.items[iOut] = a; out.x[iOut] = x; out.yTop[iOut] = yTop
                    out.textWidth[iOut] = a.textWidthPx; out.count = iOut + 1
                }
                latestSnapshot = out
            }
        } finally {
            if (DanmakuLogStats.logEnabled) {
                val durationNanos = (System.nanoTime() - actStartNanos).coerceAtLeast(0L)
                recordActDuration(durationNanos)

                val logNow = System.nanoTime()
                if (logNow - actLastLogNanos >= 1_000_000_000L) {
                    val elapsed = (logNow - actLastLogNanos) / 1_000_000_000.0
                    val fps = actFrameCount / elapsed
                    Log.d(
                        TAG,
                        "[Action] fps=%.1f  frames=%d  dropped=%d  %s  active=%d  pending=%d  cacheQ=%d  mem=%s".format(
                            fps,
                            actFrameCount,
                            actDroppedFrames,
                            actionDurationSummary(),
                            active.size,
                            pending.size,
                            cacheManager.queueDepth(),
                            DanmakuLogStats.memoryUsageSummary(),
                        )
                    )
                    actFrameCount = 0
                    actDroppedFrames = 0
                    actLastLogNanos = logNow
                    resetActionDurationStats()
                }
            }
        }
    }

    private fun recordActDuration(durationNanos: Long) {
        actDurationTotalNanos += durationNanos
        if (durationNanos > actDurationMaxNanos) actDurationMaxNanos = durationNanos
        if (actDurationSampleCount == actDurationSamples.size) {
            actDurationSamples = actDurationSamples.copyOf(actDurationSamples.size * 2)
        }
        actDurationSamples[actDurationSampleCount++] = durationNanos
    }

    private fun actionDurationSummary(): String {
        if (actDurationSampleCount == 0) return "actMs(avg/p50/p95/max)=0.00/0.00/0.00/0.00"

        val samples = actDurationSamples.copyOf(actDurationSampleCount)
        samples.sort()
        // act 耗时的平均值、中位数、95 分位和最大值。单位是毫秒
        // 如果 actMs 的 p95/max 明显高于 avg，说明 action 线程存在长尾抖动。
        val avgMs = actDurationTotalNanos.toDouble() / actDurationSampleCount / 1_000_000.0
        val p50Ms = percentileNanos(samples, 0.50).toDouble() / 1_000_000.0 // 50% 的样本都不超过这个值，也就是中位数。它表示“典型情况下有多快” 
        val p95Ms = percentileNanos(samples, 0.95).toDouble() / 1_000_000.0 // 95% 的样本都不超过这个值，只有最慢的 5% 会比它更大。它表示“尾部延迟”，也就是偶发慢帧、抖动、卡顿尖峰。
        val maxMs = actDurationMaxNanos.toDouble() / 1_000_000.0
        return "actMs(avg/p50/p95/max)=%.2f/%.2f/%.2f/%.2f".format(avgMs, p50Ms, p95Ms, maxMs)
    }

    private fun percentileNanos(sortedSamples: LongArray, percentile: Double): Long {
        if (sortedSamples.isEmpty()) return 0L
        val index = ceil((sortedSamples.size - 1) * percentile).toInt().coerceIn(0, sortedSamples.lastIndex)
        return sortedSamples[index]
    }

    private fun resetActionDurationStats() {
        actDurationTotalNanos = 0L
        actDurationMaxNanos = 0L
        actDurationSampleCount = 0
    }

    fun renderSnapshot(): RenderSnapshot = latestSnapshot

    fun draw(canvas: Canvas, snapshot: RenderSnapshot, config: DanmakuConfig) {
        if (!config.enabled) return
        val ts = textSizePx
        val scaleFactor = config.textSizeScale.coerceIn(25, 200) / 100f
        val drawTs = ts * scaleFactor
        if (drawFill.textSize != drawTs) { drawFill.textSize = drawTs; drawStroke.textSize = drawTs }
        val desiredTypeface = config.fontWeight.typeface
        if (drawFill.typeface != desiredTypeface) { drawFill.typeface = desiredTypeface; drawStroke.typeface = desiredTypeface }
        if (drawStroke.strokeWidth != strokeWidthPx) drawStroke.strokeWidth = strokeWidthPx

        val outlinePad = outlinePadPx
        val opacityAlpha = (config.opacity * 255f).roundToInt().coerceIn(0, 255)
        bitmapPaint.alpha = opacityAlpha
        drawFill.getFontMetrics(drawFontMetrics)
        val baselineOffset = outlinePad - drawFontMetrics.ascent
        val styleGen = cacheStyleGeneration

        for (i in 0 until snapshot.count) {
            val item = snapshot.items[i] ?: continue
            val x = snapshot.x[i]; val yTop = snapshot.yTop[i]
            val bmp = item.cacheBitmap
            if (bmp != null && !bmp.isRecycled && item.cacheGeneration == styleGen) {
                canvas.drawBitmap(bmp, x, yTop, bitmapPaint)
                continue
            }
            drawTextDirect(canvas, item, x, yTop, outlinePad, baselineOffset, opacityAlpha, config)
        }
    }

    // --- Data operations ---

    fun setDanmakus(list: List<Danmaku>) {
        synchronized(actionStateLock) {
            clearActives()
            allItems = list.sortedBy { it.positionMs }.mapTo(ArrayList(list.size)) { DanmakuItem(it) }
            rebuildFilteredItems()
            index = 0; lastNowMs = 0.0
            publishEmptySnapshot()
        }
    }

    fun appendDanmakus(list: List<Danmaku>, maxItems: Int, alreadySorted: Boolean) {
        synchronized(actionStateLock) {
            if (list.isEmpty()) return
            if (allItems.isEmpty()) { setDanmakus(list); return }
            val newItems = if (alreadySorted) list else list.sortedBy { it.positionMs }
            val lastTime = allItems.lastOrNull()?.timeMs() ?: Int.MIN_VALUE
            val appendAtEnd = (newItems.firstOrNull()?.positionMs ?: Int.MIN_VALUE) >= lastTime
            if (appendAtEnd) {
                for (d in newItems) allItems.add(DanmakuItem(d))
            } else {
                for (d in newItems) allItems.add(DanmakuItem(d))
                allItems.sortBy { it.timeMs() }
            }
            if (maxItems > 0) {
                val drop = allItems.size - maxItems
                if (drop > 0) allItems = allItems.subList(drop, allItems.size).toMutableList()
            }
            rebuildFilteredItems()
            if (appendAtEnd) {
                // New items are all after existing ones — keep actives, pending, index intact.
                index = index.coerceIn(0, items.size)
            } else {
                // Items inserted in middle — recalculate index but keep actives intact.
                val currentPos = lastNowMs.coerceAtLeast(0.0)
                index = lowerBound(currentPos)
            }
        }
    }

    fun trimToTimeRange(minTimeMs: Long, maxTimeMs: Long) {
        synchronized(actionStateLock) {
            if (allItems.isEmpty()) return
            val minI = minTimeMs.coerceAtLeast(0L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            val maxI = maxTimeMs.coerceAtLeast(0L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            if (maxI <= minI) return
            allItems.removeAll { it.timeMs() < minI || it.timeMs() >= maxI }
            rebuildFilteredItems()
            index = (index).coerceIn(0, items.size)
            if (pending.isNotEmpty()) {
                val keep = ArrayDeque<PendingSpawn>()
                while (pending.isNotEmpty()) {
                    val p = pending.removeFirst()
                    if (p.item.timeMs() in minI until maxI) keep.addLast(p)
                }
                pending.addAll(keep)
            }
        }
    }

    fun seekTo(positionMs: Double = 0.0) {
        synchronized(actionStateLock) {
            index = lowerBound(positionMs)
            clearActives(); pending.clear(); lastNowMs = positionMs
            publishEmptySnapshot()
        }
    }

    fun clear() {
        synchronized(actionStateLock) { clearActives(); pending.clear(); publishEmptySnapshot() }
    }

    fun release() {
        synchronized(actionStateLock) {
            clear()
            snapshotA.clear()
            snapshotB.clear()
            // Recycle cacheBitmaps held by all items (not just active ones)
            for (item in allItems) {
                val bmp = item.cacheBitmap
                if (bmp != null && !bmp.isRecycled) runCatching { bmp.recycle() }
                item.cacheBitmap = null
            }
            allItems = mutableListOf()
            items = mutableListOf()
        }
    }

    // --- Private helpers ---

    private fun rebuildFilteredItems() {
        val cfg = config
        items = allItems.filterTo(ArrayList(allItems.size)) { item ->
            val d = item.data
            if (d.level < cfg.minLevel) return@filterTo false
            when (d.mode) {
                Danmaku.MODE_SCROLL -> cfg.allowScroll
                Danmaku.MODE_TOP -> cfg.allowTop
                Danmaku.MODE_BOTTOM -> cfg.allowBottom
                else -> cfg.allowScroll
            }
        }
    }

    private fun kindOf(d: Danmaku): DanmakuKind = when (d.mode) {
        Danmaku.MODE_TOP -> DanmakuKind.TOP
        Danmaku.MODE_BOTTOM -> DanmakuKind.BOTTOM
        else -> DanmakuKind.SCROLL
    }

    private fun trySpawnScroll(item: DanmakuItem, textWidth: Float, width: Int, laneCount: Int, rollingDurationMs: Int, marginPx: Float, nowMs: Double): Boolean {
        if (item.data.text.isBlank()) return true
        val distancePx = (width.toFloat() + textWidth).coerceAtLeast(0f)
        val rawPx = distancePx / rollingDurationMs.toFloat()
        val shortPx = width.toFloat() / rollingDurationMs.toFloat()
        val maxPx = shortPx * MAX_LONG_SCROLL_SPEED_RATIO
        val pxNew = min(rawPx, maxPx)
        val durationMs = computeScrollDurationMs(distancePx, pxNew, rollingDurationMs)
        for (lane in 0 until laneCount) {
            val prev = laneLastScroll[lane]
            if (prev == null) {
                activate(item, DanmakuKind.SCROLL, lane, textWidth, pxNew, durationMs, nowMs)
                laneLastScroll[lane] = item; laneLastScrollTail[lane] = width.toFloat() + textWidth; return true
            }
            if (isScrollLaneAvailable(width.toFloat(), nowMs, prev, laneLastScrollTail[lane], pxNew, marginPx)) {
                activate(item, DanmakuKind.SCROLL, lane, textWidth, pxNew, durationMs, nowMs)
                laneLastScroll[lane] = item; laneLastScrollTail[lane] = width.toFloat() + textWidth; return true
            }
        }
        return false
    }

    private fun trySpawnFixed(kind: DanmakuKind, item: DanmakuItem, textWidth: Float, laneCount: Int, fixedDurationMs: Int, nowMs: Double): Boolean {
        if (item.data.text.isBlank()) return true
        val lanes = when (kind) { DanmakuKind.TOP -> laneLastTop; DanmakuKind.BOTTOM -> laneLastBottom; else -> return false }
        for (lane in 0 until laneCount) {
            val prev = lanes[lane]
            if (prev == null || nowMs - prev.startTimeMs >= prev.durationMs) {
                activate(item, kind, lane, textWidth, 0f, fixedDurationMs, nowMs)
                lanes[lane] = item; return true
            }
        }
        return false
    }

    private fun activate(item: DanmakuItem, kind: DanmakuKind, lane: Int, textWidth: Float, pxPerMs: Float, durationMs: Int, startTimeMs: Double) {
        item.kind = kind; item.lane = lane; item.textWidthPx = textWidth
        item.pxPerMs = pxPerMs; item.durationMs = durationMs; item.startTimeMs = startTimeMs.toInt()
        active.add(item)
    }

    private fun clearActives() {
        if (active.isEmpty()) return
        val releaseAt = currentUiFrameId + 1
        for (i in active.size - 1 downTo 0) releaseItemCache(active.removeAt(i), releaseAt)
    }

    private fun releaseItemCache(item: DanmakuItem, releaseAtFrameId: Int) {
        val bmp = item.cacheBitmap
        if (bmp != null) { cacheManager.enqueueRelease(bmp, releaseAtFrameId); item.cacheBitmap = null }
        item.cacheState = DanmakuCacheState.Init; item.cacheGeneration = -1
    }

    private fun pruneExpired(width: Int, nowMs: Double) {
        if (active.isEmpty()) return
        val releaseAt = currentUiFrameId + 1
        var write = 0
        for (read in 0 until active.size) {
            val a = active[read]
            val elapsed = nowMs - a.startTimeMs
            var keep = elapsed < a.durationMs
            if (keep && a.kind == DanmakuKind.SCROLL) keep = scrollX(width, nowMs, a.startTimeMs, a.pxPerMs) + a.textWidthPx >= 0f
            if (!keep) { releaseItemCache(a, releaseAt); continue }
            if (write != read) active[write] = a
            write++
        }
        if (write < active.size) active.subList(write, active.size).clear()
    }

    private fun skipOld(nowMs: Double, rollingDurationMs: Int) {
        val ignoreBefore = nowMs - rollingDurationMs
        while (index < items.size && items[index].timeMs() < ignoreBefore) index++
    }

    private fun dropIfLagging(nowMs: Double) {
        val dropBefore = nowMs - MAX_CATCH_UP_LAG_MS
        while (index < items.size && items[index].timeMs() < dropBefore) index++
    }

    private fun enqueuePending(kind: DanmakuKind, item: DanmakuItem, textWidth: Float, nowMs: Double) {
        if (pending.size >= MAX_PENDING) pending.removeFirst()
        pending.addLast(PendingSpawn(kind, item, textWidth, (nowMs + DELAY_STEP_MS).toInt(), nowMs.toInt()))
    }

    private fun publishEmptySnapshot() { val out = writableSnapshot(); out.clear(); latestSnapshot = out }
    private fun writableSnapshot(): RenderSnapshot = if (latestSnapshot === snapshotA) snapshotB else snapshotA

    private fun scrollX(width: Int, nowMs: Double, startTimeMs: Int, pxPerMs: Float): Float =
        (width.toFloat() - (nowMs - startTimeMs).coerceAtLeast(0.0) * pxPerMs).toFloat()

    private fun centerX(width: Int, contentWidth: Float): Float =
        if (width <= 0) 0f else ((width.toFloat() - contentWidth) / 2f).coerceAtLeast(0f)

    private fun isScrollLaneAvailable(width: Float, nowMs: Double, front: DanmakuItem, tailPrev: Float, pxNew: Float, marginPx: Float): Boolean {
        val elapsedPrev = nowMs - front.startTimeMs
        val prevRemaining = front.durationMs - elapsedPrev
        if (prevRemaining <= 0) return true
        if (tailPrev + marginPx > width) return false
        val pxPrev = front.pxPerMs
        if (pxNew <= pxPrev) return true
        val gap0 = (width - tailPrev - marginPx).coerceAtLeast(0f)
        return gap0 >= (pxNew - pxPrev) * prevRemaining
    }

    private fun computeScrollDurationMs(distancePx: Float, pxPerMs: Float, fallback: Int): Int {
        if (!distancePx.isFinite() || distancePx <= 0f || !pxPerMs.isFinite() || pxPerMs <= 0f) return fallback.coerceAtLeast(1)
        return max(fallback.coerceAtLeast(1), ceil((distancePx / pxPerMs).toDouble()).toLong().coerceIn(1L, Int.MAX_VALUE.toLong()).toInt())
    }


    private fun measureTextWidth(item: DanmakuItem, outlinePad: Float, cfg: DanmakuConfig): Float {
        val text = item.data.text
        if (text.isBlank()) return outlinePad * 2f
        // Compute effective text size for measurement
        val clampedSize = min(item.data.textSize, 25)
        val scaleFactor = cfg.textSizeScale.coerceIn(25, 200) / 100f
        val effectiveTextSizePx = (textSizePx * clampedSize / 25f * scaleFactor).coerceAtLeast(1f)
        val savedTextSize = actionPaint.textSize
        actionPaint.textSize = effectiveTextSizePx
        val width = actionPaint.measureText(text) + outlinePad * 2f
        actionPaint.textSize = savedTextSize
        return width
    }

    private fun lowerBound(pos: Double): Int {
        var l = 0; var r = items.size
        while (l < r) { val m = (l + r) ushr 1; if (items[m].timeMs() < pos) l = m + 1 else r = m }
        return l
    }

    private fun ensureLaneBuffers(laneCount: Int) {
        if (laneLastScroll.size < laneCount) laneLastScroll = arrayOfNulls(laneCount)
        if (laneLastScrollTail.size < laneCount) laneLastScrollTail = FloatArray(laneCount)
        if (laneLastTop.size < laneCount) laneLastTop = arrayOfNulls(laneCount)
        if (laneLastBottom.size < laneCount) laneLastBottom = arrayOfNulls(laneCount)
    }

    private fun drawTextDirect(canvas: Canvas, item: DanmakuItem, x: Float, yTop: Float, outlinePad: Float, baselineOffset: Float, opacityAlpha: Int, cfg: DanmakuConfig) {
        val text = item.data.text
        if (text.isBlank()) return
        val drawStrokeEnabled = strokeWidthPx > 0.01f
        val rgb = item.data.color and 0xFFFFFF
        if (drawStrokeEnabled) drawStroke.color = ((opacityAlpha * 0xCC / 255).coerceIn(0, 255) shl 24) or 0x000000
        drawFill.color = (opacityAlpha shl 24) or rgb

        // Compute per-danmaku effective text size
        val clampedSize = min(item.data.textSize, 25)
        val scaleFactor = cfg.textSizeScale.coerceIn(25, 200) / 100f
        val effectiveTs = (textSizePx * clampedSize / 25f * scaleFactor).coerceAtLeast(1f)
        val baseTs = textSizePx * scaleFactor
        if (drawFill.textSize != effectiveTs) { drawFill.textSize = effectiveTs; drawStroke.textSize = effectiveTs }
        drawFill.getFontMetrics(drawFontMetrics)
        val adjustedBaseline = outlinePad - drawFontMetrics.ascent

        val textX = x + outlinePad
        val baseline = yTop + adjustedBaseline
        if (drawStrokeEnabled) canvas.drawText(text, textX, baseline, drawStroke)
        canvas.drawText(text, textX, baseline, drawFill)

        // Restore base text size
        if (drawFill.textSize != baseTs) { drawFill.textSize = baseTs; drawStroke.textSize = baseTs }
    }

    private fun sp(v: Float): Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, displayMetrics)

    private data class PendingSpawn(val kind: DanmakuKind, val item: DanmakuItem, val textWidthPx: Float, var nextTryMs: Int, val firstTryMs: Int)

    private companion object {
        private const val TAG = "DanmakuEngine"
        // 滚动弹幕的基础穿屏时长（durationMultiplier = 1.0 时）。
        const val DEFAULT_ROLLING_DURATION_MS = 7_800f
        // 弹幕显示时长下限，避免过快难以阅读。
        const val MIN_ROLLING_DURATION_MS = 2_500
        // 弹幕显示时长上限，避免长时间占轨。
        const val MAX_ROLLING_DURATION_MS = 20_000
        // 固定弹幕的基础停留时长（durationMultiplier = 1.0 时）。
        const val FIXED_DURATION_MS = 4_000
        // 长弹幕允许比短弹幕更快，但最多只放大到短弹幕基准速度的这个倍数。
        const val MAX_LONG_SCROLL_SPEED_RATIO = 1.5f
        // 轨道暂时放不下时，pending 弹幕下一次重试插入的间隔。
        const val DELAY_STEP_MS = 220
        // 单条 pending 弹幕最多等待这么久，超过后直接放弃，避免无限积压。
        const val MAX_DELAY_MS = 1_600
        // pending 队列最大容量，超出时丢弃最旧等待项，限制内存和重试成本。
        const val MAX_PENDING = 260
        // 单帧最多尝试生成多少条到时弹幕，防止瞬时高峰拖垮 action 线程。
        const val MAX_SPAWN_PER_FRAME = 48
        // 单帧最多重试多少条 pending 弹幕，避免重试本身抢占主要布局时间。
        const val MAX_PENDING_RETRY_PER_FRAME = 48
        // 播放时间落后太多时，直接跳过更早的弹幕，优先追上当前播放进度。
        const val MAX_CATCH_UP_LAG_MS = 1_200
        // 单帧最多向缓存线程提交多少个位图构建请求，限制异步渲染压力。
        const val MAX_CACHE_REQUESTS_PER_FRAME = 12
        // 缓存构建队列的最大深度，超过后本帧不再继续提交新任务。
        const val MAX_CACHE_QUEUE_DEPTH = 64
    }
}
