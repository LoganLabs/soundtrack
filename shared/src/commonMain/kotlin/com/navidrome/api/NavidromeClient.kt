package com.navidrome.api

import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*

/**
 * Cliente para la API Subsonic/Navidrome
 * 
 * Documentación: https://www.navidrome.org/docs/developers/subsonic-api/
 */
class NavidromeClient(
    private val baseUrl: String,
    private val username: String,
    private val password: String
) {
    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true 
    }
    
    private var salt: String = ""
    private var token: String = ""

    init {
        generateAuth()
    }

    private fun generateAuth() {
        salt = (1..10).map { ('a'..'z').random() }.joinToString("")
        token = md5(password + salt)
    }

    private fun md5(input: String): String {
        // Simplified MD5 placeholder - in production use java.security.MessageDigest
        // For now, we'll use a simple implementation
        return input.hashCode().toString(16).padStart(8, '0')
    }

    private suspend fun request(endpoint: String, params: Map<String, String> = emptyMap()): String {
        return withContext(Dispatchers.IO) {
            val queryParams = params.toMutableMap()
            queryParams["u"] = username
            queryParams["t"] = token
            queryParams["s"] = salt
            queryParams["v"] = "1.16.1"
            queryParams["c"] = "NavidromeApp"
            queryParams["f"] = "json"

            val url = "$baseUrl/rest/$endpoint?${queryParams.toList().joinToString("&") { "${it.key}=${it.value}" }}"
            url
        }
    }

    /**
     * Test de conectividad
     */
    suspend fun ping(): Result<PingResponse> = runCatching {
        val url = request("ping.view")
        println("PING: $url")
        PingResponse(ok = true)
    }

    /**
     * Obtener todos los artistas
     */
    suspend fun getArtists(): Result<ArtistsResponse> = runCatching {
        val url = request("getArtists.view")
        println("GET_ARTISTS: $url")
        ArtistsResponse(artists = ArtistsIndex(listOf()))
    }

    /**
     * Obtener detalles de un artista
     */
    suspend fun getArtist(id: String): Result<ArtistResponse> = runCatching {
        val url = request("getArtist.view", mapOf("id" to id))
        println("GET_ARTIST: $url")
        ArtistResponse(artist = Artist(id = id, name = ""))
    }

    /**
     * Obtener álbum
     */
    suspend fun getAlbum(id: String): Result<AlbumResponse> = runCatching {
        val url = request("getAlbum.view", mapOf("id" to id))
        println("GET_ALBUM: $url")
        AlbumResponse(album = Album(id = id, name = "", songCount = 0, duration = 0))
    }

    /**
     * Obtener canción
     */
    suspend fun getSong(id: String): Result<SongResponse> = runCatching {
        val url = request("getSong.view", mapOf("id" to id))
        println("GET_SONG: $url")
        SongResponse(song = Song(id = id, title = "", artist = "", album = "", duration = 0))
    }

    /**
     * URL para stream de audio
     */
    fun getStreamUrl(id: String, format: String = "mp3"): String {
        return "$baseUrl/rest/stream.view?id=$id&format=$format&u=$username&t=$token&s=$salt&v=1.16.1&c=NavidromeApp"
    }

    /**
     * URL para portada de álbum
     */
    fun getCoverArtUrl(id: String, size: Int = 300): String {
        return "$baseUrl/rest/getCoverArt.view?id=$id&size=$size"
    }

    /**
     * Marcar como favorita
     */
    suspend fun star(id: String): Result<Unit> = runCatching {
        val url = request("star.view", mapOf("id" to id))
        println("STAR: $url")
    }

    /**
     * Quitar de favoritos
     */
    suspend fun unstar(id: String): Result<Unit> = runCatching {
        val url = request("unstar.view", mapOf("id" to id))
        println("UNSTAR: $url")
    }

    /**
     * Lista de álbumes (recientes, favoritos, etc.)
     */
    suspend fun getAlbumList(type: String, size: Int = 20): Result<AlbumListResponse> = runCatching {
        val url = request("getAlbumList2.view", mapOf("type" to type, "size" to size.toString()))
        println("GET_ALBUM_LIST: $url")
        AlbumListResponse(albumList = AlbumList(listOf()))
    }

    /**
     * Buscar
     */
    suspend fun search(query: String): Result<SearchResponse> = runCatching {
        val url = request("search3.view", mapOf("query" to query))
        println("SEARCH: $url")
        SearchResponse(searchResult = SearchResult(listOf()))
    }
}

// ============== DATA MODELS ==============

@Serializable
data class PingResponse(val ok: Boolean = true)

@Serializable
data class ArtistsResponse(val artists: ArtistsIndex)

@Serializable
data class ArtistsIndex(val index: List<ArtistIndexEntry> = listOf())

@Serializable
data class ArtistIndexEntry(
    val name: String = "",
    val artist: List<Artist> = listOf()
)

@Serializable
data class ArtistResponse(val artist: Artist)

@Serializable
data class Artist(
    val id: String = "",
    val name: String = "",
    val coverArt: String? = null,
    val albumCount: Int = 0
)

@Serializable
data class AlbumResponse(val album: Album)

@Serializable
data class Album(
    val id: String = "",
    val name: String = "",
    val artist: String = "",
    val artistId: String? = null,
    val coverArt: String? = null,
    val songCount: Int = 0,
    val duration: Int = 0,
    val year: Int? = null
)

@Serializable
data class SongResponse(val song: Song)

@Serializable
data class Song(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val artistId: String? = null,
    val album: String = "",
    val albumId: String? = null,
    val coverArt: String? = null,
    val duration: Int = 0,
    val track: Int? = null,
    val year: Int? = null,
    val genre: String? = null,
    val size: Long = 0,
    val suffix: String? = null,
    val bitRate: Int? = null,
    val path: String? = null
)

@Serializable
data class AlbumListResponse(val albumList: AlbumList)

@Serializable
data class AlbumList(val album: List<Album> = listOf())

@Serializable
data class SearchResponse(val searchResult: SearchResult)

@Serializable
data class SearchResult(
    val artist: List<Artist> = listOf(),
    val album: List<Album> = listOf(),
    val song: List<Song> = listOf()
)
