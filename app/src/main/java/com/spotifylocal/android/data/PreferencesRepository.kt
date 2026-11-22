package com.spotifylocal.android.data

import android.content.Context
import android.net.Uri
import java.util.UUID

class PreferencesRepository(context: Context) {
    private val prefs = context.getSharedPreferences("spotify_local_prefs", Context.MODE_PRIVATE)

    fun getOrCreateDeviceId(): String {
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val generated = "android-${UUID.randomUUID().toString().take(8)}"
        prefs.edit().putString(KEY_DEVICE_ID, generated).apply()
        return generated
    }

    fun getMusicFolderUri(): Uri? {
        val raw = prefs.getString(KEY_MUSIC_FOLDER_URI, null) ?: return null
        return runCatching { Uri.parse(raw) }.getOrNull()
    }

    fun setMusicFolderUri(uri: Uri) {
        prefs.edit().putString(KEY_MUSIC_FOLDER_URI, uri.toString()).apply()
    }

    fun getPinnedSortOption(): SortOption {
        return SortOption.fromName(prefs.getString(KEY_PINNED_SORT, SortOption.DEFAULT.name))
    }

    fun setPinnedSortOption(option: SortOption) {
        prefs.edit().putString(KEY_PINNED_SORT, option.name).apply()
    }

    companion object {
        private const val KEY_DEVICE_ID = "sync_device_id"
        private const val KEY_MUSIC_FOLDER_URI = "music_folder_uri"
        private const val KEY_PINNED_SORT = "pinned_sort_option"
    }
}
