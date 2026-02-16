package com.navidrome.app.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.navidrome.auth.AuthManager
import com.navidrome.viewmodel.Screen
import com.navidrome.viewmodel.SoundtrackViewModel

class MainActivity : ComponentActivity() {
    private val authManager = AuthManager()
    private var viewModel: SoundtrackViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            SoundtrackApp(
                authManager = authManager,
                onViewModelCreated = { vm -> viewModel = vm }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel?.cleanup()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundtrackApp(
    authManager: AuthManager,
    onViewModelCreated: (SoundtrackViewModel) -> Unit
) {
    var isLoggedIn by remember { mutableStateOf(authManager.isConfigured) }
    
    if (isLoggedIn && authManager.getClient() != null) {
        val vm = remember { 
            SoundtrackViewModel(authManager.getClient()).also { onViewModelCreated(it) }
        }
        
        LaunchedEffect(Unit) {
            vm.loadArtists()
        }
        
        when (val screen = vm.currentScreen) {
            is Screen.ArtistList -> ArtistListScreen(vm)
            is Screen.ArtistDetail -> ArtistDetailScreen(vm)
            is Screen.AlbumDetail -> AlbumDetailScreen(vm)
            is Screen.Player -> PlayerScreen(vm)
            else -> ArtistListScreen(vm)
        }
    } else {
        LoginScreen(
            authManager = authManager,
            onLoggedIn = { isLoggedIn = true }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    authManager: AuthManager,
    onLoggedIn: () -> Unit
) {
    var baseUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Soundtrack") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Conectar a Navidrome",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text("URL del servidor") },
                placeholder = { Text("http://tu-servidor:4533") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Usuario") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = {
                    isLoading = true
                    errorMessage = null
                    
                    kotlinx.coroutines.GlobalScope.launch {
                        val result = authManager.login(baseUrl, username, password)
                        result.fold(
                            onSuccess = {
                                onLoggedIn()
                            },
                            onFailure = { e ->
                                errorMessage = "Error al conectar: ${e.message}"
                            }
                        )
                        isLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && baseUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Conectar")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistListScreen(vm: SoundtrackViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Artistas") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        if (vm.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(vm.artists) { artist ->
                    ListItem(
                        headlineContent = { Text(artist.name) },
                        supportingContent = { Text("${artist.albumCount} álbumes") },
                        modifier = Modifier.clickable { vm.loadArtistAlbums(artist.id) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(vm: SoundtrackViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(vm.currentArtist?.name ?: "Artista") },
                navigationIcon = {
                    IconButton(onClick = { vm.goBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(vm.albums) { album ->
                ListItem(
                    headlineContent = { Text(album.name) },
                    supportingContent = { Text("${album.songCount} canciones") },
                    modifier = Modifier.clickable { vm.loadAlbumSongs(album.id) }
                )
                HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(vm: SoundtrackViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(vm.currentAlbum?.name ?: "Álbum") },
                navigationIcon = {
                    IconButton(onClick = { vm.goBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            itemsIndexed(vm.songs) { index, song ->
                ListItem(
                    headlineContent = { Text(song.title) },
                    supportingContent = { Text(formatDuration(song.duration)) },
                    leadingContent = { Text("${song.track ?: index + 1}") },
                    modifier = Modifier.clickable { vm.playSong(song, vm.songs) }
                )
                HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(vm: SoundtrackViewModel) {
    val song = vm.currentSong
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reproduciendo") },
                navigationIcon = {
                    IconButton(onClick = { vm.goBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = song?.title ?: "Sin canción",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = song?.artist ?: "",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = song?.album ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { vm.previousSong() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Anterior", modifier = Modifier.size(32.dp))
                }
                
                IconButton(
                    onClick = { vm.togglePlayPause() },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        if (vm.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (vm.isPlaying) "Pausar" else "Reproducir",
                        modifier = Modifier.size(40.dp)
                    )
                }
                
                IconButton(
                    onClick = { vm.nextSong() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Siguiente", modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "$mins:${secs.toString().padStart(2, '0')}"
}
