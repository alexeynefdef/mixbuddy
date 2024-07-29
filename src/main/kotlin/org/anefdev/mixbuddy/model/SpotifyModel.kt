package org.anefdev.mixbuddy.model

data class SpotifyUser(
    var id: String,
    var email: String,
    var name: String,
    var img: String,
    var url: String)

data class MusicPlaylist(
    var id: String? = null,
    var title: String? = null,
    var count: Int? = null,
    var imageUrl: String? = null)

data class MusicTrack(
    var id: String? = null,
    var title: String? = null,
    var artist: String? = null,
    var album: String? = null,
    var duration: Int? = null,
    var bpm: String? = null,
    var key: String? = null,
    var mode: String? = null,
    var camelot: String? = null,
    var matched: Boolean = false,
    var previewUrl: String? = null)

data class SongIds(
    var songIds: Array<String>? = null
)