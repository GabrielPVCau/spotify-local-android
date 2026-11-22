package com.spotifylocal.android.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ScanResult(
    val allTracks: List<Track>,
    val playlists: List<Playlist>,
    val folderName: String
)

class MusicScanner(
    private val context: Context,
    private val syncManager: SyncManager
) {
    companion object {
        private val AUDIO_EXTENSIONS = setOf(
            "mp3", "m4a", "flac", "wav", "ogg", "opus", "aac", "wma", "webm"
        )
        const val PLAYLIST_LIKED_ID = "__liked__"
        const val PLAYLIST_ALL_ID = "__all__"
        const val PLAYLIST_ROOT_ID = "root"
    }

    suspend fun scanLibrary(rootTreeUri: Uri): ScanResult = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, rootTreeUri)
            ?: return@withContext ScanResult(emptyList(), emptyList(), "")

        val mergedSync = syncManager.readMergedSyncData(rootTreeUri)
        val allTracks = mutableListOf<Track>()
        val folderPlaylists = mutableListOf<Playlist>()
        val rootTracks = mutableListOf<Track>()

        val children = root.listFiles().sortedBy { it.name?.lowercase() ?: "" }

        for (child in children) {
            val name = child.name ?: continue
            if (name == SyncManager.SYNC_DIR_NAME || name.startsWith(".")) continue

            if (child.isDirectory) {
                val playlistId = name
                val tracksInFolder = mutableListOf<Track>()
                collectAudioFiles(
                    dir = child,
                    relPrefix = name,
                    playlistId = playlistId,
                    playlistName = name,
                    mergedSync = mergedSync,
                    out = tracksInFolder
                )
                if (tracksInFolder.isNotEmpty()) {
                    val meta = mergedSync.playlists[playlistId]
                    folderPlaylists += Playlist(
                        id = playlistId,
                        name = name,
                        tracks = tracksInFolder,
                        pinned = meta?.pinned ?: false,
                        sortOrder = meta?.sortOrder ?: 0
                    )
                    allTracks += tracksInFolder
                }
            } else if (child.isFile && isAudioFile(name)) {
                val track = buildTrack(
                    file = child,
                    relPath = name,
                    playlistId = PLAYLIST_ROOT_ID,
                    playlistName = root.name ?: "Músicas",
                    mergedSync = mergedSync
                )
                rootTracks += track
                allTracks += track
            }
        }

        if (rootTracks.isNotEmpty()) {
            val meta = mergedSync.playlists[PLAYLIST_ROOT_ID]
            folderPlaylists += Playlist(
                id = PLAYLIST_ROOT_ID,
                name = root.name ?: "Pasta principal",
                tracks = rootTracks,
                pinned = meta?.pinned ?: false,
                sortOrder = meta?.sortOrder ?: 0
            )
        }

        val sortedFolders = folderPlaylists.sortedWith(
            compareByDescending<Playlist> { it.pinned }
                .thenBy { it.sortOrder }
                .thenBy { it.name.lowercase() }
        )

        val likedTracks = allTracks.filter { it.liked }
        val playlists = buildList {
            add(
                Playlist(
                    id = PLAYLIST_LIKED_ID,
                    name = "Músicas Curtidas",
                    tracks = likedTracks,
                    pinned = true,
                    isSpecial = true
                )
            )
            add(
                Playlist(
                    id = PLAYLIST_ALL_ID,
                    name = "Todas as Músicas",
                    tracks = allTracks,
                    pinned = false,
                    isSpecial = true
                )
            )
            addAll(sortedFolders)
        }

        ScanResult(
            allTracks = allTracks,
            playlists = playlists,
            folderName = root.name ?: "Música"
        )
    }

    private fun collectAudioFiles(
        dir: DocumentFile,
        relPrefix: String,
        playlistId: String,
        playlistName: String,
        mergedSync: MergedSyncData,
        out: MutableList<Track>
    ) {
        val files = dir.listFiles().sortedBy { it.name?.lowercase() ?: "" }
        for (file in files) {
            val name = file.name ?: continue
            if (name.startsWith(".")) continue
            val relPath = "$relPrefix/$name"
            if (file.isDirectory) {
                collectAudioFiles(file, relPath, playlistId, playlistName, mergedSync, out)
            } else if (file.isFile && isAudioFile(name)) {
                out += buildTrack(file, relPath, playlistId, playlistName, mergedSync)
            }
        }
    }

    private fun isAudioFile(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in AUDIO_EXTENSIONS
    }

    private fun buildTrack(
        file: DocumentFile,
        relPath: String,
        playlistId: String,
        playlistName: String,
        mergedSync: MergedSyncData
    ): Track {
        val fileName = file.name ?: relPath
        val baseName = fileName.substringBeforeLast('.')
        val (parsedArtist, parsedTitle) = parseFilename(baseName)

        var metaTitle: String? = null
        var metaArtist: String? = null
        var metaAlbum: String? = null
        var durationMs = 0L

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, file.uri)
            metaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.trim()?.takeIf { it.isNotEmpty() }
            metaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.trim()?.takeIf { it.isNotEmpty() }
            metaAlbum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.trim()?.takeIf { it.isNotEmpty() }
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            // Fall back to filename parsing
        } finally {
            runCatching { retriever.release() }
        }

        val stats = mergedSync.tracks[relPath]
        return Track(
            relPath = relPath,
            uri = file.uri,
            title = metaTitle ?: parsedTitle,
            artist = metaArtist ?: parsedArtist,
            album = metaAlbum ?: playlistName,
            durationMs = durationMs,
            playlistId = playlistId,
            playlistName = playlistName,
            lastModified = file.lastModified(),
            playCount = stats?.playCount ?: 0,
            lastPlayedAt = stats?.lastPlayedAt,
            liked = stats?.liked ?: false,
            likedUpdatedAt = stats?.likedUpdatedAt
        )
    }

    private fun parseFilename(baseName: String): Pair<String, String> {
        val cleaned = baseName
            .replace(Regex("^\\d{1,3}\\s*[-._)]\\s*"), "")
            .trim()
        val separators = listOf(" - ", " – ", " — ")
        for (sep in separators) {
            val idx = cleaned.indexOf(sep)
            if (idx > 0 && idx + sep.length < cleaned.length) {
                val artist = cleaned.substring(0, idx).trim()
                val title = cleaned.substring(idx + sep.length).trim()
                if (artist.isNotEmpty() && title.isNotEmpty()) {
                    return artist to title
                }
            }
        }
        return "Artista desconhecido" to cleaned.ifEmpty { baseName }
    }
}
