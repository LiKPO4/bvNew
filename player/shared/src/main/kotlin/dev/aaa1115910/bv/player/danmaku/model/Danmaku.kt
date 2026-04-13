package dev.aaa1115910.bv.player.danmaku.model

data class Danmaku(
    val dmid: Long,
    val positionMs: Int,
    val text: String,
    val mode: Int,
    val textSize: Int,
    val color: Int,
    val level: Int = 0,
) : Comparable<Danmaku> {
    override fun compareTo(other: Danmaku): Int = positionMs.compareTo(other.positionMs)

    companion object {
        const val MODE_SCROLL = 1
        const val MODE_BOTTOM = 4
        const val MODE_TOP = 5
    }
}
