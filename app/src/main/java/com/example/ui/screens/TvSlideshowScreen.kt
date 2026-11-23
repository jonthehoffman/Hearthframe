package com.example.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.model.GatheringAlbum
import com.example.model.GatheringPhoto
import com.example.model.TvSlideshowSettings
import com.example.ui.components.EncodedPhotoImage
import kotlinx.coroutines.delay

@Composable
fun TvSlideshowScreen(
  album: GatheringAlbum,
  allPhotos: List<GatheringPhoto>,
  settings: TvSlideshowSettings,
  isCastConnected: Boolean,
  onCastClick: () -> Unit,
  onUpdateSettings: ((TvSlideshowSettings) -> TvSlideshowSettings) -> Unit,
  onOpenSettings: () -> Unit,
  onExitTvMode: () -> Unit,
) {
  BackHandler { onExitTvMode() }

  val view = LocalView.current
  val context = LocalContext.current

  // Keep screen awake & hide system bars while TV Display Mode is active
  DisposableEffect(view, context) {
    val window = (context as? Activity)?.window
    window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    val controller = window?.let { WindowCompat.getInsetsController(it, view) }
    controller?.systemBarsBehavior =
      WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    controller?.hide(WindowInsetsCompat.Type.systemBars())

    onDispose {
      window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      controller?.show(WindowInsetsCompat.Type.systemBars())
    }
  }

  val basePhotos =
    remember(allPhotos, settings.favoritesOnly, settings.shuffleOrder) {
      val filtered =
        if (settings.favoritesOnly) {
          allPhotos.filter { it.starredByUids.isNotEmpty() }.ifEmpty { allPhotos }
        } else {
          allPhotos
        }
      if (settings.shuffleOrder) filtered.shuffled() else filtered
    }

  var currentIndex by remember { mutableIntStateOf(0) }
  var isPlaying by remember { mutableStateOf(true) }
  var controlsVisible by remember { mutableStateOf(true) }

  // Keep index valid when live photos arrive or filters change
  LaunchedEffect(basePhotos.size) {
    if (basePhotos.isNotEmpty() && currentIndex >= basePhotos.size) {
      currentIndex = 0
    }
  }

  // Auto-advance slideshow timer
  LaunchedEffect(isPlaying, currentIndex, basePhotos.size, settings.slideDurationSeconds) {
    if (isPlaying && basePhotos.size > 1) {
      delay(settings.slideDurationSeconds * 1000L)
      currentIndex = (currentIndex + 1) % basePhotos.size
    }
  }

  // Auto-hide playback controls after 4 seconds
  LaunchedEffect(controlsVisible) {
    if (controlsVisible) {
      delay(4000L)
      controlsVisible = false
    }
  }

  val currentPhoto = basePhotos.getOrNull(currentIndex)

  // Gentle Ken Burns scale animation per slide
  val scaleAnim = remember { Animatable(1f) }
  LaunchedEffect(currentPhoto?.id, settings.kenBurnsMotion, isPlaying) {
    scaleAnim.snapTo(1f)
    if (settings.kenBurnsMotion && isPlaying && currentPhoto != null) {
      scaleAnim.animateTo(
        targetValue = 1.08f,
        animationSpec =
          tween(
            durationMillis = settings.slideDurationSeconds * 1000,
            easing = LinearEasing,
          ),
      )
    }
  }

  val focusRequester = remember { FocusRequester() }
  LaunchedEffect(Unit) {
    focusRequester.requestFocus()
  }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Color(0xFF100C09))
        .focusRequester(focusRequester)
        .focusable()
        .onPreviewKeyEvent { keyEvent ->
          if (keyEvent.type != KeyEventType.KeyDown || basePhotos.isEmpty()) {
            false
          } else {
            when (keyEvent.key) {
              Key.DirectionRight, Key.PageDown -> {
                currentIndex = (currentIndex + 1) % basePhotos.size
                controlsVisible = true
                true
              }
              Key.DirectionLeft, Key.PageUp -> {
                currentIndex = if (currentIndex - 1 < 0) basePhotos.lastIndex else currentIndex - 1
                controlsVisible = true
                true
              }
              Key.Spacebar, Key.DirectionCenter, Key.Enter -> {
                isPlaying = !isPlaying
                controlsVisible = true
                true
              }
              Key.Escape -> {
                onExitTvMode()
                true
              }
              else -> false
            }
          }
        }
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
        ) {
          controlsVisible = !controlsVisible
        }
        .testTag("tv_slideshow_root"),
  ) {
    if (currentPhoto == null) {
      Column(
        modifier = Modifier.align(Alignment.Center).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.CloudDone,
          contentDescription = null,
          tint = Color(0xFFF08A65),
          modifier = Modifier.size(48.dp),
        )
        Text(
          text = "Waiting for family photos…",
          style = MaterialTheme.typography.headlineMedium,
          color = Color(0xFFF5EDE5),
        )
        Text(
          text = "Share invite code ${album.inviteCode} — photos sent by anyone will appear here live.",
          style = MaterialTheme.typography.bodyLarge,
          color = Color(0xFFC9B9AD),
        )
      }
    } else {
      Crossfade(
        targetState = currentPhoto,
        animationSpec = tween(durationMillis = 850),
        label = "tv_slide_crossfade",
      ) { photo ->
        Box(modifier = Modifier.fillMaxSize()) {
          // Soft ambient backdrop for portrait shots on wide TVs
          EncodedPhotoImage(
            photo = photo,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(0.22f),
          )
          Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f))
          )
          // Primary crisp photo with gentle Ken Burns motion
          EncodedPhotoImage(
            photo = photo,
            contentDescription = photo.caption.ifBlank { "Photo shared by ${photo.uploaderName}" },
            contentScale = ContentScale.Fit,
            modifier =
              Modifier.fillMaxSize().graphicsLayer {
                val s = scaleAnim.value
                scaleX = s
                scaleY = s
              },
          )
        }
      }

      // Bottom TV gradient scrim + metadata overlay
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .align(Alignment.BottomCenter)
            .background(
              Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))
              )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 20.dp),
      ) {
        if (settings.showOverlayCaptions) {
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Surface(
                color = Color(0xFFF08A65).copy(alpha = 0.22f),
                shape = CircleShape,
              ) {
                Text(
                  text = currentPhoto.parsedMomentTag.label,
                  style = MaterialTheme.typography.labelMedium,
                  color = Color(0xFFFFDAD0),
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
              }
              Text(
                text = "Shared by ${currentPhoto.uploaderName} · ${currentPhoto.dayLabel}",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFFE8D9C8),
              )
            }
            if (currentPhoto.caption.isNotBlank()) {
              Text(
                text = currentPhoto.caption,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
              )
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "${album.title} · ${album.gatheringMonth}",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFC9B9AD),
              )
              Text(
                text = "Live Cloud Sync · ${currentIndex + 1} / ${basePhotos.size}",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF9EC0A1),
              )
            }
          }
        }
      }
    }

    // Auto-hiding top & center TV playback controls
    AnimatedVisibility(
      visible = controlsVisible,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.fillMaxSize(),
    ) {
      Box(
        modifier =
          Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp),
      ) {
        // Top bar
        Row(
          modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            color = Color.Black.copy(alpha = 0.6f),
            shape = CircleShape,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(end = 14.dp),
            ) {
              IconButton(
                onClick = onExitTvMode,
                modifier = Modifier.size(48.dp).testTag("exit_tv_mode_button"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = "Exit TV Slideshow",
                  tint = Color.White,
                )
              }
              Text(
                text = "Exit TV Mode",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
              )
            }
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(color = Color.Black.copy(alpha = 0.6f), shape = CircleShape) {
              IconButton(
                onClick = { onUpdateSettings { it.copy(favoritesOnly = !it.favoritesOnly) } },
                modifier = Modifier.size(48.dp),
              ) {
                Icon(
                  imageVector = if (settings.favoritesOnly) Icons.Filled.Star else Icons.Outlined.StarBorder,
                  contentDescription = "Toggle favorites only",
                  tint = if (settings.favoritesOnly) Color(0xFFEAB965) else Color.White,
                )
              }
            }
            Surface(color = Color.Black.copy(alpha = 0.6f), shape = CircleShape) {
              IconButton(
                onClick = { onUpdateSettings { it.copy(shuffleOrder = !it.shuffleOrder) } },
                modifier = Modifier.size(48.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.Shuffle,
                  contentDescription = "Toggle shuffle",
                  tint = if (settings.shuffleOrder) Color(0xFFF08A65) else Color.White,
                )
              }
            }
            Surface(color = Color.Black.copy(alpha = 0.6f), shape = CircleShape) {
              IconButton(
                onClick = onCastClick,
                modifier = Modifier.size(48.dp).testTag("tv_cast_button"),
              ) {
                Icon(
                  imageVector =
                    if (isCastConnected) Icons.Filled.CastConnected else Icons.Filled.Cast,
                  contentDescription = "Cast to Smart TV",
                  tint = if (isCastConnected) Color(0xFF9EC0A1) else Color.White,
                )
              }
            }
            Surface(color = Color.Black.copy(alpha = 0.6f), shape = CircleShape) {
              IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.size(48.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.Settings,
                  contentDescription = "TV slideshow settings",
                  tint = Color.White,
                )
              }
            }
          }
        }

        // Center playback transport bar
        if (basePhotos.isNotEmpty()) {
          Surface(
            color = Color.Black.copy(alpha = 0.68f),
            shape = CircleShape,
            modifier = Modifier.align(Alignment.Center),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              IconButton(
                onClick = {
                  currentIndex =
                    if (currentIndex - 1 < 0) basePhotos.lastIndex else currentIndex - 1
                  controlsVisible = true
                },
                modifier = Modifier.size(48.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.SkipPrevious,
                  contentDescription = "Previous photo",
                  tint = Color.White,
                )
              }
              FilledIconButton(
                onClick = {
                  isPlaying = !isPlaying
                  controlsVisible = true
                },
                colors =
                  IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color(0xFFF08A65),
                    contentColor = Color(0xFF2B1207),
                  ),
                modifier = Modifier.size(56.dp).testTag("tv_play_pause_button"),
              ) {
                Icon(
                  imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                  contentDescription = if (isPlaying) "Pause slideshow" else "Resume slideshow",
                )
              }
              IconButton(
                onClick = {
                  currentIndex = (currentIndex + 1) % basePhotos.size
                  controlsVisible = true
                },
                modifier = Modifier.size(48.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.SkipNext,
                  contentDescription = "Next photo",
                  tint = Color.White,
                )
              }
            }
          }
        }
      }
    }
  }
}
