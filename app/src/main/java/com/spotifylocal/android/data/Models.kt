package com.spotifylocal.android.data

import android.net.Uri
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class Track(
    val relPath: String,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val playlistId: String,
    val playlistName: String,
    val lastModified: Long,
    val playCount: Int = 0,
    val lastPlayedAt: String? = null,
    val liked: Boolean = false,
    val likedUpdatedAt: String? = null
)

data class Playlist(
    val id: String,
    val name: String,
    val tracks: List<Track>,
    val pinned: Boolean = false,
    val sortOrder: Int = 0,
    val isSpecial: Boolean = false
)

enum class SortOption(val label: String) {
    DEFAULT("Ordem padrão"),
    MOST_PLAYED("Mais reproduzidas"),
    LEAST_PLAYED("Menos reproduzidas"),
    TITLE_ASC("Título A-Z"),
    ARTIST_ASC("Artista A-Z"),
    RECENTLY_ADDED("Adicionadas recentemente");

    companion object {
        fun fromName(name: String?): SortOption =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}

enum class RepeatMode {
    OFF, ALL, ONE
}

@Serializable
data class SyncFile(
    val version: Int = 1,
    @SerialName("device_id") val deviceId: String = "android",
    @SerialName("updated_at") val updatedAt: String = "",
    val tracks: Map<String, SyncTrackEntry> = emptyMap(),
    val playlists: Map<String, SyncPlaylistEntry> = emptyMap()
)

@Serializable
data class SyncTrackEntry(
    @SerialName("play_count") val playCount: Int = 0,
    @SerialName("last_played_at") val lastPlayedAt: String? = null,
    val liked: Boolean = false,
    @SerialName("liked_updated_at") val likedUpdatedAt: String? = null
)

@Serializable
data class SyncPlaylistEntry(
    val pinned: Boolean = false,
    @SerialName("sort_order") val sortOrder: Int? = null,
    @SerialName("updated_at") val updatedAt: String = ""
)

data class MergedTrackStats(
    val playCount: Int = 0,
    val lastPlayedAt: String? = null,
    val liked: Boolean = false,
    val likedUpdatedAt: String? = null
)

data class MergedPlaylistMeta(
    val pinned: Boolean = false,
    val sortOrder: Int = 0,
    val updatedAt: String = ""
)

data class MergedSyncData(
    val tracks: Map<String, MergedTrackStats> = emptyMap(),
    val playlists: Map<String, MergedPlaylistMeta> = emptyMap()
)
