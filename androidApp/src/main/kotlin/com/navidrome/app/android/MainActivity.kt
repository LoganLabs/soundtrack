package com.navidrome.app.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.navidrome.auth.AuthManager
import com.navidrome.model.Artist
import com.navidrome.model.Album
import com.navidrome.model.Song
import com.navidrome.viewmodel.Screen
import com.navidrome.viewmodel.SoundtrackViewModel
import com.navidrome.viewmodel.SearchResults

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
            vm.loadHomeData()
        }
        
        when (val screen = vm.currentScreen) {
            is Screen.Home -> HomeScreen(vm)
            is Screen.Search -> SearchScreen(vm)
            is Screen.ArtistDetail -> ArtistDetailScreen(vm)
            is Screen.AlbumDetail -> AlbumDetailScreen(vm)
            is Screen.Player -> PlayerScreen(vm)
            else -> HomeScreen(vm)
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
fun HomeScreen(vm: SoundtrackViewModel) {
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Soundtrack") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    IconButton(onClick = { 
                        if (searchQuery.isNotBlank()) {
                            vm.search(searchQuery)
                        }
                    }) {
                        Icon(Icons.Default.Search, contentDescription = "Buscar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar artistas, álbumes, canciones...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { 
                            vm.search(searchQuery)
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "Buscar")
                        }
                    }
                }
            )

            if (vm.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Artists Section
                    item {
                        SectionHeader(title = "Artistas", onSeeAllClick = { /* TODO */ })
                    }
                    item {
                        ArtistCarousel(vm.artists, vm)
                    }
                    
                    // Albums Section
                    item {
                        SectionHeader(title = "Álbumes", onSeeAllClick = { /* TODO */ })
                    }
                    item {
                        AlbumCarousel(vm.allAlbums, vm)
                    }
                    
                    // Songs Section
                    item {
                        SectionHeader(title = "Canciones", onSeeAllClick = { /* TODO */ })
                    }
                    item {
                        SongCarousel(vm.allSongs, vm)
                    }
                    
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, onSeeAllClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge
        )
        TextButton(onClick = onSeeAllClick) {
            Text("Ver todos")
        }
    }
}

@Composable
fun ArtistCarousel(artists: List<Artist>, vm: SoundtrackViewModel) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = Modifier.height(100.dp)
    ) {
        items(artists) { artist ->
            Card(
                modifier = Modifier
                    .width(80.dp)
                    .fillMaxHeight()
                    .clickable { vm.loadArtistAlbums(artist.id) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = artist.name,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
fun AlbumCarousel(albums: List<Album>, vm: SoundtrackViewModel) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = Modifier.height(140.dp)
    ) {
        items(albums) { album ->
            Card(
                modifier = Modifier
                    .width(100.dp)
                    .fillMaxHeight()
                    .clickable { vm.openAlbum(album.id) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Album,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = album.name,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2
                    )
                    Text(
                        text = album.artist,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun SongCarousel(songs: List<Song>, vm: SoundtrackViewModel) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = Modifier.height(80.dp)
    ) {
        items(songs) { song ->
            Card(
                modifier = Modifier
                    .width(150.dp)
                    .fillMaxHeight()
                    .clickable { vm.playSong(song, songs) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(vm: SoundtrackViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Búsqueda: ${vm.searchQuery}") },
                navigationIcon = {
                    IconButton(onClick = { 
                        vm.clearSearch()
                        vm.goBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        val results = vm.searchResults
        
        if (vm.isSearching) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (results.isEmpty) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Sin resultados")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Artists
                if (results.artists.isNotEmpty()) {
                    item { SectionHeader(title = "Artistas", onSeeAllClick = {}) }
                    items(results.artists) { artist ->
                        ListItem(
                            headlineContent = { Text(artist.name) },
                            modifier = Modifier.clickable { vm.loadArtistAlbums(artist.id) }
                        )
                    }
                }
                
                // Albums
                if (results.albums.isNotEmpty()) {
                    item { SectionHeader(title = "Álbumes", onSeeAllClick = {}) }
                    items(results.albums) { album ->
                        ListItem(
                            headlineContent = { Text(album.name) },
                            supportingContent = { Text(album.artist) },
                            modifier = Modifier.clickable { vm.openAlbum(album.id) }
                        )
                    }
                }
                
                // Songs
                if (results.songs.isNotEmpty()) {
                    item { SectionHeader(title = "Canciones", onSeeAllClick = {}) }
                    items(results.songs) { song ->
                        ListItem(
                            headlineContent = { Text(song.title) },
                            supportingContent = { Text(song.artist) },
                            modifier = Modifier.clickable { vm.playSong(song, results.songs) }
                        )
                    }
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
                    modifier = Modifier.clickable { vm.openAlbum(album.id) }
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
