package com.spotifylocal.android.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.spotifylocal.android.data.RepeatMode
import com.spotifylocal.android.data.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerUiState(
    val currentTrackRelPath: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val queueSize: Int = 0
)

class PlayerController(
    private val context: Context,
    private val onTrackCompleted: (relPath: String) -> Unit
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var progressJob: Job? = null
    private var lastObservedMediaId: String? = null

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            syncStateFromPlayer()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                val finishedId = controller?.currentMediaItem?.mediaId ?: lastObservedMediaId
                if (!finishedId.isNullOrBlank()) {
                    onTrackCompleted(finishedId)
                }
            }
            syncStateFromPlayer()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val previousId = lastObservedMediaId
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO && !previousId.isNullOrBlank()) {
                onTrackCompleted(previousId)
            }
            lastObservedMediaId = mediaItem?.mediaId
            syncStateFromPlayer()
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            syncStateFromPlayer()
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            syncStateFromPlayer()
        }
    }

    fun connect() {
        if (controllerFuture != null) return
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future

        scope.launch {
            runCatching {
                val mc = future.await()
                controller = mc
                mc.addListener(playerListener)
                lastObservedMediaId = mc.currentMediaItem?.mediaId
                syncStateFromPlayer()
                startProgressLoop()
            }
        }
    }

    fun playQueue(tracks: List<Track>, startIndex: Int = 0, shuffle: Boolean = false) {
        val mc = controller ?: return
        if (tracks.isEmpty()) return

        val mediaItems = tracks.map { track ->
            MediaItem.Builder()
                .setMediaId(track.relPath)
                .setUri(track.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .build()
                )
                .build()
        }

        val safeIndex = startIndex.coerceIn(0, mediaItems.lastIndex)
        mc.shuffleModeEnabled = shuffle
        mc.setMediaItems(mediaItems, safeIndex, 0L)
        lastObservedMediaId = mediaItems[safeIndex].mediaId
        mc.prepare()
        mc.play()
        syncStateFromPlayer()
    }

    fun togglePlayPause() {
        val mc = controller ?: return
        if (mc.isPlaying) {
            mc.pause()
        } else {
            if (mc.playbackState == Player.STATE_ENDED) {
                mc.seekTo(0L)
            }
            mc.play()
        }
        syncStateFromPlayer()
    }

    fun next() {
        val mc = controller ?: return
        if (mc.hasNextMediaItem()) {
            mc.seekToNextMediaItem()
        }
        syncStateFromPlayer()
    }

    fun previous() {
        val mc = controller ?: return
        if (mc.currentPosition > 3000L) {
            mc.seekTo(0L)
        } else if (mc.hasPreviousMediaItem()) {
            mc.seekToPreviousMediaItem()
        } else {
            mc.seekTo(0L)
        }
        syncStateFromPlayer()
    }

    fun seekTo(positionMs: Long) {
        val mc = controller ?: return
        mc.seekTo(positionMs.coerceAtLeast(0L))
        syncStateFromPlayer()
    }

    fun toggleShuffle() {
        val mc = controller ?: return
        mc.shuffleModeEnabled = !mc.shuffleModeEnabled
        syncStateFromPlayer()
    }

    fun cycleRepeatMode() {
        val mc = controller ?: return
        mc.repeatMode = when (mc.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        syncStateFromPlayer()
    }

    fun release() {
        progressJob?.cancel()
        progressJob = null
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controller = null
    }

    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val mc = controller
                if (mc != null && mc.isPlaying) {
                    _state.update {
                        it.copy(
                            positionMs = mc.currentPosition.coerceAtLeast(0L),
                            durationMs = mc.duration.coerceAtLeast(0L)
                        )
                    }
                }
                delay(500L)
            }
        }
    }

    private fun syncStateFromPlayer() {
        val mc = controller ?: return
        val repeat = when (mc.repeatMode) {
            Player.REPEAT_MODE_ALL -> RepeatMode.ALL
            Player.REPEAT_MODE_ONE -> RepeatMode.ONE
            else -> RepeatMode.OFF
        }
        _state.update {
            it.copy(
                currentTrackRelPath = mc.currentMediaItem?.mediaId,
                isPlaying = mc.isPlaying,
                positionMs = mc.currentPosition.coerceAtLeast(0L),
                durationMs = mc.duration.coerceAtLeast(0L),
                shuffleEnabled = mc.shuffleModeEnabled,
                repeatMode = repeat,
                queueSize = mc.mediaItemCount
            )
        }
    }
}
