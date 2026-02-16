package com.navidrome.model

import kotlinx.serialization.Serializable

@Serializable
data class Artist(
    val id: String = "",
    val name: String = "",
    val coverArt: String? = null,
    val albumCount: Int = 0,
    val artistImageUrl: String? = null
)

@Serializable
data class Album(
    val id: String = "",
    val name: String = "",
    val artist: String = "",
    val artistId: String? = null,
    val coverArt: String? = null,
    val songCount: Int = 0,
    val duration: Int = 0,
    val year: Int? = null,
    val genre: String? = null
)

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
