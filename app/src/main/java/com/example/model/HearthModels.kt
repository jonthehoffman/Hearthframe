package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class MomentTag(val label: String) {
  DINNER_TABLE("Dinner Table"),
  PORTRAITS("Family Portraits"),
  CANDID_FUN("Candid & Kids"),
  OUTDOORS("Autumn Outdoors"),
  KITCHEN_PIE("Kitchen & Pie"),
  FIRESIDE("Fireside & Games"),
  OTHER("Gathering Moment");

  companion object {
    fun fromRaw(raw: String?): MomentTag =
      entries.firstOrNull { it.name == raw } ?: OTHER
  }
}

@Keep
@IgnoreExtraProperties
data class GatheringAlbum(
  @DocumentId val id: String = "",
  val hostUserId: String = "",
  val hostName: String = "",
  val title: String = "",
  val gatheringMonth: String = "",
  val inviteCode: String = "",
  val memberUids: List<String> = emptyList(),
  val photoCount: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class AlbumInvite(
  @DocumentId val id: String = "",
  val albumId: String = "",
  val hostUserId: String = "",
  val albumTitle: String = "",
  val gatheringMonth: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class GatheringPhoto(
  @DocumentId val id: String = "",
  val albumId: String = "",
  val uploaderId: String = "",
  val uploaderName: String = "",
  val caption: String = "",
  val momentTag: String = MomentTag.OTHER.name,
  val dayLabel: String = "",
  val takenAtMillis: Long = 0L,
  val imageBase64: String = "",
  val aspectRatio: Double = 1.33,
  val starredByUids: List<String> = emptyList(),
  val memberUids: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val parsedMomentTag: MomentTag
    get() = MomentTag.fromRaw(momentTag)
}

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>

  data class Success<T>(val data: T) : UiState<T>

  data class Error(val message: String) : UiState<Nothing>
}

data class SyncUploadStatus(
  val totalCount: Int = 0,
  val completedCount: Int = 0,
  val failedCount: Int = 0,
  val activeSync: Boolean = false,
)

data class TvSlideshowSettings(
  val slideDurationSeconds: Int = 8,
  val kenBurnsMotion: Boolean = true,
  val showOverlayCaptions: Boolean = true,
  val shuffleOrder: Boolean = false,
  val favoritesOnly: Boolean = false,
  val launchIntoTvMode: Boolean = false,
  val autoProjectExternalDisplay: Boolean = true,
)
