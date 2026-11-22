package com.spotifylocal.android.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class CachedTrack(
    val relPath: String,
    val uriString: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val playlistId: String,
    val playlistName: String,
    val lastModified: Long,
    val sizeBytes: Long
)

@Serializable
data class CachedLibrary(
    val folderUri: String,
    val folderName: String,
    val tracks: List<CachedTrack> = emptyList()
)

class LibraryCache(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val cacheFile: File
        get() = File(context.filesDir, "library_cache.json")

    suspend fun load(folderUri: Uri): CachedLibrary? = withContext(Dispatchers.IO) {
        val file = cacheFile
        if (!file.exists()) return@withContext null
        runCatching {
            val text = file.readText(Charsets.UTF_8)
            if (text.isBlank()) return@runCatching null
            val cached = json.decodeFromString<CachedLibrary>(text)
            if (cached.folderUri == folderUri.toString()) cached else null
        }.getOrNull()
    }

    suspend fun save(library: CachedLibrary) = withContext(Dispatchers.IO) {
        runCatching {
            val tmp = File(context.filesDir, "library_cache.json.tmp")
            tmp.writeText(json.encodeToString(library), Charsets.UTF_8)
            if (!tmp.renameTo(cacheFile)) {
                cacheFile.writeText(json.encodeToString(library), Charsets.UTF_8)
                tmp.delete()
            }
        }
    }
}
