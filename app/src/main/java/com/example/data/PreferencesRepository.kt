package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.TvSlideshowSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.hearthDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "hearthframe_prefs")

class PreferencesRepository(private val context: Context) {

  private object Keys {
    val SELECTED_ALBUM_ID = stringPreferencesKey("selected_album_id")
    val SLIDE_DURATION_SEC = intPreferencesKey("tv_slide_duration_sec")
    val KEN_BURNS_MOTION = booleanPreferencesKey("tv_ken_burns_motion")
    val SHOW_OVERLAY_CAPTIONS = booleanPreferencesKey("tv_show_captions")
    val SHUFFLE_ORDER = booleanPreferencesKey("tv_shuffle_order")
    val FAVORITES_ONLY = booleanPreferencesKey("tv_favorites_only")
    val LAUNCH_INTO_TV = booleanPreferencesKey("tv_launch_on_open")
    val AUTO_PROJECT_EXTERNAL = booleanPreferencesKey("tv_auto_project_external")
  }

  val selectedAlbumId: Flow<String?> =
    context.hearthDataStore.data.map { prefs -> prefs[Keys.SELECTED_ALBUM_ID] }

  val tvSettings: Flow<TvSlideshowSettings> =
    context.hearthDataStore.data.map { prefs ->
      TvSlideshowSettings(
        slideDurationSeconds = prefs[Keys.SLIDE_DURATION_SEC] ?: 8,
        kenBurnsMotion = prefs[Keys.KEN_BURNS_MOTION] ?: true,
        showOverlayCaptions = prefs[Keys.SHOW_OVERLAY_CAPTIONS] ?: true,
        shuffleOrder = prefs[Keys.SHUFFLE_ORDER] ?: false,
        favoritesOnly = prefs[Keys.FAVORITES_ONLY] ?: false,
        launchIntoTvMode = prefs[Keys.LAUNCH_INTO_TV] ?: false,
        autoProjectExternalDisplay = prefs[Keys.AUTO_PROJECT_EXTERNAL] ?: true,
      )
    }

  suspend fun setSelectedAlbumId(albumId: String) {
    context.hearthDataStore.edit { prefs -> prefs[Keys.SELECTED_ALBUM_ID] = albumId }
  }

  suspend fun updateTvSettings(transform: (TvSlideshowSettings) -> TvSlideshowSettings) {
    context.hearthDataStore.edit { prefs ->
      val current =
        TvSlideshowSettings(
          slideDurationSeconds = prefs[Keys.SLIDE_DURATION_SEC] ?: 8,
          kenBurnsMotion = prefs[Keys.KEN_BURNS_MOTION] ?: true,
          showOverlayCaptions = prefs[Keys.SHOW_OVERLAY_CAPTIONS] ?: true,
          shuffleOrder = prefs[Keys.SHUFFLE_ORDER] ?: false,
          favoritesOnly = prefs[Keys.FAVORITES_ONLY] ?: false,
          launchIntoTvMode = prefs[Keys.LAUNCH_INTO_TV] ?: false,
          autoProjectExternalDisplay = prefs[Keys.AUTO_PROJECT_EXTERNAL] ?: true,
        )
      val updated = transform(current)
      prefs[Keys.SLIDE_DURATION_SEC] = updated.slideDurationSeconds
      prefs[Keys.KEN_BURNS_MOTION] = updated.kenBurnsMotion
      prefs[Keys.SHOW_OVERLAY_CAPTIONS] = updated.showOverlayCaptions
      prefs[Keys.SHUFFLE_ORDER] = updated.shuffleOrder
      prefs[Keys.FAVORITES_ONLY] = updated.favoritesOnly
      prefs[Keys.LAUNCH_INTO_TV] = updated.launchIntoTvMode
      prefs[Keys.AUTO_PROJECT_EXTERNAL] = updated.autoProjectExternalDisplay
    }
  }
}
