package dev.aaa1115910.bv.tv.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import dev.aaa1115910.biliapi.entity.Picture
import dev.aaa1115910.biliapi.entity.reply.Comment
import qrcode.QRCode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.UUID

/**
 * 从评论文本中提取所有 URL
 */
internal fun extractUrls(commentContent: List<String>): List<String> {
    val text = commentContent.joinToString("")
    val urlRegex = Regex("""https?://[^\s<>"']+""")
    return urlRegex.findAll(text).map { it.value }.distinct().toList()
}

/**
 * 为指定 URL 生成二维码图片（底部包含多行链接文本），直接返回内存位图
 *
 * @param qrSizePx 二维码区域的像素尺寸（默认 400px ≈ 200dp @ 2x）
 * @param textAreaHeightPx 底部文本区域的像素高度
 */
internal fun createQrBitmap(url: String, qrSizePx: Int = 500, textAreaHeightPx: Int = 160): ImageBitmap {
    val padding = 20

    // 1. 生成 QR 码位图
    val output = ByteArrayOutputStream()
    QRCode(url).render().writeImage(output)
    val input = ByteArrayInputStream(output.toByteArray())
    val qrBitmap = BitmapFactory.decodeStream(input)!!
    val scaledQr = Bitmap.createScaledBitmap(qrBitmap, qrSizePx, qrSizePx, true)
    qrBitmap.recycle()

    // 2. 合并二维码（带内边距）+ 底部多行文本到一张位图
    val contentWidth = qrSizePx + padding * 2
    val totalHeight = padding + qrSizePx + 8 + textAreaHeightPx
    val resultBitmap = Bitmap.createBitmap(contentWidth, totalHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(resultBitmap)
    canvas.drawColor(android.graphics.Color.WHITE)

    // 二维码居中于 padding 区域
    canvas.drawBitmap(scaledQr, padding.toFloat(), padding.toFloat(), null)
    scaledQr.recycle()

    // 3. StaticLayout 渲染多行文本（最多 4 行，超出截断）
    val textPaint = TextPaint().apply {
        color = android.graphics.Color.BLACK
        textSize = 32f
        isAntiAlias = true
        typeface = Typeface.DEFAULT
    }

    val textWidth = contentWidth - padding * 2
    val staticLayout = StaticLayout.Builder.obtain(url, 0, url.length, textPaint, textWidth)
        .setAlignment(Layout.Alignment.ALIGN_CENTER)
        .setMaxLines(4)
        .setEllipsize(android.text.TextUtils.TruncateAt.END)
        .build()

    canvas.save()
    canvas.translate(padding.toFloat(), padding + qrSizePx + 8f)
    staticLayout.draw(canvas)
    canvas.restore()

    return resultBitmap.asImageBitmap()
}

/**
 * 获取评论展示内容：原有图片 + 内容链接生成的二维码
 *
 * @return [DisplayResult] 包含合并后的 pictures 列表和内存位图覆盖映射
 */
internal fun getDisplayPictures(context: Context, comment: Comment): DisplayResult {
    val urls = extractUrls(comment.content)
    val baseCount = comment.pictures.size
    val qrPictures = urls.map {
        Picture(url = "", width = 0, height = 0, key = UUID.randomUUID().toString())
    }
    val qrBitmaps = urls.mapIndexed { index, url ->
        (baseCount + index) to createQrBitmap(url)
    }.toMap()

    return DisplayResult(
        pictures = comment.pictures + qrPictures,
        bitmapOverrides = qrBitmaps
    )
}

/**
 * 评论展示结果，包含合并后的图片列表和需要以内存位图覆盖的索引映射
 */
internal data class DisplayResult(
    val pictures: List<Picture>,
    val bitmapOverrides: Map<Int, ImageBitmap>
)
