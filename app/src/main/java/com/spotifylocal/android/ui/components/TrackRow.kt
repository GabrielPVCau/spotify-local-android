package com.spotifylocal.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.spotifylocal.android.ui.theme.SpotifyElevated
import com.spotifylocal.android.ui.theme.SpotifyGreen
import com.spotifylocal.android.ui.theme.SpotifyTextPrimary
import com.spotifylocal.android.ui.theme.SpotifyTextSecondary

@Composable
fun TrackRow(
    track: Track,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onToggleLike: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(SpotifyElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = if (isCurrent) SpotifyGreen else SpotifyTextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                color = if (isCurrent) SpotifyGreen else SpotifyTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = SpotifyTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (track.playCount > 0) {
                    Text(
                        text = "• ${track.playCount}x",
                        style = MaterialTheme.typography.labelSmall,
                        color = SpotifyGreen
                    )
                }
                if (track.durationMs > 0) {
                    Text(
                        text = "• ${formatDuration(track.durationMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SpotifyTextSecondary
                    )
                }
            }
        }

        IconButton(onClick = onToggleLike) {
            Icon(
                imageVector = if (track.liked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (track.liked) "Descurtir" else "Curtir",
                tint = if (track.liked) SpotifyGreen else SpotifyTextSecondary
            )
        }
    }
}

fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
