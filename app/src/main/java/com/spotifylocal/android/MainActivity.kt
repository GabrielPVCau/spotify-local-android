package com.spotifylocal.android

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.spotifylocal.android.ui.MainViewModel
import com.spotifylocal.android.ui.SpotifyLocalApp
import com.spotifylocal.android.ui.theme.SpotifyLocalTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching {
                contentResolver.takePersistableUriPermission(uri, flags)
            }
            viewModel.onFolderSelected(uri)
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            SpotifyLocalTheme {
                SpotifyLocalApp(
                    viewModel = viewModel,
                    onPickFolder = { folderPickerLauncher.launch(null) }
                )
            }
        }
    }
}
