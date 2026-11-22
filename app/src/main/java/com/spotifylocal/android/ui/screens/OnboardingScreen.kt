package com.spotifylocal.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.spotifylocal.android.ui.theme.SpotifyBlack
import com.spotifylocal.android.ui.theme.SpotifyGreen
import com.spotifylocal.android.ui.theme.SpotifyTextPrimary
import com.spotifylocal.android.ui.theme.SpotifyTextSecondary

@Composable
fun OnboardingScreen(onPickFolder: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpotifyBlack)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(SpotifyGreen),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = SpotifyBlack,
                modifier = Modifier.size(52.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Spotify Local",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = SpotifyTextPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Selecione a pasta de músicas sincronizada pelo Syncthing (ex.: musica). Subpastas viram playlists e seus dados são sincronizados automaticamente via .player-sync/.",
            style = MaterialTheme.typography.bodyMedium,
            color = SpotifyTextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onPickFolder,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SpotifyGreen,
                contentColor = Color.Black
            )
        ) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = "Selecionar pasta de músicas",
                fontWeight = FontWeight.Bold
            )
        }
    }
}
