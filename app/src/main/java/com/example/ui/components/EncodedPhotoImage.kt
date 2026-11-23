package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.data.PhotoCodec
import com.example.model.GatheringPhoto

@Composable
fun EncodedPhotoImage(
  photo: GatheringPhoto,
  contentDescription: String?,
  modifier: Modifier = Modifier,
  contentScale: ContentScale = ContentScale.Crop,
) {
  val bitmap by
    produceState<ImageBitmap?>(initialValue = null, key1 = photo.id, key2 = photo.imageBase64) {
      value = PhotoCodec.decodeForDisplay(photo.id, photo.imageBase64)
    }

  val loaded = bitmap
  if (loaded != null) {
    Image(
      bitmap = loaded,
      contentDescription = contentDescription,
      contentScale = contentScale,
      modifier = modifier,
    )
  } else {
    Box(
      modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = Icons.Filled.Photo,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.size(32.dp),
      )
    }
  }
}
