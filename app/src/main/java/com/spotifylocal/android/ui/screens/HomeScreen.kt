package com.spotifylocal.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
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
import com.spotifylocal.android.data.MusicScanner
import com.spotifylocal.android.data.Track
import com.spotifylocal.android.ui.MainUiState
import com.spotifylocal.android.ui.components.TrackRow
import com.spotifylocal.android.ui.theme.SpotifyElevated
import com.spotifylocal.android.ui.theme.SpotifyGreen
import com.spotifylocal.android.ui.theme.SpotifyTextPrimary
import com.spotifylocal.android.ui.theme.SpotifyTextSecondary

@Composable
fun HomeScreen(
    uiState: MainUiState,
    currentTrackRelPath: String?,
    onOpenPlaylist: (String) -> Unit,
    onPlayTrack: (List<Track>, Int) -> Unit,
    onToggleLike: (Track) -> Unit,
    onRescan: () -> Unit,
    onChangeFolder: () -> Unit
) {
    val topPlayed = uiState.allTracks
        .filter { it.playCount > 0 }
        .sortedByDescending { it.playCount }
        .take(10)

    val recentlyPlayed = uiState.allTracks
        .filter { !it.lastPlayedAt.isNullOrBlank() }
        .sortedByDescending { it.lastPlayedAt }
        .take(10)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Spotify Local",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = SpotifyTextPrimary
                    )
                    Text(
                        text = "${uiState.folderName} • ${uiState.allTracks.size} músicas",
                        style = MaterialTheme.typography.bodySmall,
                        color = SpotifyTextSecondary
                    )
                }
                Row {
                    IconButton(onClick = onRescan) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reescanear",
                            tint = SpotifyTextPrimary
                        )
                    }
                    IconButton(onClick = onChangeFolder) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Trocar pasta",
                            tint = SpotifyTextPrimary
                        )
                    }
                }
            }
        }

        if (uiState.isScanning) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = SpotifyGreen)
                }
            }
        }

        item {
            Text(
                text = "Suas Playlists",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SpotifyTextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.playlists, key = { it.id }) { playlist ->
                    Column(
                        modifier = Modifier
                            .width(136.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SpotifyElevated)
                            .clickable { onOpenPlaylist(playlist.id) }
                            .padding(12.dp)
                    ) {
                        Icon(
                            imageVector = if (playlist.id == MusicScanner.PLAYLIST_LIKED_ID) {
                                Icons.Default.Favorite
                            } else {
                                Icons.Default.LibraryMusic
                            },
                            contentDescription = null,
                            tint = SpotifyGreen,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = SpotifyTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${playlist.tracks.size} músicas",
                            style = MaterialTheme.typography.labelSmall,
                            color = SpotifyTextSecondary
                        )
                    }
                }
            }
        }

        if (recentlyPlayed.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Tocadas recentemente",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SpotifyTextPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            itemsIndexed(recentlyPlayed, key = { _, t -> "recent_${t.relPath}" }) { index, track ->
                TrackRow(
                    track = track,
                    isCurrent = track.relPath == currentTrackRelPath,
                    onClick = { onPlayTrack(recentlyPlayed, index) },
                    onToggleLike = { onToggleLike(track) }
                )
            }
        }

        if (topPlayed.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Mais reproduzidas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SpotifyTextPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            itemsIndexed(topPlayed, key = { _, t -> "top_${t.relPath}" }) { index, track ->
                TrackRow(
                    track = track,
                    isCurrent = track.relPath == currentTrackRelPath,
                    onClick = { onPlayTrack(topPlayed, index) },
                    onToggleLike = { onToggleLike(track) }
                )
            }
        }
    }
}
