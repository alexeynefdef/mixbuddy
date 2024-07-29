package org.anefdev.mixbuddy.util

import io.github.oshai.kotlinlogging.KotlinLogging
import org.anefdev.mixbuddy.model.MusicPlaylist
import org.anefdev.mixbuddy.model.MusicTrack
import org.springframework.stereotype.Component
import se.michaelthelin.spotify.model_objects.miscellaneous.AudioAnalysis
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack
import se.michaelthelin.spotify.model_objects.specification.Track
import kotlin.math.abs

private val logger = KotlinLogging.logger {}

@Component
class PlaylistParser {

    fun getTrackListNoMetaData(playlist: List<PlaylistTrack>): List<MusicTrack> {

        logger.info { "getTrackListNoMetaData [ Getting a track list without metadata ... ]" }

        return playlist.stream().map { playlistTrack ->
            val musicTrack = MusicTrack()
            musicTrack.id = playlistTrack.track.id
            musicTrack
        }.toList()
    }

    fun getAllPlaylists(playlists: List<PlaylistSimplified>): List<MusicPlaylist> {
        return playlists.stream().map { playlist ->
            val musicPlaylist = MusicPlaylist()
            musicPlaylist.id = playlist.id
            musicPlaylist.title = playlist.name
            musicPlaylist.count = playlist.tracks.total
            musicPlaylist.imageUrl = playlist.images[0].url!!
            musicPlaylist
        }.toList()
    }

    fun convertTrackAnalysisToTrack(analysis: AudioAnalysis): MusicTrack {
        val track = MusicTrack()
        val trackAnalysis = analysis.track
        track.bpm = trackAnalysis.tempo.toString()
        track.key = trackAnalysis.key.toString()
        track.mode = trackAnalysis.mode.toString()

        return track
    }

    fun parseTrackAnalysisKey(track: MusicTrack): MusicTrack {
        val keys: MutableMap<Int, String> = HashMap()
        keys[-1] = "NO_KEY"
        keys[0] = "C"
        keys[1] = "D♭"
        keys[2] = "D"
        keys[3] = "E♭"
        keys[4] = "E"
        keys[5] = "F"
        keys[6] = "F♯"
        keys[7] = "G"
        keys[8] = "A♭"
        keys[9] = "A"
        keys[10] = "B♭"
        keys[11] = "B"

        val camelot: MutableMap<String?, String> = HashMap()
        camelot["A♭-Min"] = "a1"
        camelot["E♭-Min"] = "a2"
        camelot["B♭-Min"] = "a3"
        camelot["F-Min"] = "a4"
        camelot["C-Min"] = "a5"
        camelot["G-Min"] = "a6"
        camelot["D-Min"] = "a7"
        camelot["A-Min"] = "a8"
        camelot["E-Min"] = "a9"
        camelot["B-Min"] = "a10"
        camelot["F♯-Min"] = "a11"
        camelot["D♭-Min"] = "a12"
        camelot["B-Maj"] = "b1"
        camelot["F♯-Maj"] = "b2"
        camelot["D♭-Maj"] = "b3"
        camelot["A♭-Maj"] = "b4"
        camelot["E♭-Maj"] = "b5"
        camelot["B♭-Maj"] = "b6"
        camelot["F-Maj"] = "b7"
        camelot["C-Maj"] = "b8"
        camelot["G-Maj"] = "b9"
        camelot["D-Maj"] = "b10"
        camelot["A-Maj"] = "b11"
        camelot["E-Maj"] = "b12"

        track.key = (keys[track.key!!.toInt()]
                + "-"
                + (if (track.mode == "MAJOR") "Maj" else "Min"))
        track.camelot = camelot[track.key]
        return track
    }

    fun parseTrackInfo(track: MusicTrack, trackInfo: Track): MusicTrack {
        track.id = trackInfo.id
        track.title = trackInfo.name
        track.artist = trackInfo.artists[0].name
        track.album = trackInfo.album.name
        track.duration = trackInfo.durationMs
        track.previewUrl = trackInfo.previewUrl
        return track
    }

    fun sortPlaylist(playlist: List<MusicTrack>, trackId: String): List<MusicTrack> {
        val harmonies: MutableMap<String?, List<String?>> = HashMap()
        harmonies["a1"] = listOf("a12", "a2", "b1")
        harmonies["a2"] = listOf("a1", "a3", "b2")
        harmonies["a3"] = listOf("a2", "a4", "b3")
        harmonies["a4"] = listOf("a3", "a5", "b4")
        harmonies["a5"] = listOf("a4", "a6", "b5")
        harmonies["a6"] = listOf("a5", "a7", "b6")
        harmonies["a7"] = listOf("a6", "a8", "b7")
        harmonies["a8"] = listOf("a7", "a9", "b8")
        harmonies["a9"] = listOf("a8", "a10", "b9")
        harmonies["a10"] = listOf("a9", "a11", "b10")
        harmonies["a11"] = listOf("a10", "a12", "b11")
        harmonies["a12"] = listOf("a11", "a1", "b12")
        harmonies["b1"] = listOf("b12", "b2", "a1")
        harmonies["b2"] = listOf("b1", "b2", "a2")
        harmonies["b3"] = listOf("b2", "b4", "a3")
        harmonies["b4"] = listOf("b3", "b5", "a4")
        harmonies["b5"] = listOf("b4", "b6", "a5")
        harmonies["b6"] = listOf("b5", "b7", "a6")
        harmonies["b7"] = listOf("b6", "b8", "a7")
        harmonies["b8"] = listOf("b7", "b9", "a8")
        harmonies["b9"] = listOf("b8", "b10", "a9")
        harmonies["b10"] = listOf("b9", "b11", "a10")
        harmonies["b11"] = listOf("b10", "b12", "a11")
        harmonies["b12"] = listOf("b11", "b1", "a12")

        var trackFind: MusicTrack? = null
        for (track in playlist) {
            if (track.id == trackId) {
                trackFind = track
            }
        }
        checkNotNull(trackFind)
        val currentHarmonies = harmonies[trackFind.camelot]!!
        val matchedTracks: MutableList<MusicTrack> = ArrayList()

        for (track in playlist) {
            if (currentHarmonies.contains(track.camelot) || trackFind.camelot == track.camelot) {
                track.matched = true
                matchedTracks.add(track)
            }
        }

        matchedTracks.sortWith { t1, t2 ->
            val diff = abs(t1.bpm!!.toFloat() - t2.bpm!!.toFloat())
            when {
                diff > 0 -> 1
                diff < 0 -> -1
                else -> 0
            }
        }

        val playlistFinal: MutableList<MusicTrack> = ArrayList(matchedTracks)

        for (track in playlist) {
            if (!playlistFinal.contains(track) && trackFind.id != track.id) {
                playlistFinal.add(track)
            }
        }

        return playlistFinal
    }

}