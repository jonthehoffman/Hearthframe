package com.example.ui.cast

import android.app.Presentation
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.media.MediaRouter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Display
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.model.GatheringAlbum
import com.example.model.GatheringPhoto
import com.example.model.TvSlideshowSettings
import com.example.ui.components.EncodedPhotoImage
import com.example.ui.theme.HearthframeTheme
import kotlinx.coroutines.delay

data class CastDisplayStatus(
  val externalDisplay: Display? = null,
  val routeName: String? = null,
) {
  val isConnected: Boolean
    get() = externalDisplay != null || !routeName.isNullOrBlank()

  val displayLabel: String
    get() =
      routeName?.takeIf { it.isNotBlank() }
        ?: externalDisplay?.name?.takeIf { it.isNotBlank() }
        ?: "Smart TV"
}

@Composable
fun rememberCastDisplayStatus(): CastDisplayStatus {
  val context = LocalContext.current
  var status by remember { mutableStateOf(queryCastStatus(context)) }

  DisposableEffect(context) {
    val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
    val mediaRouter = context.getSystemService(Context.MEDIA_ROUTER_SERVICE) as? MediaRouter
    val handler = Handler(Looper.getMainLooper())

    val displayListener =
      object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {
          status = queryCastStatus(context)
        }

        override fun onDisplayRemoved(displayId: Int) {
          status = queryCastStatus(context)
        }

        override fun onDisplayChanged(displayId: Int) {
          status = queryCastStatus(context)
        }
      }

    val routerCallback =
      object : MediaRouter.SimpleCallback() {
        override fun onRouteSelected(router: MediaRouter?, type: Int, info: MediaRouter.RouteInfo?) {
          status = queryCastStatus(context)
        }

        override fun onRouteUnselected(router: MediaRouter?, type: Int, info: MediaRouter.RouteInfo?) {
          status = queryCastStatus(context)
        }

        override fun onRouteChanged(router: MediaRouter?, info: MediaRouter.RouteInfo?) {
          status = queryCastStatus(context)
        }

        override fun onRoutePresentationDisplayChanged(
          router: MediaRouter?,
          info: MediaRouter.RouteInfo?,
        ) {
          status = queryCastStatus(context)
        }
      }

    displayManager?.registerDisplayListener(displayListener, handler)
    mediaRouter?.addCallback(MediaRouter.ROUTE_TYPE_LIVE_VIDEO, routerCallback)
    status = queryCastStatus(context)

    onDispose {
      displayManager?.unregisterDisplayListener(displayListener)
      mediaRouter?.removeCallback(routerCallback)
    }
  }

  return status
}

private fun queryCastStatus(context: Context): CastDisplayStatus {
  val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
  val presentationDisplay =
    displayManager
      ?.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
      ?.firstOrNull { it.displayId != Display.DEFAULT_DISPLAY }
      ?: displayManager?.displays?.firstOrNull { it.displayId != Display.DEFAULT_DISPLAY }

  val mediaRouter = context.getSystemService(Context.MEDIA_ROUTER_SERVICE) as? MediaRouter
  val selectedVideoRoute = mediaRouter?.getSelectedRoute(MediaRouter.ROUTE_TYPE_LIVE_VIDEO)
  val defaultRoute = mediaRouter?.defaultRoute
  val activeRouteName =
    if (selectedVideoRoute != null && selectedVideoRoute != defaultRoute) {
      selectedVideoRoute.name?.toString()
    } else {
      presentationDisplay?.name
    }

  return CastDisplayStatus(
    externalDisplay = presentationDisplay,
    routeName = activeRouteName,
  )
}

fun openSystemCastPicker(context: Context): Boolean {
  val castIntent =
    Intent(Settings.ACTION_CAST_SETTINGS).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
  if (castIntent.resolveActivity(context.packageManager) != null) {
    return try {
      context.startActivity(castIntent)
      true
    } catch (_: Exception) {
      false
    }
  }
  val displayIntent =
    Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
  return try {
    if (displayIntent.resolveActivity(context.packageManager) != null) {
      context.startActivity(displayIntent)
      true
    } else {
      false
    }
  } catch (_: Exception) {
    false
  }
}

