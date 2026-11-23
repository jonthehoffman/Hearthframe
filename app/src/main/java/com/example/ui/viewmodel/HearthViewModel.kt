package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HearthRepository
import com.example.data.PhotoCodec
import com.example.data.PreferencesRepository
import com.example.model.GatheringAlbum
import com.example.model.GatheringPhoto
import com.example.model.MomentTag
import com.example.model.SyncUploadStatus
import com.example.model.TvSlideshowSettings
import com.example.model.UiState
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HearthViewModel(
  private val repository: HearthRepository,
  private val preferencesRepository: PreferencesRepository,
  private val currentUserId: String,
  private val currentUserName: String,
) : ViewModel() {

  private val _selectedDayFilter = MutableStateFlow<String?>(null)
  val selectedDayFilter: StateFlow<String?> = _selectedDayFilter.asStateFlow()

  private val _selectedMomentFilter = MutableStateFlow<MomentTag?>(null)
  val selectedMomentFilter: StateFlow<MomentTag?> = _selectedMomentFilter.asStateFlow()

  private val _selectedContributorFilter = MutableStateFlow<String?>(null)
  val selectedContributorFilter: StateFlow<String?> = _selectedContributorFilter.asStateFlow()

  private val _favoritesOnlyFilter = MutableStateFlow(false)
  val favoritesOnlyFilter: StateFlow<Boolean> = _favoritesOnlyFilter.asStateFlow()

  private val _syncStatus = MutableStateFlow(SyncUploadStatus())
  val syncStatus: StateFlow<SyncUploadStatus> = _syncStatus.asStateFlow()

  private val _isTvModeActive = MutableStateFlow(false)
  val isTvModeActive: StateFlow<Boolean> = _isTvModeActive.asStateFlow()

  private var didApplyAutoLaunchTv = false
  private var lastSyncedMemberKey: String? = null

  private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
  val events = _events.asSharedFlow()

  val tvSettings: StateFlow<TvSlideshowSettings> =
    preferencesRepository.tvSettings.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = TvSlideshowSettings(),
    )

  val albumsState: StateFlow<UiState<List<GatheringAlbum>>> =
    repository
      .observeMyAlbums(currentUserId)
      .map<List<GatheringAlbum>, UiState<List<GatheringAlbum>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing albums", rootError)
        emit(UiState.Error(rootError.localizedMessage ?: "Failed to load family gatherings"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val activeAlbum: StateFlow<GatheringAlbum?> =
    combine(albumsState, preferencesRepository.selectedAlbumId) { state, savedId ->
      val albums = (state as? UiState.Success)?.data.orEmpty()
      albums.firstOrNull { it.id == savedId } ?: albums.firstOrNull()
    }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = null,
      )

  val photosState: StateFlow<UiState<List<GatheringPhoto>>> =
    activeAlbum
      .flatMapLatest { album ->
        if (album == null) {
          flowOf(UiState.Success(emptyList()))
        } else {
          repository
            .observeAlbumPhotos(album.id, currentUserId)
            .map<List<GatheringPhoto>, UiState<List<GatheringPhoto>>> { photos ->
              // Self-healing member propagation when new family members join
              val memberKey = "${album.id}:${album.memberUids.sorted().joinToString(",")}"
              if (memberKey != lastSyncedMemberKey && photos.any { !it.memberUids.containsAll(album.memberUids) }) {
                lastSyncedMemberKey = memberKey
                viewModelScope.launch { repository.syncMissingMemberUids(album, photos) }
              }
              UiState.Success(photos)
            }
            .catch { error ->
              val firestoreError =
                generateSequence(error) { it.cause }
                  .filterIsInstance<FirebaseFirestoreException>()
                  .firstOrNull()
              if (firestoreError == null && error is CancellationException) throw error
              val rootError = firestoreError ?: error
              Log.w(TAG, "Error observing photos", rootError)
              emit(UiState.Error(rootError.localizedMessage ?: "Failed to sync family photos"))
            }
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  init {
    viewModelScope.launch {
      repository.ensureUserProfile(currentUserName)
    }
    viewModelScope.launch {
      combine(tvSettings, activeAlbum) { settings, album -> settings to album }
        .collect { (settings, album) ->
          if (!didApplyAutoLaunchTv && album != null) {
            didApplyAutoLaunchTv = true
            if (settings.launchIntoTvMode) {
              _isTvModeActive.value = true
            }
          }
        }
    }
  }

  fun selectAlbum(albumId: String) {
    viewModelScope.launch {
      preferencesRepository.setSelectedAlbumId(albumId)
      clearFilters()
    }
  }

  fun setDayFilter(dayLabel: String?) {
    _selectedDayFilter.value = if (_selectedDayFilter.value == dayLabel) null else dayLabel
  }

  fun setMomentFilter(tag: MomentTag?) {
    _selectedMomentFilter.value = if (_selectedMomentFilter.value == tag) null else tag
  }

  fun setContributorFilter(name: String?) {
    _selectedContributorFilter.value = if (_selectedContributorFilter.value == name) null else name
  }

  fun toggleFavoritesFilter() {
    _favoritesOnlyFilter.value = !_favoritesOnlyFilter.value
  }

  fun clearFilters() {
    _selectedDayFilter.value = null
    _selectedMomentFilter.value = null
    _selectedContributorFilter.value = null
    _favoritesOnlyFilter.value = false
  }

  fun enterTvMode() {
    _isTvModeActive.value = true
  }

  fun exitTvMode() {
    _isTvModeActive.value = false
  }

  fun updateTvSettings(transform: (TvSlideshowSettings) -> TvSlideshowSettings) {
    viewModelScope.launch { preferencesRepository.updateTvSettings(transform) }
  }

  fun createAlbum(
    title: String,
    gatheringMonth: String,
    onDone: (Boolean) -> Unit = {},
  ) {
    viewModelScope.launch {
      val result = repository.createAlbum(title, gatheringMonth, currentUserName)
      result
        .onSuccess { created ->
          preferencesRepository.setSelectedAlbumId(created.id)
          _events.tryEmit("Created '${created.title}' · Share code ${created.inviteCode}")
          onDone(true)
        }
        .onFailure { err ->
          _events.tryEmit(err.localizedMessage ?: "Couldn't create gathering")
          onDone(false)
        }
    }
  }

  fun joinAlbumByCode(code: String, onDone: (Boolean) -> Unit = {}) {
    viewModelScope.launch {
      val result = repository.joinAlbumByInviteCode(code)
      result
        .onSuccess { albumId ->
          preferencesRepository.setSelectedAlbumId(albumId)
          clearFilters()
          _events.tryEmit("Joined family gathering! Syncing photos now.")
          onDone(true)
        }
        .onFailure { err ->
          _events.tryEmit(err.localizedMessage ?: "Couldn't join with that code")
          onDone(false)
        }
    }
  }

  fun sendPhotos(
    context: Context,
    uris: List<Uri>,
    momentTag: MomentTag,
    caption: String,
  ) {
    val album = activeAlbum.value
    if (album == null) {
      _events.tryEmit("Create or join a gathering album first.")
      return
    }
    if (uris.isEmpty()) return

    viewModelScope.launch {
      _syncStatus.value =
        SyncUploadStatus(
          totalCount = uris.size,
          completedCount = 0,
          failedCount = 0,
          activeSync = true,
        )
      var succeeded = 0
      var failed = 0
      uris.forEachIndexed { index, uri ->
        val encoded = PhotoCodec.compressAndEncode(context, uri)
        val uploadResult =
          encoded.fold(
            onSuccess = { photoPayload ->
              repository.uploadPhoto(
                album = album,
                uploaderName = currentUserName,
                encoded = photoPayload,
                caption = caption,
                momentTag = momentTag,
              )
            },
            onFailure = { Result.failure(it) },
          )
        if (uploadResult.isSuccess) {
          succeeded++
        } else {
          failed++
        }
        _syncStatus.value =
          SyncUploadStatus(
            totalCount = uris.size,
            completedCount = index + 1,
            failedCount = failed,
            activeSync = index + 1 < uris.size,
          )
      }
      _syncStatus.value = SyncUploadStatus(activeSync = false)
      val message =
        when {
          failed == 0 && succeeded == 1 -> "1 photo synced to ${album.title}"
          failed == 0 -> "$succeeded photos synced to ${album.title}"
          succeeded > 0 -> "Synced $succeeded photos ($failed couldn't be processed)"
          else -> "Couldn't sync selected photos. Please try again."
        }
      _events.tryEmit(message)
    }
  }

  fun createCameraCaptureUri(context: Context): Uri {
    val capturesDir = File(context.cacheDir, "captures").apply { mkdirs() }
    val captureFile = File(capturesDir, "hearth_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
      context,
      "${context.packageName}.fileprovider",
      captureFile,
    )
  }

  fun toggleStar(photo: GatheringPhoto) {
    val album = activeAlbum.value ?: return
    viewModelScope.launch {
      repository.toggleStarPhoto(album.id, photo, currentUserId).onFailure {
        _events.tryEmit(it.localizedMessage ?: "Couldn't update favorite")
      }
    }
  }

  fun updatePhotoDetails(photoId: String, caption: String, momentTag: MomentTag) {
    val album = activeAlbum.value ?: return
    viewModelScope.launch {
      repository.updatePhotoDetails(album.id, photoId, caption, momentTag)
        .onSuccess { _events.tryEmit("Photo details updated") }
        .onFailure { _events.tryEmit(it.localizedMessage ?: "Couldn't update photo") }
    }
  }

  fun deletePhoto(photoId: String) {
    val album = activeAlbum.value ?: return
    viewModelScope.launch {
      repository.deletePhoto(album, photoId)
        .onSuccess { _events.tryEmit("Removed photo from album") }
        .onFailure { _events.tryEmit(it.localizedMessage ?: "Couldn't remove photo") }
    }
  }

  fun emitMessage(message: String) {
    _events.tryEmit(message)
  }

  private companion object {
    const val TAG = "HearthVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
