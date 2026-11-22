package com.spotifylocal.android.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.OffsetDateTime

class SyncManager(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        prettyPrint = true
        encodeDefaults = true
    }

    companion object {
        const val SYNC_DIR_NAME = ".player-sync"
        const val ANDROID_SYNC_FILE = "android.json"
        const val DEVICE_ID = "android"
    }

    fun nowIsoUtc(): String = Instant.now().toString()

    suspend fun readMergedSyncData(rootTreeUri: Uri): MergedSyncData = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, rootTreeUri) ?: return@withContext MergedSyncData()
        val syncDir = root.findFile(SYNC_DIR_NAME)?.takeIf { it.isDirectory }
            ?: return@withContext MergedSyncData()

        val syncFiles = syncDir.listFiles()
            .filter { it.isFile && (it.name?.endsWith(".json", ignoreCase = true) == true) }
            .mapNotNull { file -> readSyncFile(file) }

        mergeSyncFiles(syncFiles)
    }

    fun mergeSyncFiles(files: List<SyncFile>): MergedSyncData {
        val mergedTracks = mutableMapOf<String, MergedTrackStats>()
        val mergedPlaylists = mutableMapOf<String, MergedPlaylistMeta>()

        for (file in files) {
            for ((relPath, entry) in file.tracks) {
                val existing = mergedTracks[relPath]
                if (existing == null) {
                    mergedTracks[relPath] = MergedTrackStats(
                        playCount = entry.playCount.coerceAtLeast(0),
                        lastPlayedAt = entry.lastPlayedAt,
                        liked = entry.liked,
                        likedUpdatedAt = entry.likedUpdatedAt
                    )
                } else {
                    val totalPlays = existing.playCount + entry.playCount.coerceAtLeast(0)
                    val latestPlayed = pickLatestTimestamp(existing.lastPlayedAt, entry.lastPlayedAt)
                    val useIncomingLike = isLaterTimestamp(entry.likedUpdatedAt, existing.likedUpdatedAt)
                    mergedTracks[relPath] = MergedTrackStats(
                        playCount = totalPlays,
                        lastPlayedAt = latestPlayed,
                        liked = if (useIncomingLike) entry.liked else existing.liked,
                        likedUpdatedAt = if (useIncomingLike) entry.likedUpdatedAt else existing.likedUpdatedAt
                    )
                }
            }

            for ((playlistId, entry) in file.playlists) {
                val existing = mergedPlaylists[playlistId]
                if (existing == null || isLaterTimestamp(entry.updatedAt, existing.updatedAt)) {
                    mergedPlaylists[playlistId] = MergedPlaylistMeta(
                        pinned = entry.pinned,
                        sortOrder = entry.sortOrder ?: 0,
                        updatedAt = entry.updatedAt
                    )
                }
            }
        }

        return MergedSyncData(tracks = mergedTracks, playlists = mergedPlaylists)
    }

    suspend fun recordPlay(rootTreeUri: Uri, relPath: String): MergedSyncData = withContext(Dispatchers.IO) {
        updateAndroidSyncFile(rootTreeUri) { current ->
            val now = nowIsoUtc()
            val prev = current.tracks[relPath] ?: SyncTrackEntry()
            val updatedEntry = prev.copy(
                playCount = prev.playCount + 1,
                lastPlayedAt = now
            )
            current.copy(
                version = 1,
                deviceId = DEVICE_ID,
                updatedAt = now,
                tracks = current.tracks + (relPath to updatedEntry)
            )
        }
        readMergedSyncData(rootTreeUri)
    }

    suspend fun setTrackLiked(rootTreeUri: Uri, relPath: String, liked: Boolean): MergedSyncData =
        withContext(Dispatchers.IO) {
            updateAndroidSyncFile(rootTreeUri) { current ->
                val now = nowIsoUtc()
                val prev = current.tracks[relPath] ?: SyncTrackEntry()
                val updatedEntry = prev.copy(
                    liked = liked,
                    likedUpdatedAt = now
                )
                current.copy(
                    version = 1,
                    deviceId = DEVICE_ID,
                    updatedAt = now,
                    tracks = current.tracks + (relPath to updatedEntry)
                )
            }
            readMergedSyncData(rootTreeUri)
        }

    suspend fun setPlaylistPinned(
        rootTreeUri: Uri,
        playlistId: String,
        pinned: Boolean,
        sortOrder: Int = 0
    ): MergedSyncData = withContext(Dispatchers.IO) {
        updateAndroidSyncFile(rootTreeUri) { current ->
            val now = nowIsoUtc()
            val updatedEntry = SyncPlaylistEntry(
                pinned = pinned,
                sortOrder = sortOrder,
                updatedAt = now
            )
            current.copy(
                version = 1,
                deviceId = DEVICE_ID,
                updatedAt = now,
                playlists = current.playlists + (playlistId to updatedEntry)
            )
        }
        readMergedSyncData(rootTreeUri)
    }

    private fun updateAndroidSyncFile(rootTreeUri: Uri, transform: (SyncFile) -> SyncFile) {
        val root = DocumentFile.fromTreeUri(context, rootTreeUri) ?: return
        val syncDir = root.findFile(SYNC_DIR_NAME)?.takeIf { it.isDirectory }
            ?: root.createDirectory(SYNC_DIR_NAME)
            ?: return

        val existingFile = syncDir.findFile(ANDROID_SYNC_FILE)?.takeIf { it.isFile }
        val current = existingFile?.let { readSyncFile(it) } ?: SyncFile(
            version = 1,
            deviceId = DEVICE_ID,
            updatedAt = nowIsoUtc()
        )

        val updated = transform(current)
        val targetFile = existingFile ?: syncDir.createFile("application/json", ANDROID_SYNC_FILE) ?: return
        val payload = json.encodeToString(updated)

        runCatching {
            context.contentResolver.openOutputStream(targetFile.uri, "wt")?.use { out ->
                out.write(payload.toByteArray(Charsets.UTF_8))
                out.flush()
            }
        }
    }

    private fun readSyncFile(file: DocumentFile): SyncFile? {
        return runCatching {
            context.contentResolver.openInputStream(file.uri)?.use { input ->
                val text = input.bufferedReader(Charsets.UTF_8).readText()
                if (text.isBlank()) null else json.decodeFromString<SyncFile>(text)
            }
        }.getOrNull()
    }

    private fun pickLatestTimestamp(a: String?, b: String?): String? {
        if (a.isNullOrBlank()) return b
        if (b.isNullOrBlank()) return a
        return if (isLaterTimestamp(b, a)) b else a
    }

    private fun parseIsoInstant(ts: String): Instant? {
        return runCatching { OffsetDateTime.parse(ts).toInstant() }.getOrNull()
            ?: runCatching { Instant.parse(ts) }.getOrNull()
    }

    private fun isLaterTimestamp(candidate: String?, baseline: String?): Boolean {
        if (candidate.isNullOrBlank()) return false
        if (baseline.isNullOrBlank()) return true
        val candInstant = parseIsoInstant(candidate)
        val baseInstant = parseIsoInstant(baseline)
        return if (candInstant != null && baseInstant != null) {
            candInstant.isAfter(baseInstant)
        } else {
            candidate > baseline
        }
    }
}
