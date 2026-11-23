package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.GatheringAlbum
import com.example.model.GatheringPhoto
import com.example.model.MomentTag
import com.example.model.SyncUploadStatus
import com.example.model.UiState
import com.example.ui.auth.signOut
import com.example.ui.cast.ExternalTvPresentationHost
import com.example.ui.cast.openSystemCastPicker
import com.example.ui.cast.rememberCastDisplayStatus
import com.example.ui.components.EncodedPhotoImage
import com.example.ui.viewmodel.HearthViewModel

@Composable
fun HearthHomeScreen(
  viewModel: HearthViewModel,
  currentUserId: String,
  currentUserName: String,
  currentUserEmail: String?,
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  val snackbarHostState = remember { SnackbarHostState() }

  val albumsState by viewModel.albumsState.collectAsStateWithLifecycle()
  val activeAlbum by viewModel.activeAlbum.collectAsStateWithLifecycle()
  val photosState by viewModel.photosState.collectAsStateWithLifecycle()
  val selectedDay by viewModel.selectedDayFilter.collectAsStateWithLifecycle()
  val selectedMoment by viewModel.selectedMomentFilter.collectAsStateWithLifecycle()
  val selectedContributor by viewModel.selectedContributorFilter.collectAsStateWithLifecycle()
  val favoritesOnly by viewModel.favoritesOnlyFilter.collectAsStateWithLifecycle()
  val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
  val isTvModeActive by viewModel.isTvModeActive.collectAsStateWithLifecycle()
  val tvSettings by viewModel.tvSettings.collectAsStateWithLifecycle()

  var showCreateAlbumDialog by remember { mutableStateOf(false) }
  var showJoinAlbumDialog by remember { mutableStateOf(false) }
  var showSettingsDialog by remember { mutableStateOf(false) }
  var showCastDialog by remember { mutableStateOf(false) }
  var selectedPhotoForDetail by remember { mutableStateOf<GatheringPhoto?>(null) }
  var pendingUploadUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
  var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

  val castStatus = rememberCastDisplayStatus()

  LaunchedEffect(viewModel) {
    viewModel.events.collect { msg ->
      snackbarHostState.showSnackbar(msg)
    }
  }

  // Zero-permission Android Photo Picker (up to 20 photos at once)
  val multiPhotoPicker =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.PickMultipleVisualMedia(20)
    ) { uris ->
      if (uris.isNotEmpty()) {
        pendingUploadUris = uris
      }
    }

  // Zero-permission TakePicture via FileProvider
  val cameraCaptureLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.TakePicture()
    ) { success ->
      val uri = pendingCameraUri
      if (success && uri != null) {
        pendingUploadUris = listOf(uri)
      }
      pendingCameraUri = null
    }

  val album = activeAlbum
  val allPhotos = (photosState as? UiState.Success)?.data.orEmpty()

  ExternalTvPresentationHost(
    display = castStatus.externalDisplay,
    enabled = tvSettings.autoProjectExternalDisplay && !isTvModeActive,
    album = album,
    photos = allPhotos,
    settings = tvSettings,
  )

  if (isTvModeActive && album != null) {
    TvSlideshowScreen(
      album = album,
      allPhotos = allPhotos,
      settings = tvSettings,
      isCastConnected = castStatus.isConnected,
      onCastClick = { showCastDialog = true },
      onUpdateSettings = viewModel::updateTvSettings,
      onOpenSettings = { showSettingsDialog = true },
      onExitTvMode = viewModel::exitTvMode,
    )
  } else {
    val filteredPhotos =
      remember(allPhotos, selectedDay, selectedMoment, selectedContributor, favoritesOnly, currentUserId) {
        allPhotos.filter { photo ->
          (selectedDay == null || photo.dayLabel == selectedDay) &&
            (selectedMoment == null || photo.parsedMomentTag == selectedMoment) &&
            (selectedContributor == null || photo.uploaderName == selectedContributor) &&
            (!favoritesOnly || photo.starredByUids.isNotEmpty())
        }
      }

    val availableDays = remember(allPhotos) { allPhotos.map { it.dayLabel }.distinct() }
    val availableContributors = remember(allPhotos) { allPhotos.map { it.uploaderName }.distinct() }

    Scaffold(
      containerColor = MaterialTheme.colorScheme.background,
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      snackbarHost = {
        SnackbarHost(
          hostState = snackbarHostState,
          modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
        )
      },
      floatingActionButton = {
        if (album != null) {
          Row(
            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            FilledTonalButton(
              onClick = {
                val uri = viewModel.createCameraCaptureUri(context)
                pendingCameraUri = uri
                cameraCaptureLauncher.launch(uri)
              },
              shape = CircleShape,
              contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
              modifier = Modifier.testTag("snap_sync_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = stringResource(R.string.action_snap_sync),
                modifier = Modifier.size(20.dp),
              )
              Spacer(Modifier.width(6.dp))
              Text("Snap")
            }
            ExtendedFloatingActionButton(
              onClick = {
                multiPhotoPicker.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              icon = {
                Icon(
                  imageVector = Icons.Filled.AddPhotoAlternate,
                  contentDescription = null,
                )
              },
              text = { Text(stringResource(R.string.action_send_photos)) },
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.testTag("send_photos_fab"),
            )
          }
        }
      },
    ) { innerPadding ->
      BoxWithConstraints(
        modifier =
          Modifier.fillMaxSize()
            .padding(innerPadding)
            .windowInsetsPadding(WindowInsets.statusBars),
      ) {
        val isWideDesktop = maxWidth >= 840.dp
        val columnCount =
          when {
            maxWidth >= 1150.dp -> 4
            maxWidth >= 840.dp -> 3
            maxWidth >= 600.dp -> 3
            else -> 2
          }

        if (isWideDesktop) {
          Row(modifier = Modifier.fillMaxSize()) {
            // Desktop / Tablet Left Sidebar Pane
            Surface(
              color = MaterialTheme.colorScheme.surfaceContainerLowest,
              tonalElevation = 1.dp,
              modifier = Modifier.width(340.dp).fillMaxSize(),
            ) {
              Column(
                modifier =
                  Modifier.fillMaxSize()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
              ) {
                TopGatheringBar(
                  albums = (albumsState as? UiState.Success)?.data.orEmpty(),
                  activeAlbum = album,
                  isCastConnected = castStatus.isConnected,
                  onSelectAlbum = viewModel::selectAlbum,
                  onCreateClick = { showCreateAlbumDialog = true },
                  onJoinClick = { showJoinAlbumDialog = true },
                  onStartTvMode = viewModel::enterTvMode,
                  onCastClick = { showCastDialog = true },
                  onOpenSettings = { showSettingsDialog = true },
                  compactActions = true,
                )

                if (album != null) {
                  GatheringInviteBanner(
                    album = album,
                    photoCount = allPhotos.size,
                    onCopyCode = {
                      clipboardManager.setText(AnnotatedString(album.inviteCode))
                      viewModel.emitMessage("Copied invite code ${album.inviteCode}")
                    },
                    onShareInvite = {
                      val shareIntent =
                        Intent(Intent.ACTION_SEND).apply {
                          type = "text/plain"
                          putExtra(
                            Intent.EXTRA_TEXT,
                            "Send your ${album.gatheringMonth} photos to our shared Hearthframe TV album \"${album.title}\"! Join with invite code: ${album.inviteCode}",
                          )
                        }
                      context.startActivity(Intent.createChooser(shareIntent, "Invite Family"))
                    },
                  )

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Button(
                      onClick = viewModel::enterTvMode,
                      shape = MaterialTheme.shapes.large,
                      modifier = Modifier.weight(1f).height(50.dp).testTag("desktop_tv_mode_button"),
                    ) {
                      Icon(Icons.Filled.Tv, contentDescription = null)
                      Spacer(Modifier.width(6.dp))
                      Text("TV Mode")
                    }
                    FilledTonalButton(
                      onClick = { showCastDialog = true },
                      shape = MaterialTheme.shapes.large,
                      modifier = Modifier.height(50.dp).testTag("desktop_cast_button"),
                    ) {
                      Icon(
                        imageVector =
                          if (castStatus.isConnected) Icons.Filled.CastConnected else Icons.Filled.Cast,
                        contentDescription = stringResource(R.string.cd_cast_button),
                      )
                      Spacer(Modifier.width(6.dp))
                      Text(if (castStatus.isConnected) "Casting" else "Cast")
                    }
                  }

                  if (allPhotos.isNotEmpty()) {
                    AutoOrganizationFilterStrip(
                      availableDays = availableDays,
                      availableContributors = availableContributors,
                      selectedDay = selectedDay,
                      selectedMoment = selectedMoment,
                      selectedContributor = selectedContributor,
                      favoritesOnly = favoritesOnly,
                      onSelectDay = viewModel::setDayFilter,
                      onSelectMoment = viewModel::setMomentFilter,
                      onSelectContributor = viewModel::setContributorFilter,
                      onToggleFavorites = viewModel::toggleFavoritesFilter,
                      onClearAll = viewModel::clearFilters,
                    )
                  }
                }
              }
            }

            // Desktop / Tablet Main Content Pane
            Column(modifier = Modifier.weight(1f).fillMaxSize()) {
              SyncProgressBanner(syncStatus = syncStatus)
              AlbumGalleryContent(
                albumsState = albumsState,
                album = album,
                photosState = photosState,
                allPhotos = allPhotos,
                filteredPhotos = filteredPhotos,
                availableDays = availableDays,
                availableContributors = availableContributors,
                selectedDay = selectedDay,
                selectedMoment = selectedMoment,
                selectedContributor = selectedContributor,
                favoritesOnly = favoritesOnly,
                currentUserId = currentUserId,
                columnCount = columnCount,
                showInlineBannerAndFilters = false,
                onCreateClick = { showCreateAlbumDialog = true },
                onJoinClick = { showJoinAlbumDialog = true },
                onSendPhotosClick = {
                  multiPhotoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                  )
                },
                onCopyCode = {},
                onShareInvite = {},
                onSelectDay = viewModel::setDayFilter,
                onSelectMoment = viewModel::setMomentFilter,
                onSelectContributor = viewModel::setContributorFilter,
                onToggleFavorites = viewModel::toggleFavoritesFilter,
                onClearFilters = viewModel::clearFilters,
                onPhotoClick = { selectedPhotoForDetail = it },
                onToggleStar = viewModel::toggleStar,
              )
            }
          }
        } else {
          Column(modifier = Modifier.fillMaxSize()) {
            // Top Editorial App Header
            TopGatheringBar(
              albums = (albumsState as? UiState.Success)?.data.orEmpty(),
              activeAlbum = album,
              isCastConnected = castStatus.isConnected,
              onSelectAlbum = viewModel::selectAlbum,
              onCreateClick = { showCreateAlbumDialog = true },
              onJoinClick = { showJoinAlbumDialog = true },
              onStartTvMode = viewModel::enterTvMode,
              onCastClick = { showCastDialog = true },
              onOpenSettings = { showSettingsDialog = true },
              compactActions = false,
            )

            SyncProgressBanner(syncStatus = syncStatus)

            AlbumGalleryContent(
              albumsState = albumsState,
              album = album,
              photosState = photosState,
              allPhotos = allPhotos,
              filteredPhotos = filteredPhotos,
              availableDays = availableDays,
              availableContributors = availableContributors,
              selectedDay = selectedDay,
              selectedMoment = selectedMoment,
              selectedContributor = selectedContributor,
              favoritesOnly = favoritesOnly,
              currentUserId = currentUserId,
              columnCount = columnCount,
              showInlineBannerAndFilters = true,
              onCreateClick = { showCreateAlbumDialog = true },
              onJoinClick = { showJoinAlbumDialog = true },
              onSendPhotosClick = {
                multiPhotoPicker.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              onCopyCode = {
                if (album != null) {
                  clipboardManager.setText(AnnotatedString(album.inviteCode))
                  viewModel.emitMessage("Copied invite code ${album.inviteCode}")
                }
              },
              onShareInvite = {
                if (album != null) {
                  val shareIntent =
                    Intent(Intent.ACTION_SEND).apply {
                      type = "text/plain"
                      putExtra(
                        Intent.EXTRA_TEXT,
                        "Send your ${album.gatheringMonth} photos to our shared Hearthframe TV album \"${album.title}\"! Join with invite code: ${album.inviteCode}",
                      )
                    }
                  context.startActivity(Intent.createChooser(shareIntent, "Invite Family"))
                }
              },
              onSelectDay = viewModel::setDayFilter,
              onSelectMoment = viewModel::setMomentFilter,
              onSelectContributor = viewModel::setContributorFilter,
              onToggleFavorites = viewModel::toggleFavoritesFilter,
              onClearFilters = viewModel::clearFilters,
              onPhotoClick = { selectedPhotoForDetail = it },
              onToggleStar = viewModel::toggleStar,
            )
          }
        }
      }
    }
  }

  if (showCreateAlbumDialog) {
    CreateGatheringDialog(
      onDismiss = { showCreateAlbumDialog = false },
      onCreate = { title, month ->
        viewModel.createAlbum(title, month) { success ->
          if (success) showCreateAlbumDialog = false
        }
      },
    )
  }

  if (showJoinAlbumDialog) {
    JoinGatheringDialog(
      onDismiss = { showJoinAlbumDialog = false },
      onJoin = { code ->
        viewModel.joinAlbumByCode(code) { success ->
          if (success) showJoinAlbumDialog = false
        }
      },
    )
  }

  if (pendingUploadUris.isNotEmpty()) {
    SendPhotosSheetDialog(
      photoCount = pendingUploadUris.size,
      onDismiss = { pendingUploadUris = emptyList() },
      onConfirmUpload = { tag, caption ->
        val toUpload = pendingUploadUris
        pendingUploadUris = emptyList()
        viewModel.sendPhotos(context, toUpload, tag, caption)
      },
    )
  }

  val activeDetailPhoto = selectedPhotoForDetail
  if (activeDetailPhoto != null && album != null) {
    // Keep detail dialog synced with latest star/caption changes
    val latestPhoto = allPhotos.firstOrNull { it.id == activeDetailPhoto.id } ?: activeDetailPhoto
    PhotoDetailDialog(
      photo = latestPhoto,
      currentUserId = currentUserId,
      albumHostUserId = album.hostUserId,
      onToggleStar = { viewModel.toggleStar(latestPhoto) },
      onUpdateDetails = { caption, tag ->
        viewModel.updatePhotoDetails(latestPhoto.id, caption, tag)
      },
      onDelete = { viewModel.deletePhoto(latestPhoto.id) },
      onDismiss = { selectedPhotoForDetail = null },
    )
  }

  if (showSettingsDialog) {
    TvAndAccountSettingsDialog(
      userName = currentUserName,
      userEmail = currentUserEmail,
      tvSettings = tvSettings,
      onUpdateSettings = viewModel::updateTvSettings,
      onSignOut = {
        signOut(credentialManager = credentialManager, scope = scope)
      },
      onDismiss = { showSettingsDialog = false },
    )
  }

  if (showCastDialog) {
    CastToTvDialog(
      castStatus = castStatus,
      tvSettings = tvSettings,
      hasActiveAlbum = album != null,
      onOpenSystemCastPicker = {
        val launched = openSystemCastPicker(context)
        if (!launched) {
          viewModel.emitMessage("Open Android Quick Settings > Cast to mirror to your Smart TV.")
        }
      },
      onStartLocalTvSlideshow = viewModel::enterTvMode,
      onToggleAutoProject = { enabled ->
        viewModel.updateTvSettings { it.copy(autoProjectExternalDisplay = enabled) }
      },
      onDismiss = { showCastDialog = false },
    )
  }
}

