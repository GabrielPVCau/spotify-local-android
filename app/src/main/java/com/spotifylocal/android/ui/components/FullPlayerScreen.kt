package com.spotifylocal.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spotifylocal.android.data.RepeatMode
import com.spotifylocal.android.data.Track
import com.spotifylocal.android.playback.PlayerUiState
import com.spotifylocal.android.ui.theme.SpotifyBlack
import com.spotifylocal.android.ui.theme.SpotifyElevated
import com.spotifylocal.android.ui.theme.SpotifyGreen
import com.spotifylocal.android.ui.theme.SpotifyTextPrimary
import com.spotifylocal.android.ui.theme.SpotifyTextSecondary

@Composable
fun FullPlayerScreen(
    track: Track,
    playerState: PlayerUiState,
    onDismiss: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleLike: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SpotifyBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1F3B2C), SpotifyBlack)
                    )
                )
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Fechar player",
                        tint = SpotifyTextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "TOCANDO DA PLAYLIST",
                        style = MaterialTheme.typography.labelSmall,
                        color = SpotifyTextSecondary
                    )
                    Text(
                        text = track.playlistName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = SpotifyTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SpotifyElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = SpotifyGreen,
                    modifier = Modifier.size(112.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = SpotifyTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodyLarge,
                        color = SpotifyTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (track.playCount > 0) {
                        Text(
                            text = "${track.playCount} reproduções",
                            style = MaterialTheme.typography.labelMedium,
                            color = SpotifyGreen
                        )
                    }
                }
                IconButton(onClick = onToggleLike) {
                    Icon(
                        imageVector = if (track.liked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Curtir",
                        tint = if (track.liked) SpotifyGreen else SpotifyTextPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val duration = playerState.durationMs.coerceAtLeast(1L)
            val position = playerState.positionMs.coerceIn(0L, duration)

            Slider(
                value = position.toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..duration.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = SpotifyTextPrimary,
                    activeTrackColor = SpotifyGreen,
                    inactiveTrackColor = SpotifyTextSecondary.copy(alpha = 0.3f)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration(position),
                    style = MaterialTheme.typography.labelSmall,
                    color = SpotifyTextSecondary
                )
                Text(
                    text = formatDuration(playerState.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = SpotifyTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Aleatório",
                        tint = if (playerState.shuffleEnabled) SpotifyGreen else SpotifyTextSecondary
                    )
                }

                IconButton(onClick = onPrevious) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Anterior",
                        tint = SpotifyTextPrimary,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(SpotifyTextPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onPlayPause, modifier = Modifier.size(68.dp)) {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playerState.isPlaying) "Pausar" else "Reproduzir",
                            tint = SpotifyBlack,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                IconButton(onClick = onNext) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Próxima",
                        tint = SpotifyTextPrimary,
                        modifier = Modifier.size(40.dp)
                    )
                }

                IconButton(onClick = onCycleRepeat) {
                    val icon = if (playerState.repeatMode == RepeatMode.ONE) {
                        Icons.Default.RepeatOne
                    } else {
                        Icons.Default.Repeat
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = "Repetir",
                        tint = if (playerState.repeatMode != RepeatMode.OFF) SpotifyGreen else SpotifyTextSecondary
                    )
                }
            }
        }
    }
}
