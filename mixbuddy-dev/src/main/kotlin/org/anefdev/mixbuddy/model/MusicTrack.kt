package org.anefdev.mixbuddy.model

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
    var previewUrl: String? = null) {

}