@Composable
fun ExternalTvPresentationHost(
  display: Display?,
  enabled: Boolean,
  album: GatheringAlbum?,
  photos: List<GatheringPhoto>,
  settings: TvSlideshowSettings,
) {
  if (!enabled || display == null || album == null) return

  val hostView = LocalView.current
  val context = LocalContext.current
  val latestAlbum by rememberUpdatedState(album)
  val latestPhotos by rememberUpdatedState(photos)
  val latestSettings by rememberUpdatedState(settings)

  DisposableEffect(display, hostView) {
    val lifecycleOwner = hostView.findViewTreeLifecycleOwner()
    val viewModelStoreOwner = hostView.findViewTreeViewModelStoreOwner()
    val savedStateOwner = hostView.findViewTreeSavedStateRegistryOwner()

    val presentation =
      object : Presentation(context, display) {
        override fun onCreate(savedInstanceState: Bundle?) {
          super.onCreate(savedInstanceState)
          val composeView =
            ComposeView(context).apply {
              setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
              setViewTreeLifecycleOwner(lifecycleOwner)
              setViewTreeViewModelStoreOwner(viewModelStoreOwner)
              setViewTreeSavedStateRegistryOwner(savedStateOwner)
              setContent {
                HearthframeTheme(darkTheme = true) {
                  ExternalTvSlideshowContent(
                    album = latestAlbum,
                    allPhotos = latestPhotos,
                    settings = latestSettings,
                    tvName = display.name ?: "Smart TV",
                  )
                }
              }
            }
          setContentView(composeView)
        }
      }

    try {
      presentation.show()
    } catch (_: Exception) {
      // Display disconnected before presentation attached
    }

    onDispose {
      try {
        if (presentation.isShowing) {
          presentation.dismiss()
        }
      } catch (_: Exception) {
      }
    }
  }
}

@Composable
private fun ExternalTvSlideshowContent(
  album: GatheringAlbum,
  allPhotos: List<GatheringPhoto>,
  settings: TvSlideshowSettings,
  tvName: String,
) {
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

  LaunchedEffect(basePhotos.size) {
    if (basePhotos.isNotEmpty() && currentIndex >= basePhotos.size) {
      currentIndex = 0
    }
  }

  LaunchedEffect(currentIndex, basePhotos.size, settings.slideDurationSeconds) {
    if (basePhotos.size > 1) {
      delay(settings.slideDurationSeconds * 1000L)
      currentIndex = (currentIndex + 1) % basePhotos.size
    }
  }

  val currentPhoto = basePhotos.getOrNull(currentIndex)
  val scaleAnim = remember { Animatable(1f) }
  LaunchedEffect(currentPhoto?.id, settings.kenBurnsMotion) {
    scaleAnim.snapTo(1f)
    if (settings.kenBurnsMotion && currentPhoto != null) {
      scaleAnim.animateTo(
        targetValue = 1.07f,
        animationSpec =
          tween(
            durationMillis = settings.slideDurationSeconds * 1000,
            easing = LinearEasing,
          ),
      )
    }
  }

  Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0E0A08))) {
    if (currentPhoto == null) {
      Column(
        modifier = Modifier.align(Alignment.Center).padding(36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.CloudDone,
          contentDescription = null,
          tint = Color(0xFFF08A65),
          modifier = Modifier.size(56.dp),
        )
        Text(
          text = album.title,
          style = MaterialTheme.typography.displaySmall,
          color = Color(0xFFF5EDE5),
        )
        Text(
          text = "Connected to $tvName · Share code ${album.inviteCode} to sync photos live",
          style = MaterialTheme.typography.titleMedium,
          color = Color(0xFFC9B9AD),
        )
      }
    } else {
      Crossfade(
        targetState = currentPhoto,
        animationSpec = tween(durationMillis = 850),
        label = "external_tv_crossfade",
      ) { photo ->
        Box(modifier = Modifier.fillMaxSize()) {
          EncodedPhotoImage(
            photo = photo,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(0.22f),
          )
          Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))
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

      if (settings.showOverlayCaptions) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .align(Alignment.BottomCenter)
              .background(
                Brush.verticalGradient(
                  colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))
                )
              )
              .padding(horizontal = 36.dp, vertical = 28.dp),
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Surface(color = Color(0xFFF08A65).copy(alpha = 0.25f), shape = CircleShape) {
                Text(
                  text = currentPhoto.parsedMomentTag.label,
                  style = MaterialTheme.typography.labelLarge,
                  color = Color(0xFFFFDAD0),
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
              }
              Text(
                text = "Shared by ${currentPhoto.uploaderName} · ${currentPhoto.dayLabel}",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFE8D9C8),
              )
            }
            if (currentPhoto.caption.isNotBlank()) {
              Text(
                text = currentPhoto.caption,
                style = MaterialTheme.typography.headlineMedium,
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
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFC9B9AD),
              )
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.CastConnected,
                  contentDescription = null,
                  tint = Color(0xFF9EC0A1),
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  text = "Casting Live · ${currentIndex + 1} / ${basePhotos.size}",
                  style = MaterialTheme.typography.labelLarge,
                  color = Color(0xFF9EC0A1),
                )
              }
            }
          }
        }
      }
    }
  }
}
