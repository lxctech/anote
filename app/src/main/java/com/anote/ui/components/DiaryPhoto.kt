package com.anote.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.anote.AnoteApp
import com.anote.R
import com.anote.data.local.DiaryImage

@Composable
fun DiaryPhoto(
    image: DiaryImage,
    modifier: Modifier = Modifier,
    size: Dp? = null,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val context = LocalContext.current
    val file = (context.applicationContext as AnoteApp).container.imageStore.fileFor(image.relativePath)
    AsyncImage(
        model = file,
        contentDescription = stringResource(R.string.cd_diary_image),
        contentScale = contentScale,
        modifier = modifier
            .then(if (size != null) Modifier.size(size) else Modifier)
            .clip(RoundedCornerShape(12.dp)),
    )
}

@Composable
fun PhotoStrip(images: List<DiaryImage>, modifier: Modifier = Modifier) {
    if (images.isEmpty()) return
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        images.take(3).forEach { image ->
            DiaryPhoto(image = image, size = 72.dp)
        }
    }
}
