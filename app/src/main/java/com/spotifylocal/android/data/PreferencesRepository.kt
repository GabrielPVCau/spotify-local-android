package com.spotifylocal.android.data

import android.content.Context
import android.net.Uri

class PreferencesRepository(context: Context) {
    private val prefs = context.getSharedPreferences("spotify_local_prefs", Context.MODE_PRIVATE)

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
        private const val KEY_MUSIC_FOLDER_URI = "music_folder_uri"
        private const val KEY_PINNED_SORT = "pinned_sort_option"
    }
}
