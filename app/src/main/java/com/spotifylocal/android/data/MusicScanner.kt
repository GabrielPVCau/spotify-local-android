package com.spotifylocal.android.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ScanResult(
    val allTracks: List<Track>,
    val playlists: List<Playlist>,
    val folderName: String,
    val cachedLibrary: CachedLibrary? = null
)

private data class DocEntry(
    val docId: String,
    val name: String,
    val mimeType: String,
    val lastModified: Long,
    val sizeBytes: Long
) {
    val isDirectory: Boolean
        get() = mimeType == DocumentsContract.Document.MIME_TYPE_DIR
}

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

        private val PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_SIZE
        )
    }

    suspend fun buildFromCache(
        rootTreeUri: Uri,
        cached: CachedLibrary
    ): ScanResult = withContext(Dispatchers.IO) {
        val mergedSync = syncManager.readMergedSyncData(rootTreeUri)
        val tracks = cached.tracks.map { entry ->
            val stats = mergedSync.tracks[entry.relPath]
            Track(
                relPath = entry.relPath,
                uri = Uri.parse(entry.uriString),
                title = entry.title,
                artist = entry.artist,
                album = entry.album,
                durationMs = entry.durationMs,
                playlistId = entry.playlistId,
                playlistName = entry.playlistName,
                lastModified = entry.lastModified,
                playCount = stats?.playCount ?: 0,
                lastPlayedAt = stats?.lastPlayedAt,
                liked = stats?.liked ?: false,
                likedUpdatedAt = stats?.likedUpdatedAt
            )
        }
        buildScanResult(tracks, mergedSync, cached.folderName, cached)
    }

    suspend fun scanLibrary(
        rootTreeUri: Uri,
        cachedTracksByRelPath: Map<String, CachedTrack> = emptyMap()
    ): ScanResult = withContext(Dispatchers.IO) {
        val rootDocId = runCatching { DocumentsContract.getTreeDocumentId(rootTreeUri) }.getOrNull()
            ?: return@withContext ScanResult(emptyList(), emptyList(), "")

        val folderName = DocumentFile.fromTreeUri(context, rootTreeUri)?.name ?: "Música"
        val mergedSync = syncManager.readMergedSyncData(rootTreeUri)

        val allTracks = mutableListOf<Track>()
        val newCachedTracks = mutableListOf<CachedTrack>()

        val children = queryChildren(rootTreeUri, rootDocId).sortedBy { it.name.lowercase() }

        for (child in children) {
            val name = child.name
            if (name == SyncManager.SYNC_DIR_NAME || name.startsWith(".")) continue

            if (child.isDirectory) {
                collectAudioFiles(
                    rootTreeUri = rootTreeUri,
                    dirDocId = child.docId,
                    relPrefix = name,
                    playlistId = name,
                    playlistName = name,
                    mergedSync = mergedSync,
                    cachedTracksByRelPath = cachedTracksByRelPath,
                    tracksOut = allTracks,
                    cacheOut = newCachedTracks
                )
            } else if (isAudioFile(name)) {
                val fileUri = DocumentsContract.buildDocumentUriUsingTree(rootTreeUri, child.docId)
                val (track, cachedTrack) = buildTrack(
                    fileUri = fileUri,
                    fileName = name,
                    relPath = name,
                    playlistId = PLAYLIST_ROOT_ID,
                    playlistName = folderName,
                    lastModified = child.lastModified,
                    sizeBytes = child.sizeBytes,
                    mergedSync = mergedSync,
                    cached = cachedTracksByRelPath[name]
                )
                allTracks += track
                newCachedTracks += cachedTrack
            }
        }

        val cachedLibrary = CachedLibrary(
            folderUri = rootTreeUri.toString(),
            folderName = folderName,
            tracks = newCachedTracks
        )
        buildScanResult(allTracks, mergedSync, folderName, cachedLibrary)
    }

    private fun buildScanResult(
        allTracks: List<Track>,
        mergedSync: MergedSyncData,
        folderName: String,
        cachedLibrary: CachedLibrary?
    ): ScanResult {
        val groupedByPlaylist = allTracks.groupBy { it.playlistId }
        val folderPlaylists = groupedByPlaylist.map { (playlistId, tracks) ->
            val playlistName = tracks.firstOrNull()?.playlistName
                ?: if (playlistId == PLAYLIST_ROOT_ID) folderName else playlistId
            val meta = mergedSync.playlists[playlistId]
            Playlist(
                id = playlistId,
                name = playlistName,
                tracks = tracks,
                pinned = meta?.pinned ?: false,
                sortOrder = meta?.sortOrder ?: 0
            )
        }.sortedWith(
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
            addAll(folderPlaylists)
        }

        return ScanResult(
            allTracks = allTracks,
            playlists = playlists,
            folderName = folderName,
            cachedLibrary = cachedLibrary
        )
    }

    private fun queryChildren(rootTreeUri: Uri, parentDocId: String): List<DocEntry> {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(rootTreeUri, parentDocId)
        val result = mutableListOf<DocEntry>()
        runCatching {
            context.contentResolver.query(childrenUri, PROJECTION, null, null, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val modIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                val sizeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)

                while (cursor.moveToNext()) {
                    val docId = if (idIdx >= 0) cursor.getString(idIdx) else continue
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx) else continue
                    if (docId.isNullOrBlank() || name.isNullOrBlank()) continue
                    val mime = if (mimeIdx >= 0) cursor.getString(mimeIdx) ?: "" else ""
                    val lastMod = if (modIdx >= 0 && !cursor.isNull(modIdx)) cursor.getLong(modIdx) else 0L
                    val size = if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) cursor.getLong(sizeIdx) else 0L
                    result += DocEntry(docId, name, mime, lastMod, size)
                }
            }
        }
        return result
    }

    private fun collectAudioFiles(
        rootTreeUri: Uri,
        dirDocId: String,
        relPrefix: String,
        playlistId: String,
        playlistName: String,
        mergedSync: MergedSyncData,
        cachedTracksByRelPath: Map<String, CachedTrack>,
        tracksOut: MutableList<Track>,
        cacheOut: MutableList<CachedTrack>
    ) {
        val entries = queryChildren(rootTreeUri, dirDocId).sortedBy { it.name.lowercase() }
        for (entry in entries) {
            val name = entry.name
            if (name.startsWith(".")) continue
            val relPath = "$relPrefix/$name"
            if (entry.isDirectory) {
                collectAudioFiles(
                    rootTreeUri = rootTreeUri,
                    dirDocId = entry.docId,
                    relPrefix = relPath,
                    playlistId = playlistId,
                    playlistName = playlistName,
                    mergedSync = mergedSync,
                    cachedTracksByRelPath = cachedTracksByRelPath,
                    tracksOut = tracksOut,
                    cacheOut = cacheOut
                )
            } else if (isAudioFile(name)) {
                val fileUri = DocumentsContract.buildDocumentUriUsingTree(rootTreeUri, entry.docId)
                val (track, cachedTrack) = buildTrack(
                    fileUri = fileUri,
                    fileName = name,
                    relPath = relPath,
                    playlistId = playlistId,
                    playlistName = playlistName,
                    lastModified = entry.lastModified,
                    sizeBytes = entry.sizeBytes,
                    mergedSync = mergedSync,
                    cached = cachedTracksByRelPath[relPath]
                )
                tracksOut += track
                cacheOut += cachedTrack
            }
        }
    }

    private fun isAudioFile(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in AUDIO_EXTENSIONS
    }

    private fun buildTrack(
        fileUri: Uri,
        fileName: String,
        relPath: String,
        playlistId: String,
        playlistName: String,
        lastModified: Long,
        sizeBytes: Long,
        mergedSync: MergedSyncData,
        cached: CachedTrack?
    ): Pair<Track, CachedTrack> {
        val title: String
        val artist: String
        val album: String
        val durationMs: Long

        if (cached != null && cached.lastModified == lastModified && cached.sizeBytes == sizeBytes) {
            title = cached.title
            artist = cached.artist
            album = cached.album
            durationMs = cached.durationMs
        } else {
            val baseName = fileName.substringBeforeLast('.')
            val (parsedArtist, parsedTitle) = parseFilename(baseName)

            var metaTitle: String? = null
            var metaArtist: String? = null
            var metaAlbum: String? = null
            var metaDurationMs = 0L

            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, fileUri)
                metaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    ?.trim()?.takeIf { it.isNotEmpty() }
                metaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?.trim()?.takeIf { it.isNotEmpty() }
                metaAlbum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                    ?.trim()?.takeIf { it.isNotEmpty() }
                metaDurationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
            } catch (_: Exception) {
                // Fall back to filename parsing
            } finally {
                runCatching { retriever.release() }
            }

            title = metaTitle ?: parsedTitle
            artist = metaArtist ?: parsedArtist
            album = metaAlbum ?: playlistName
            durationMs = metaDurationMs
        }

        val stats = mergedSync.tracks[relPath]
        val track = Track(
            relPath = relPath,
            uri = fileUri,
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            playlistId = playlistId,
            playlistName = playlistName,
            lastModified = lastModified,
            playCount = stats?.playCount ?: 0,
            lastPlayedAt = stats?.lastPlayedAt,
            liked = stats?.liked ?: false,
            likedUpdatedAt = stats?.likedUpdatedAt
        )
        val newCached = CachedTrack(
            relPath = relPath,
            uriString = fileUri.toString(),
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            playlistId = playlistId,
            playlistName = playlistName,
            lastModified = lastModified,
            sizeBytes = sizeBytes
        )
        return track to newCached
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
