package com.navidrome.viewmodel

import kotlinx.coroutines.*
import com.navidrome.api.NavidromeClient
import com.navidrome.model.Artist
import com.navidrome.model.Album
import com.navidrome.model.Song

/**
 * Estado de la navegación
 */
sealed class Screen {
    object Login : Screen()
    object Home : Screen()
    object Search : Screen()
    data class ArtistDetail(val artistId: String) : Screen()
    data class AlbumDetail(val albumId: String) : Screen()
    object Player : Screen()
}

/**
 * ViewModel principal de la aplicación
 */
class SoundtrackViewModel(
    private val client: NavidromeClient?
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Estado
    var currentScreen: Screen = Screen.Home
        private set
    
    // Home data
    var artists: List<Artist> = emptyList()
        private set
    
    var allAlbums: List<Album> = emptyList()
        private set
    
    var allSongs: List<Song> = emptyList()
        private set
    
    // Current selections
    var currentArtist: Artist? = null
        private set
    
    var albums: List<Album> = emptyList()
        private set
    
    var currentAlbum: Album? = null
        private set
    
    var songs: List<Song> = emptyList()
        private set
    
    // Search
    var searchQuery: String = ""
        private set
    
    var searchResults: SearchResults = SearchResults()
        private set
    
    var isSearching: Boolean = false
        private set
    
    // UI State
    var isLoading: Boolean = false
        private set
    
    var errorMessage: String? = null
        private set
    
    // Reproductor
    var currentSong: Song? = null
        private set
    
    var isPlaying: Boolean = false
        private set
    
    var currentSongIndex: Int = -1
        private set

    // Navigation
    fun navigateTo(screen: Screen) {
        currentScreen = screen
    }

    fun goBack(): Boolean {
        currentScreen = when (currentScreen) {
            is Screen.Home -> return false
            is Screen.Search -> Screen.Home
            is Screen.ArtistDetail -> Screen.Home
            is Screen.AlbumDetail -> Screen.Home
            is Screen.Player -> Screen.Home
            else -> Screen.Home
        }
        return true
    }

    // Home Data Loading
    fun loadHomeData() {
        if (client == null) return
        
        scope.launch {
            isLoading = true
            errorMessage = null
            
            // Load artists
            client.getArtists().fold(
                onSuccess = { response ->
                    artists = response.artists.index.flatMap { it.artist }.sortedBy { it.name.lowercase() }
                },
                onFailure = { e ->
                    errorMessage = "Error cargando artistas: ${e.message}"
                }
            )
            
            // Load albums (recent ones)
            client.getAlbumList("alphabeticalByName", 100).fold(
                onSuccess = { response ->
                    allAlbums = response.albumList.album.sortedBy { it.name.lowercase() }
                },
                onFailure = { e ->
                    errorMessage = "Error cargando álbumes: ${e.message}"
                }
            )
            
            // Load random songs
            client.getRandomSongs(50).fold(
                onSuccess = { response ->
                    allSongs = response.songs.sortedBy { it.title.lowercase() }
                },
                onFailure = { e ->
                    errorMessage = "Error cargando canciones: ${e.message}"
                }
            )
            
            currentScreen = Screen.Home
            isLoading = false
        }
    }

    // Search
    fun search(query: String) {
        if (client == null || query.isBlank()) {
            searchResults = SearchResults()
            return
        }
        
        searchQuery = query
        isSearching = true
        
        scope.launch {
            client.search(query).fold(
                onSuccess = { response ->
                    searchResults = SearchResults(
                        artists = response.searchResult.artist,
                        albums = response.searchResult.album,
                        songs = response.searchResult.song
                    )
                    currentScreen = Screen.Search
                },
                onFailure = { e ->
                    errorMessage = "Error en búsqueda: ${e.message}"
                }
            )
            isSearching = false
        }
    }
    
    fun clearSearch() {
        searchQuery = ""
        searchResults = SearchResults()
    }

    // Artist Detail
    fun loadArtistAlbums(artistId: String) {
        if (client == null) return
        
        scope.launch {
            isLoading = true
            errorMessage = null
            
            client.getArtist(artistId).fold(
                onSuccess = { response ->
                    currentArtist = response.artist
                    albums = response.artist.albumList ?: emptyList()
                    currentScreen = Screen.ArtistDetail(artistId)
                },
                onFailure = { e ->
                    errorMessage = "Error cargando álbumes: ${e.message}"
                }
            )
            
            isLoading = false
        }
    }

    // Album Detail
    fun loadAlbumSongs(albumId: String) {
        if (client == null) return
        
        scope.launch {
            isLoading = true
            errorMessage = null
            
            client.getAlbum(albumId).fold(
                onSuccess = { response ->
                    currentAlbum = response.album
                    songs = response.album.songList ?: emptyList()
                    currentScreen = Screen.AlbumDetail(albumId)
                },
                onFailure = { e ->
                    errorMessage = "Error cargando canciones: ${e.message}"
                }
            )
            
            isLoading = false
        }
    }

    // Direct album/song navigation
    fun openAlbum(albumId: String) {
        loadAlbumSongs(albumId)
    }

    // Player
    fun playSong(song: Song, playlist: List<Song> = songs) {
        currentSong = song
        currentSongIndex = playlist.indexOf(song)
        isPlaying = true
        currentScreen = Screen.Player
    }

    fun togglePlayPause() {
        isPlaying = !isPlaying
    }

    fun nextSong() {
        if (currentSongIndex < songs.size - 1) {
            currentSongIndex++
            currentSong = songs[currentSongIndex]
        }
    }

    fun previousSong() {
        if (currentSongIndex > 0) {
            currentSongIndex--
            currentSong = songs[currentSongIndex]
        }
    }

    fun getStreamUrl(songId: String): String? = client?.getStreamUrl(songId)

    fun getCoverArtUrl(coverArtId: String?, size: Int = 300): String? {
        if (coverArtId == null) return null
        return client?.getCoverArtUrl(coverArtId, size)
    }

    fun cleanup() {
        scope.cancel()
    }
}

data class SearchResults(
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList(),
    val songs: List<Song> = emptyList()
) {
    val isEmpty: Boolean get() = artists.isEmpty() && albums.isEmpty() && songs.isEmpty()
}
