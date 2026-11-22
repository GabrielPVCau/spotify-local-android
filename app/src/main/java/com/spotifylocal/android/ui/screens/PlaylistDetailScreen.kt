package com.spotifylocal.android.ui.screens

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spotifylocal.android.data.Playlist
import com.spotifylocal.android.data.SortOption
import com.spotifylocal.android.data.Track
import com.spotifylocal.android.ui.components.TrackRow
import com.spotifylocal.android.ui.theme.SpotifyElevated
import com.spotifylocal.android.ui.theme.SpotifyGreen
import com.spotifylocal.android.ui.theme.SpotifyTextPrimary
import com.spotifylocal.android.ui.theme.SpotifyTextSecondary

@Composable
fun PlaylistDetailScreen(
    playlist: Playlist,
    tracks: List<Track>,
    currentTrackRelPath: String?,
    sortOption: SortOption,
    pinnedSortOption: SortOption,
    filterQuery: String,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    onTrackClick: (Int) -> Unit,
    onToggleLike: (Track) -> Unit,
    onSortSelected: (SortOption) -> Unit,
    onPinSort: () -> Unit,
    onFilterChange: (String) -> Unit,
    onTogglePlaylistPin: () -> Unit
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar",
                    tint = SpotifyTextPrimary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SpotifyTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${tracks.size} músicas",
                    style = MaterialTheme.typography.bodySmall,
                    color = SpotifyTextSecondary
                )
            }
            if (!playlist.isSpecial) {
                IconButton(onClick = onTogglePlaylistPin) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Fixar playlist",
                        tint = if (playlist.pinned) SpotifyGreen else SpotifyTextSecondary
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onPlayAll,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpotifyGreen,
                    contentColor = Color.Black
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Tocar", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onShuffleAll,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(
                    Icons.Default.Shuffle,
                    contentDescription = null,
                    tint = SpotifyTextPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Aleatório", color = SpotifyTextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { sortMenuExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = null,
                        tint = SpotifyGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = sortOption.label,
                        color = SpotifyTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false }
                ) {
                    SortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option.label,
                                    color = if (option == sortOption) SpotifyGreen else SpotifyTextPrimary
                                )
                            },
                            onClick = {
                                onSortSelected(option)
                                sortMenuExpanded = false
                            }
                        )
                    }
                }
            }

            val isPinned = sortOption == pinnedSortOption
            OutlinedButton(
                onClick = onPinSort,
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = null,
                    tint = if (isPinned) SpotifyGreen else SpotifyTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Fixar padrão",
                    color = if (isPinned) SpotifyGreen else SpotifyTextPrimary,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = filterQuery,
            onValueChange = onFilterChange,
            placeholder = { Text("Filtrar nesta playlist...", color = SpotifyTextSecondary) },
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

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            itemsIndexed(tracks, key = { _, t -> t.relPath }) { index, track ->
                TrackRow(
                    track = track,
                    isCurrent = track.relPath == currentTrackRelPath,
                    onClick = { onTrackClick(index) },
                    onToggleLike = { onToggleLike(track) }
                )
            }
        }
    }
}
