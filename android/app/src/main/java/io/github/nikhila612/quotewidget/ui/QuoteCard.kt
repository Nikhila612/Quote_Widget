package io.github.nikhila612.quotewidget.ui

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import io.github.nikhila612.quotewidget.core.Quote
import java.io.File

/** Shareable picture of a quote: 1080 x 1350 (4:5), drawn like the iOS QuoteCard. */
object QuoteCard {
    private const val WIDTH = 1080
    private const val HEIGHT = 1350
    private const val PAD_X = 110f
    private const val PAD_Y = 100f

    /** Longer quotes get smaller type so every quote fits. iOS uses the same steps. */
    private fun fontSize(text: String) = when {
        text.length < 80 -> 76f
        text.length < 160 -> 62f
        else -> 50f
    }

    fun render(quote: Quote, stops: List<Color>, ink: Color): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bg = Paint().apply {
            shader = LinearGradient(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(),
                stops.map { it.toArgb() }.toIntArray(), null, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), bg)

        val textWidth = (WIDTH - 2 * PAD_X).toInt()
        val size = fontSize(quote.text)
        val quotePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink.toArgb(); textSize = size; typeface = Typeface.SERIF
        }
        val authorPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink.toArgb(); textSize = 40f; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val quoteLayout = layout("“${quote.text}”", quotePaint, textWidth, spacing = size * 0.18f)
        val authorLayout = layout("— ${quote.author}", authorPaint, textWidth, spacing = 0f)

        // Quote and author centred as a block, like the iOS card.
        val gap = 48f
        val blockHeight = quoteLayout.height + gap + authorLayout.height
        var y = (HEIGHT - blockHeight) / 2f
        canvas.save(); canvas.translate(PAD_X, y); quoteLayout.draw(canvas); canvas.restore()
        y += quoteLayout.height + gap
        canvas.save(); canvas.translate(PAD_X, y); authorLayout.draw(canvas); canvas.restore()

        val brand = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink.copy(alpha = 0.6f).toArgb(); textSize = 34f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        canvas.drawText("Sunote", PAD_X, HEIGHT - PAD_Y, brand)
        return bitmap
    }

    private fun layout(text: String, paint: TextPaint, width: Int, spacing: Float): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(spacing, 1f)
            .build()

    /** Opens the share sheet with the picture. */
    fun share(context: Context, bitmap: Bitmap) {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "sunote-quote.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("image/png")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri("Today's quote", uri)
        context.startActivity(Intent.createChooser(send, "Share quote"))
    }

    /** Saves the picture to Pictures/Sunote. Needs no permission on Android 10 and later. */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun saveToPhotos(context: Context, bitmap: Bitmap): Boolean {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "Sunote ${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Sunote")
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        return resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } ?: false
    }
}
