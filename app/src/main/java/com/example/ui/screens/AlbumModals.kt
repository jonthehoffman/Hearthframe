package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.MomentTag
import com.example.model.TvSlideshowSettings
import com.example.ui.cast.CastDisplayStatus

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateGatheringDialog(
  onDismiss: () -> Unit,
  onCreate: (title: String, month: String) -> Unit,
) {
  var title by remember { mutableStateOf("") }
  var month by remember { mutableStateOf("November 2026") }
  val suggestedMonths = listOf("November 2026", "Thanksgiving 2026", "November 2025")

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Create Family Gathering",
        style = MaterialTheme.typography.headlineSmall,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text = "Everyone in your family can join with the invite code and drop photos straight to the TV.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
          value = title,
          onValueChange = { if (it.length <= 80) title = it },
          label = { Text("Gathering Name") },
          placeholder = { Text("e.g., Hoffman Thanksgiving") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("create_album_title_input"),
        )
        OutlinedTextField(
          value = month,
          onValueChange = { if (it.length <= 40) month = it },
          label = { Text("Month & Year") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("create_album_month_input"),
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          suggestedMonths.forEach { suggestion ->
            FilterChip(
              selected = month == suggestion,
              onClick = { month = suggestion },
              label = { Text(suggestion) },
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onCreate(title, month) },
        enabled = title.trim().length >= 2 && month.trim().length >= 2,
        modifier = Modifier.testTag("confirm_create_album_button"),
      ) {
        Text("Create Album")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
  )
}

@Composable
fun JoinGatheringDialog(
  onDismiss: () -> Unit,
  onJoin: (code: String) -> Unit,
) {
  var code by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Filled.GroupAdd,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
      )
    },
    title = {
      Text(
        text = "Join Family Gathering",
        style = MaterialTheme.typography.headlineSmall,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text = "Enter the invite code shared by your family host to sync photos automatically.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
          value = code,
          onValueChange = { raw ->
            val sanitized = raw.uppercase().filter { it.isLetterOrDigit() }.take(12)
            code = sanitized
          },
          label = { Text("Invite Code") },
          placeholder = { Text("e.g., NOV26K") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("join_album_code_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onJoin(code) },
        enabled = code.length >= 4,
        modifier = Modifier.testTag("confirm_join_album_button"),
      ) {
        Text("Join Album")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SendPhotosSheetDialog(
  photoCount: Int,
  onDismiss: () -> Unit,
  onConfirmUpload: (momentTag: MomentTag, caption: String) -> Unit,
) {
  var selectedTag by remember { mutableStateOf(MomentTag.DINNER_TABLE) }
  var caption by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Filled.CloudUpload,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
      )
    },
    title = {
      Text(
        text = if (photoCount == 1) "Send 1 Photo to Cloud" else "Send $photoCount Photos to Cloud",
        style = MaterialTheme.typography.headlineSmall,
      )
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = "Hearthframe automatically reads dates and optimizes images for living room TVs.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = "Tag this moment",
          style = MaterialTheme.typography.labelLarge,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          MomentTag.entries.forEach { tag ->
            FilterChip(
              selected = selectedTag == tag,
              onClick = { selectedTag = tag },
              label = { Text(tag.label) },
            )
          }
        }
        OutlinedTextField(
          value = caption,
          onValueChange = { if (it.length <= 160) caption = it },
          label = { Text("Optional caption for TV") },
          placeholder = { Text("e.g., Before the pumpkin pie disappeared") },
          maxLines = 2,
          modifier = Modifier.fillMaxWidth().testTag("upload_caption_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirmUpload(selectedTag, caption) },
        modifier = Modifier.testTag("confirm_send_photos_button"),
      ) {
        Text("Sync Now")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
  )
}

@Composable
fun TvAndAccountSettingsDialog(
  userName: String,
  userEmail: String?,
  tvSettings: TvSlideshowSettings,
  onUpdateSettings: ((TvSlideshowSettings) -> TvSlideshowSettings) -> Unit,
  onSignOut: () -> Unit,
  onDismiss: () -> Unit,
) {
  val durations = listOf(5, 8, 15, 30)

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Filled.Tv,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
      )
    },
    title = {
      Text(
        text = "TV Display & Account",
        style = MaterialTheme.typography.headlineSmall,
      )
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = "Signed in as $userName" + if (!userEmail.isNullOrBlank()) " ($userEmail)" else "",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider()
        Text(
          text = "Slide Interval",
          style = MaterialTheme.typography.labelLarge,
        )
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          durations.forEach { sec ->
            FilterChip(
              selected = tvSettings.slideDurationSeconds == sec,
              onClick = { onUpdateSettings { it.copy(slideDurationSeconds = sec) } },
              label = { Text("${sec}s") },
            )
          }
        }
        SettingSwitchRow(
          label = "Gentle Ken Burns zoom",
          checked = tvSettings.kenBurnsMotion,
          onCheckedChange = { enabled -> onUpdateSettings { it.copy(kenBurnsMotion = enabled) } },
        )
        SettingSwitchRow(
          label = "Show contributor & caption on TV",
          checked = tvSettings.showOverlayCaptions,
          onCheckedChange = { enabled -> onUpdateSettings { it.copy(showOverlayCaptions = enabled) } },
        )
        SettingSwitchRow(
          label = "Shuffle slideshow order",
          checked = tvSettings.shuffleOrder,
          onCheckedChange = { enabled -> onUpdateSettings { it.copy(shuffleOrder = enabled) } },
        )
        SettingSwitchRow(
          label = "Show starred favorites only on TV",
          checked = tvSettings.favoritesOnly,
          onCheckedChange = { enabled -> onUpdateSettings { it.copy(favoritesOnly = enabled) } },
        )
        SettingSwitchRow(
          label = "Start in TV Display Mode on launch",
          checked = tvSettings.launchIntoTvMode,
          onCheckedChange = { enabled -> onUpdateSettings { it.copy(launchIntoTvMode = enabled) } },
        )
        SettingSwitchRow(
          label = "Auto-project slideshow when Smart TV connects",
          checked = tvSettings.autoProjectExternalDisplay,
          onCheckedChange = { enabled ->
            onUpdateSettings { it.copy(autoProjectExternalDisplay = enabled) }
          },
        )
        HorizontalDivider()
        OutlinedButton(
          onClick = {
            onDismiss()
            onSignOut()
          },
          colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
          modifier = Modifier.fillMaxWidth().testTag("sign_out_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Logout,
            contentDescription = null,
          )
          Spacer(Modifier.width(8.dp))
          Text("Sign Out")
        }
      }
    },
    confirmButton = {
      Button(onClick = onDismiss) {
        Text("Done")
      }
    },
  )
}

