package org.schabi.newpipe.music.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ArtworkCache {
    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    fun cached(key: String): Bitmap? = cache.get(key)

    suspend fun load(source: String?, sizePx: Int): Bitmap? {
        if (source.isNullOrBlank()) {
            return null
        }
        val key = "$source@$sizePx"
        cache.get(key)?.let { return it }
        return withContext(Dispatchers.IO) {
            val bitmap = runCatching {
                when {
                    source.startsWith("/") -> decodeFile(source, sizePx)
                    source.startsWith("http") -> Picasso.get()
                        .load(source)
                        .resize(sizePx, sizePx)
                        .centerCrop()
                        .get()
                    else -> null
                }
            }.getOrNull()
            if (bitmap != null) {
                cache.put(key, bitmap)
            }
            bitmap
        }
    }

    private fun decodeFile(path: String, sizePx: Int): Bitmap? {
        val file = File(path)
        if (!file.exists()) {
            return null
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        while (bounds.outWidth / sample > sizePx * 2 && sample < 8) {
            sample *= 2
        }
        return BitmapFactory.decodeFile(
            path,
            BitmapFactory.Options().apply { inSampleSize = sample }
        )
    }
}

@Composable
fun Artwork(
    source: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    size: Dp = 56.dp,
    showPlaceholderIcon: Boolean = true
) {
    val sizePx = with(androidx.compose.ui.platform.LocalDensity.current) { size.roundToPx() }
    val key = "$source@$sizePx"
    // The holder itself must be keyed: produceState only relaunches its producer and keeps the
    // previous value, so after the queue advanced in the background the mini player kept showing
    // the old cover until (or unless) the new one finished loading.
    var bitmap by remember(key) { mutableStateOf(ArtworkCache.cached(key)) }
    LaunchedEffect(key) {
        if (bitmap == null) {
            bitmap = ArtworkCache.load(source, sizePx)
        }
    }
    val colors = listOf(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)
    )
    Box(
        modifier = modifier
            .clip(shape)
            .background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center
    ) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else if (showPlaceholderIcon) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.fillMaxSize(0.42f)
            )
        }
    }
}