@Composable
private fun SyncProgressBanner(syncStatus: SyncUploadStatus) {
  AnimatedVisibility(visible = syncStatus.activeSync) {
    Surface(
      color = MaterialTheme.colorScheme.tertiaryContainer,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
        Text(
          text = "Syncing ${syncStatus.completedCount} of ${syncStatus.totalCount} family photos to cloud…",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
          progress = {
            if (syncStatus.totalCount > 0) {
              syncStatus.completedCount.toFloat() / syncStatus.totalCount.toFloat()
            } else 0f
          },
          modifier = Modifier.fillMaxWidth().clip(CircleShape),
        )
      }
    }
  }
}

@Composable
private fun AlbumGalleryContent(
  albumsState: UiState<List<GatheringAlbum>>,
  album: GatheringAlbum?,
  photosState: UiState<List<GatheringPhoto>>,
  allPhotos: List<GatheringPhoto>,
  filteredPhotos: List<GatheringPhoto>,
  availableDays: List<String>,
  availableContributors: List<String>,
  selectedDay: String?,
  selectedMoment: MomentTag?,
  selectedContributor: String?,
  favoritesOnly: Boolean,
  currentUserId: String,
  columnCount: Int,
  showInlineBannerAndFilters: Boolean,
  onCreateClick: () -> Unit,
  onJoinClick: () -> Unit,
  onSendPhotosClick: () -> Unit,
  onCopyCode: () -> Unit,
  onShareInvite: () -> Unit,
  onSelectDay: (String?) -> Unit,
  onSelectMoment: (MomentTag?) -> Unit,
  onSelectContributor: (String?) -> Unit,
  onToggleFavorites: () -> Unit,
  onClearFilters: () -> Unit,
  onPhotoClick: (GatheringPhoto) -> Unit,
  onToggleStar: (GatheringPhoto) -> Unit,
) {
  when (albumsState) {
    is UiState.Loading -> {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
    }

    is UiState.Error -> {
      ErrorBannerCard(
        message = albumsState.message,
        modifier = Modifier.padding(20.dp),
      )
    }

    is UiState.Success -> {
      if (album == null) {
        NoAlbumsEmptyState(
          onCreateClick = onCreateClick,
          onJoinClick = onJoinClick,
        )
      } else {
        LazyVerticalGrid(
          columns = GridCells.Fixed(columnCount),
          contentPadding =
            PaddingValues(
              start = 16.dp,
              end = 16.dp,
              top = 8.dp,
              bottom = 112.dp,
            ),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxSize(),
        ) {
          if (showInlineBannerAndFilters) {
            item(span = { GridItemSpan(maxLineSpan) }) {
              GatheringInviteBanner(
                album = album,
                photoCount = allPhotos.size,
                onCopyCode = onCopyCode,
                onShareInvite = onShareInvite,
              )
            }

            if (allPhotos.isNotEmpty()) {
              item(span = { GridItemSpan(maxLineSpan) }) {
                AutoOrganizationFilterStrip(
                  availableDays = availableDays,
                  availableContributors = availableContributors,
                  selectedDay = selectedDay,
                  selectedMoment = selectedMoment,
                  selectedContributor = selectedContributor,
                  favoritesOnly = favoritesOnly,
                  onSelectDay = onSelectDay,
                  onSelectMoment = onSelectMoment,
                  onSelectContributor = onSelectContributor,
                  onToggleFavorites = onToggleFavorites,
                  onClearAll = onClearFilters,
                )
              }
            }
          }

          when (photosState) {
            is UiState.Loading -> {
              item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                  modifier = Modifier.fillMaxWidth().padding(48.dp),
                  contentAlignment = Alignment.Center,
                ) {
                  CircularProgressIndicator()
                }
              }
            }

            is UiState.Error -> {
              item(span = { GridItemSpan(maxLineSpan) }) {
                ErrorBannerCard(message = photosState.message)
              }
            }

            is UiState.Success -> {
              if (allPhotos.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                  NoPhotosEmptyState(onSendPhotosClick = onSendPhotosClick)
                }
              } else if (filteredPhotos.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                  Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Text(
                      text = "No photos match these filters",
                      style = MaterialTheme.typography.titleMedium,
                    )
                    OutlinedButton(onClick = onClearFilters) {
                      Text("Show All ${allPhotos.size} Photos")
                    }
                  }
                }
              } else {
                items(items = filteredPhotos, key = { it.id }) { photo ->
                  PhotoGridCard(
                    photo = photo,
                    isStarred = currentUserId in photo.starredByUids,
                    onPhotoClick = { onPhotoClick(photo) },
                    onToggleStar = { onToggleStar(photo) },
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun TopGatheringBar(
  albums: List<GatheringAlbum>,
  activeAlbum: GatheringAlbum?,
  isCastConnected: Boolean,
  onSelectAlbum: (String) -> Unit,
  onCreateClick: () -> Unit,
  onJoinClick: () -> Unit,
  onStartTvMode: () -> Unit,
  onCastClick: () -> Unit,
  onOpenSettings: () -> Unit,
  compactActions: Boolean = false,
) {
  var menuExpanded by remember { mutableStateOf(false) }

  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = if (compactActions) 0.dp else 16.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Box(modifier = Modifier.weight(1f)) {
      Surface(
        onClick = { menuExpanded = true },
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = CircleShape,
        modifier = Modifier.testTag("album_switcher_button"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.CloudDone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = activeAlbum?.title ?: stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
          )
          Icon(
            imageVector = Icons.Filled.ExpandMore,
            contentDescription = "Switch gathering album",
            modifier = Modifier.size(20.dp),
          )
        }
      }

      DropdownMenu(
        expanded = menuExpanded,
        onDismissRequest = { menuExpanded = false },
      ) {
        albums.forEach { albumItem ->
          DropdownMenuItem(
            text = {
              Column {
                Text(albumItem.title, style = MaterialTheme.typography.titleMedium)
                Text(
                  "${albumItem.gatheringMonth} · Code ${albumItem.inviteCode}",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            },
            onClick = {
              menuExpanded = false
              onSelectAlbum(albumItem.id)
            },
          )
        }
        if (albums.isNotEmpty()) {
          HorizontalDivider()
        }
        DropdownMenuItem(
          text = { Text(stringResource(R.string.action_create_gathering)) },
          leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
          onClick = {
            menuExpanded = false
            onCreateClick()
          },
        )
        DropdownMenuItem(
          text = { Text(stringResource(R.string.action_join_gathering)) },
          leadingIcon = { Icon(Icons.Filled.GroupAdd, contentDescription = null) },
          onClick = {
            menuExpanded = false
            onJoinClick()
          },
        )
      }
    }

    Spacer(Modifier.width(8.dp))

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      if (activeAlbum != null && !compactActions) {
        FilledTonalButton(
          onClick = onStartTvMode,
          shape = CircleShape,
          colors =
            ButtonDefaults.filledTonalButtonColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
          modifier = Modifier.testTag("tv_mode_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.Tv,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(Modifier.width(6.dp))
          Text("TV Mode", style = MaterialTheme.typography.labelLarge)
        }
      }
      if (!compactActions) {
        IconButton(
          onClick = onCastClick,
          modifier = Modifier.size(48.dp).testTag("cast_tv_button"),
        ) {
          Icon(
            imageVector =
              if (isCastConnected) Icons.Filled.CastConnected else Icons.Filled.Cast,
            contentDescription = stringResource(R.string.cd_cast_button),
            tint =
              if (isCastConnected) MaterialTheme.colorScheme.secondary
              else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      IconButton(
        onClick = onOpenSettings,
        modifier = Modifier.size(48.dp).testTag("settings_button"),
      ) {
        Icon(
          imageVector = Icons.Filled.Settings,
          contentDescription = "TV display and account settings",
        )
      }
    }
  }
}

@Composable
private fun GatheringInviteBanner(
  album: GatheringAlbum,
  photoCount: Int,
  onCopyCode: () -> Unit,
  onShareInvite: () -> Unit,
) {
  Card(
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
      ),
    shape = MaterialTheme.shapes.large,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = album.gatheringMonth.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = album.title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = "$photoCount memories · ${album.memberUids.size} family members · Auto-syncing",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column {
            Text(
              text = stringResource(R.string.label_invite_code),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = album.inviteCode,
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(
              onClick = onCopyCode,
              modifier = Modifier.size(48.dp).testTag("copy_invite_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.ContentCopy,
                contentDescription = stringResource(R.string.action_copy_code),
              )
            }
            FilledTonalButton(
              onClick = onShareInvite,
              shape = CircleShape,
              modifier = Modifier.testTag("share_invite_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.Share,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(Modifier.width(6.dp))
              Text("Invite")
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AutoOrganizationFilterStrip(
  availableDays: List<String>,
  availableContributors: List<String>,
  selectedDay: String?,
  selectedMoment: MomentTag?,
  selectedContributor: String?,
  favoritesOnly: Boolean,
  onSelectDay: (String?) -> Unit,
  onSelectMoment: (MomentTag?) -> Unit,
  onSelectContributor: (String?) -> Unit,
  onToggleFavorites: () -> Unit,
  onClearAll: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      FilterChip(
        selected = favoritesOnly,
        onClick = onToggleFavorites,
        leadingIcon = {
          Icon(
            imageVector = if (favoritesOnly) Icons.Filled.Star else Icons.Outlined.StarBorder,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
        },
        label = { Text("TV Highlights") },
        colors =
          FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
          ),
      )

      availableDays.forEach { day ->
        FilterChip(
          selected = selectedDay == day,
          onClick = { onSelectDay(day) },
          label = { Text(day) },
        )
      }

      availableContributors.forEach { contributor ->
        FilterChip(
          selected = selectedContributor == contributor,
          onClick = { onSelectContributor(contributor) },
          label = { Text(contributor) },
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      MomentTag.entries.forEach { tag ->
        FilterChip(
          selected = selectedMoment == tag,
          onClick = { onSelectMoment(tag) },
          label = { Text(tag.label) },
        )
      }
      if (selectedDay != null || selectedMoment != null || selectedContributor != null || favoritesOnly) {
        FilterChip(
          selected = false,
          onClick = onClearAll,
          label = { Text("Reset") },
        )
      }
    }
  }
}

@Composable
private fun PhotoGridCard(
  photo: GatheringPhoto,
  isStarred: Boolean,
  onPhotoClick: () -> Unit,
  onToggleStar: () -> Unit,
) {
  Card(
    shape = MaterialTheme.shapes.medium,
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
      ),
    modifier =
      Modifier.fillMaxWidth()
        .clip(MaterialTheme.shapes.medium)
        .clickable(onClick = onPhotoClick)
        .testTag("photo_card_${photo.id}"),
  ) {
    Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
      EncodedPhotoImage(
        photo = photo,
        contentDescription = photo.caption.ifBlank { "Photo shared by ${photo.uploaderName}" },
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .height(68.dp)
            .align(Alignment.BottomCenter)
            .background(
              Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.68f))
              )
            )
      )
      IconButton(
        onClick = onToggleStar,
        modifier = Modifier.align(Alignment.TopEnd).size(48.dp).testTag("star_photo_${photo.id}"),
      ) {
        Surface(
          color = Color.Black.copy(alpha = 0.42f),
          shape = CircleShape,
          modifier = Modifier.size(32.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = if (isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
              contentDescription = if (isStarred) "Unstar photo" else "Star photo for TV",
              tint = if (isStarred) Color(0xFFEAB965) else Color.White,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }
      Column(
        modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Text(
          text = photo.uploaderName,
          style = MaterialTheme.typography.labelLarge,
          color = Color.White,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = photo.caption.ifBlank { photo.parsedMomentTag.label },
          style = MaterialTheme.typography.labelSmall,
          color = Color.White.copy(alpha = 0.85f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun NoAlbumsEmptyState(
  onCreateClick: () -> Unit,
  onJoinClick: () -> Unit,
) {
  Box(
    modifier = Modifier.fillMaxSize().padding(24.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Image(
        painter = painterResource(id = R.drawable.img_empty_album),
        contentDescription = stringResource(R.string.cd_empty_album),
        contentScale = ContentScale.Crop,
        modifier =
          Modifier.fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(MaterialTheme.shapes.large),
      )
      Text(
        text = stringResource(R.string.empty_album_title),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
      )
      Text(
        text = stringResource(R.string.empty_album_body),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Button(
          onClick = onCreateClick,
          shape = MaterialTheme.shapes.large,
          modifier = Modifier.weight(1f).height(52.dp).testTag("empty_create_album_button"),
        ) {
          Text("Create Album")
        }
        OutlinedButton(
          onClick = onJoinClick,
          shape = MaterialTheme.shapes.large,
          modifier = Modifier.weight(1f).height(52.dp).testTag("empty_join_album_button"),
        ) {
          Text("Join Code")
        }
      }
    }
  }
}

@Composable
private fun NoPhotosEmptyState(
  onSendPhotosClick: () -> Unit,
) {
  Card(
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
      ),
    shape = MaterialTheme.shapes.large,
    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Image(
        painter = painterResource(id = R.drawable.img_empty_album),
        contentDescription = stringResource(R.string.cd_empty_album),
        contentScale = ContentScale.Crop,
        modifier =
          Modifier.fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(MaterialTheme.shapes.medium),
      )
      Text(
        text = stringResource(R.string.empty_photos_title),
        style = MaterialTheme.typography.headlineSmall,
      )
      Text(
        text = stringResource(R.string.empty_photos_body),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Button(
        onClick = onSendPhotosClick,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("empty_send_photos_button"),
      ) {
        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.action_send_photos))
      }
    }
  }
}

@Composable
private fun ErrorBannerCard(
  message: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = MaterialTheme.colorScheme.errorContainer,
    shape = MaterialTheme.shapes.medium,
    modifier = modifier.fillMaxWidth(),
  ) {
    Text(
      text = message,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onErrorContainer,
      modifier = Modifier.padding(16.dp),
    )
  }
}