@Composable
private fun SettingSwitchRow(
  label: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      modifier = Modifier.weight(1f).padding(end = 12.dp),
    )
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}

@Composable
fun CastToTvDialog(
  castStatus: CastDisplayStatus,
  tvSettings: TvSlideshowSettings,
  hasActiveAlbum: Boolean,
  onOpenSystemCastPicker: () -> Unit,
  onStartLocalTvSlideshow: () -> Unit,
  onToggleAutoProject: (Boolean) -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector =
          if (castStatus.isConnected) Icons.Filled.CastConnected else Icons.Filled.Cast,
        contentDescription = null,
        tint =
          if (castStatus.isConnected) MaterialTheme.colorScheme.secondary
          else MaterialTheme.colorScheme.primary,
      )
    },
    title = {
      Text(
        text =
          if (castStatus.isConnected) "Casting to ${castStatus.displayLabel}"
          else "Cast to Smart TV",
        style = MaterialTheme.typography.headlineSmall,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text =
            if (castStatus.isConnected) {
              "Your external TV display is connected. Hearthframe can project the live family slideshow directly onto ${castStatus.displayLabel} while your phone stays a remote."
            } else {
              "Connect your phone or tablet to a Smart TV (Chromecast, Roku, Google TV, Miracast, or HDMI) to stream your November gathering slideshow live."
            },
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SettingSwitchRow(
          label = "Project slideshow onto connected TV screen",
          checked = tvSettings.autoProjectExternalDisplay,
          onCheckedChange = onToggleAutoProject,
        )

        Button(
          onClick = onOpenSystemCastPicker,
          shape = MaterialTheme.shapes.large,
          modifier = Modifier.fillMaxWidth().testTag("open_system_cast_picker_button"),
        ) {
          Icon(Icons.Filled.Cast, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text(
            if (castStatus.isConnected) "Change Cast Display"
            else "Open Wireless Cast Picker"
          )
        }

        if (hasActiveAlbum) {
          OutlinedButton(
            onClick = {
              onDismiss()
              onStartLocalTvSlideshow()
            },
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth().testTag("cast_dialog_local_tv_button"),
          ) {
            Icon(Icons.Filled.Tv, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Start Full-Screen TV Slideshow Here")
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Close")
      }
    },
  )
}
