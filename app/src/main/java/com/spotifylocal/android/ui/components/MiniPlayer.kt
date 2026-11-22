package com.spotifylocal.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spotifylocal.android.data.Track
import com.spotifylocal.android.playback.PlayerUiState
import com.spotifylocal.android.ui.theme.SpotifyElevated
import com.spotifylocal.android.ui.theme.SpotifyGreen
import com.spotifylocal.android.ui.theme.SpotifyTextPrimary
import com.spotifylocal.android.ui.theme.SpotifyTextSecondary

@Composable
fun MiniPlayer(
    track: Track,
    playerState: PlayerUiState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onToggleLike: () -> Unit,
    onClick: () -> Unit
) {
    val progress = if (playerState.durationMs > 0L) {
        (playerState.positionMs.toFloat() / playerState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SpotifyElevated)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = SpotifyTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = SpotifyTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onToggleLike) {
                Icon(
                    imageVector = if (track.liked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Curtir",
                    tint = if (track.liked) SpotifyGreen else SpotifyTextSecondary
                )
            }

            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playerState.isPlaying) "Pausar" else "Reproduzir",
                    tint = SpotifyTextPrimary
                )
            }

            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Próxima",
                    tint = SpotifyTextPrimary
                )
            }
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp),
            color = SpotifyGreen,
            trackColor = SpotifyTextSecondary.copy(alpha = 0.2f)
        )
    }
}
