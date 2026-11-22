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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PushPin
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
import com.spotifylocal.android.data.Playlist
import com.spotifylocal.android.ui.theme.SpotifyElevated
import com.spotifylocal.android.ui.theme.SpotifyGreen
import com.spotifylocal.android.ui.theme.SpotifyTextPrimary
import com.spotifylocal.android.ui.theme.SpotifyTextSecondary

@Composable
fun PlaylistsScreen(
    playlists: List<Playlist>,
    isScanning: Boolean,
    onOpenPlaylist: (String) -> Unit,
    onRescan: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Playlists",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = SpotifyTextPrimary
            )
            IconButton(onClick = onRescan) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Atualizar",
                    tint = SpotifyTextPrimary
                )
            }
        }

        if (isScanning) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SpotifyGreen)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(playlists, key = { it.id }) { playlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenPlaylist(playlist.id) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SpotifyElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (playlist.id == MusicScanner.PLAYLIST_LIKED_ID) {
                                    Icons.Default.Favorite
                                } else {
                                    Icons.Default.LibraryMusic
                                },
                                contentDescription = null,
                                tint = SpotifyGreen,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = SpotifyTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${playlist.tracks.size} músicas",
                                style = MaterialTheme.typography.bodySmall,
                                color = SpotifyTextSecondary
                            )
                        }

                        if (playlist.pinned) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Fixada",
                                tint = SpotifyGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
