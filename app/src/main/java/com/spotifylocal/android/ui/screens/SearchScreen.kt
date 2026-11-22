package com.spotifylocal.android.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spotifylocal.android.data.Playlist
import com.spotifylocal.android.data.Track
import com.spotifylocal.android.ui.components.TrackRow
import com.spotifylocal.android.ui.theme.SpotifyElevated
import com.spotifylocal.android.ui.theme.SpotifyGreen
import com.spotifylocal.android.ui.theme.SpotifyTextPrimary
import com.spotifylocal.android.ui.theme.SpotifyTextSecondary

@Composable
fun SearchScreen(
    query: String,
    allTracks: List<Track>,
    playlists: List<Playlist>,
    currentTrackRelPath: String?,
    onQueryChange: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onPlayTrack: (List<Track>, Int) -> Unit,
    onToggleLike: (Track) -> Unit
) {
    val trimmed = query.trim().lowercase()

    val matchedPlaylists = remember(trimmed, playlists) {
        if (trimmed.isEmpty()) emptyList()
        else playlists.filter { it.name.lowercase().contains(trimmed) }
    }

    val matchedTracks = remember(trimmed, allTracks) {
        if (trimmed.isEmpty()) allTracks.take(30)
        else allTracks.filter {
            it.title.lowercase().contains(trimmed) ||
                it.artist.lowercase().contains(trimmed) ||
                it.album.lowercase().contains(trimmed) ||
                it.playlistName.lowercase().contains(trimmed)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Buscar",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = SpotifyTextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        )

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Músicas, artistas ou playlists...", color = SpotifyTextSecondary) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = SpotifyTextSecondary)
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SpotifyElevated,
                unfocusedContainerColor = SpotifyElevated,
                focusedBorderColor = SpotifyGreen,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = SpotifyTextPrimary,
                unfocusedTextColor = SpotifyTextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (matchedPlaylists.isNotEmpty()) {
                item {
                    Text(
                        text = "Playlists",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SpotifyTextSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
                items(matchedPlaylists, key = { "pl_${it.id}" }) { playlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenPlaylist(playlist.id) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LibraryMusic,
                            contentDescription = null,
                            tint = SpotifyGreen,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = playlist.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = SpotifyTextPrimary
                            )
                            Text(
                                text = "${playlist.tracks.size} músicas",
                                style = MaterialTheme.typography.bodySmall,
                                color = SpotifyTextSecondary
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = if (trimmed.isEmpty()) "Músicas" else "Resultados (${matchedTracks.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SpotifyTextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            itemsIndexed(matchedTracks, key = { _, t -> "search_${t.relPath}" }) { index, track ->
                TrackRow(
                    track = track,
                    isCurrent = track.relPath == currentTrackRelPath,
                    onClick = { onPlayTrack(matchedTracks, index) },
                    onToggleLike = { onToggleLike(track) }
                )
            }
        }
    }
}
