package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.GatheringPhoto
import com.example.model.MomentTag
import com.example.ui.components.EncodedPhotoImage

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhotoDetailDialog(
  photo: GatheringPhoto,
  currentUserId: String,
  albumHostUserId: String,
  onToggleStar: () -> Unit,
  onUpdateDetails: (caption: String, momentTag: MomentTag) -> Unit,
  onDelete: () -> Unit,
  onDismiss: () -> Unit,
) {
  val canEditOrDelete = photo.uploaderId == currentUserId || albumHostUserId == currentUserId
  val isStarred = currentUserId in photo.starredByUids
  var isEditing by remember { mutableStateOf(false) }
  var editCaption by remember(photo.id) { mutableStateOf(photo.caption) }
  var editTag by remember(photo.id) { mutableStateOf(photo.parsedMomentTag) }
  var showDeleteConfirm by remember { mutableStateOf(false) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      shape = MaterialTheme.shapes.large,
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      modifier = Modifier.fillMaxWidth(0.94f).padding(vertical = 16.dp),
    ) {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Shared by ${photo.uploaderName}",
              style = MaterialTheme.typography.titleMedium,
            )
            Text(
              text = photo.dayLabel,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          IconButton(
            onClick = onToggleStar,
            modifier = Modifier.size(48.dp).testTag("detail_star_button"),
          ) {
            Icon(
              imageVector = if (isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
              contentDescription = if (isStarred) "Remove TV highlight star" else "Star for TV highlights",
              tint = if (isStarred) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(48.dp).testTag("detail_close_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Close,
              contentDescription = "Close photo details",
            )
          }
        }

        Box(
          modifier =
            Modifier.fillMaxWidth()
              .aspectRatio(photo.aspectRatio.toFloat().coerceIn(0.6f, 1.8f))
              .clip(MaterialTheme.shapes.medium)
              .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
          EncodedPhotoImage(
            photo = photo,
            contentDescription = photo.caption.ifBlank { "Family gathering photo by ${photo.uploaderName}" },
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().align(Alignment.Center),
          )
        }

        if (isEditing) {
          OutlinedTextField(
            value = editCaption,
            onValueChange = { if (it.length <= 160) editCaption = it },
            label = { Text("Caption") },
            maxLines = 2,
            modifier = Modifier.fillMaxWidth(),
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            MomentTag.entries.forEach { tag ->
              FilterChip(
                selected = editTag == tag,
                onClick = { editTag = tag },
                label = { Text(tag.label) },
              )
            }
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
          ) {
            TextButton(onClick = { isEditing = false }) { Text("Cancel") }
            Spacer(Modifier.width(8.dp))
            Button(
              onClick = {
                onUpdateDetails(editCaption, editTag)
                isEditing = false
              }
            ) {
              Text("Save")
            }
          }
        } else {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            AssistChip(
              onClick = {},
              label = { Text(photo.parsedMomentTag.label) },
              shape = CircleShape,
            )
            if (photo.starredByUids.isNotEmpty()) {
              AssistChip(
                onClick = onToggleStar,
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp),
                  )
                },
                label = { Text("${photo.starredByUids.size} TV Highlight") },
              )
            }
          }

          if (photo.caption.isNotBlank()) {
            Text(
              text = photo.caption,
              style = MaterialTheme.typography.bodyLarge,
            )
          }

          if (canEditOrDelete) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              TextButton(
                onClick = { isEditing = true },
                modifier = Modifier.testTag("detail_edit_button"),
              ) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Edit Caption")
              }
              TextButton(
                onClick = { showDeleteConfirm = true },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("detail_delete_button"),
              ) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Remove")
              }
            }
          }
        }
      }
    }
  }

  if (showDeleteConfirm) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirm = false },
      title = { Text("Remove photo from album?") },
      text = { Text("This removes the photo from everyone's synced album and TV slideshow.") },
      confirmButton = {
        Button(
          onClick = {
            showDeleteConfirm = false
            onDelete()
            onDismiss()
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) {
          Text("Remove")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteConfirm = false }) { Text("Keep") }
      },
    )
  }
}
