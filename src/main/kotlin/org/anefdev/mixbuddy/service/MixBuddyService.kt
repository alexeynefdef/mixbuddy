package org.anefdev.mixbuddy.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.anefdev.mixbuddy.conf.SpotifyConfig
import org.anefdev.mixbuddy.model.MusicPlaylist
import org.anefdev.mixbuddy.model.MusicTrack
import org.anefdev.mixbuddy.model.SongIds
import org.anefdev.mixbuddy.model.SpotifyUser
import org.anefdev.mixbuddy.util.PlaylistParser
import org.springframework.stereotype.Service
import se.michaelthelin.spotify.SpotifyApi
import se.michaelthelin.spotify.SpotifyHttpManager
import se.michaelthelin.spotify.enums.AuthorizationScope
import se.michaelthelin.spotify.model_objects.specification.Paging
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack
import se.michaelthelin.spotify.model_objects.specification.Track
import java.io.File
import java.net.URI
import java.util.*

private val logger = KotlinLogging.logger {}

@Service
class MixBuddyService(
    private val config: SpotifyConfig,
    private val playlistParser: PlaylistParser
) {

    private var allPlaylists: List<MusicPlaylist>? = null
    private var playlistParsed: List<MusicTrack>? = null
    private var currentUser: SpotifyUser? = null
    private val spotifyApi: SpotifyApi = initSpotifyAPI()
    private val playlistDescription: String = "This playlist created by mixbuddy app."

    private final fun initSpotifyAPI(): SpotifyApi {
        val spotifyApi = SpotifyApi.Builder()
            .setClientId(config.clientId)
            .setClientSecret(config.clientSecret)
            .setRedirectUri(SpotifyHttpManager.makeUri(config.callbackUrl))
            .build()
        return spotifyApi
    }

    /**
     * Gets URI for authorization with Spotify.
     * <br></br>
     * Sync
     * @return Authorization code URI
     */
    fun getAuthorizationCodeUri(): URI {
        logger.info { "getAuthorisationCodeURI [ Get authorization code URI ... ]" }

        val authorizationCodeUriRequest =
            spotifyApi.authorizationCodeUri()
                .scope(
                    AuthorizationScope.USER_LIBRARY_READ,
                    AuthorizationScope.PLAYLIST_MODIFY_PUBLIC,
                    AuthorizationScope.PLAYLIST_MODIFY_PRIVATE,
                    AuthorizationScope.USER_LIBRARY_MODIFY,
                    AuthorizationScope.USER_READ_PRIVATE,
                    AuthorizationScope.USER_READ_EMAIL
                )
                .show_dialog(true)
                .build()
        val uri = authorizationCodeUriRequest.execute()
        logger.info { "getAuthorisationCodeURI [ URI: $uri ]" }
        logger.info { "getAuthorisationCodeURI [ OK ]" }
        return uri
    }

    /**
     * Gets and saves the access and refresh tokens in SpotifyApi object.
     * <br></br>
     * Sync
     * @param code Authorization code for Spotify
     */
    fun setAuthorizationToken(code: String) {
        logger.info { "setAuthorizationToken [ Get authorization token ..." }
        logger.info { "setAuthorizationToken [ Authorization code: $code ]" }

        val authorizationCodeRequest = spotifyApi.authorizationCode(code).build()
        val authorizationCodeCredentials = authorizationCodeRequest.execute()
        // Set access and refresh token for further "spotifyApi" object usage
        spotifyApi.accessToken = authorizationCodeCredentials.accessToken
        spotifyApi.refreshToken = authorizationCodeCredentials.refreshToken

        logger.info { "setAuthorizationToken [ OK ]" }
    }

    /**
     * Loads all playlists of the current user.
     * <br></br>
     * Sync
     * @return List of user's playlists
     */
    fun loadAllUsersPlaylists(): List<MusicPlaylist>? {
        logger.info { "loadAllUsersPlaylists [ Loading all user's playlists ... ]" }
        val allUserPlaylists: List<MusicPlaylist>
        val currentUserID = currentUser?.id
        val getListOfUsersPlaylistsRequest = spotifyApi
            .getListOfUsersPlaylists(currentUserID)
            .build()
        val playlistSimplifiedPaging = getListOfUsersPlaylistsRequest.execute()
        val allUserPlaylistsSimple = Arrays.stream(playlistSimplifiedPaging.items).toList()
        allUserPlaylists = playlistParser.getAllPlaylists(allUserPlaylistsSimple)

        this.allPlaylists = allUserPlaylists
        logger.info { "loadAllUsersPlaylists [ OK ]" }

        return this.allPlaylists
    }

    /**
     * Loads playlist by passed ID.
     * <br></br>
     * Async
     * @param playlistId ID of the playlist
     * @return List of MusicTrack playlist
     */
    fun loadPlaylist(playlistId: String): List<MusicTrack>? {
        logger.info { "loadPlaylist [ Loading playlist ... ]" }
        logger.info { "loadPlaylist [ Playlist with ID: $playlistId ]" }

        val playlist: Paging<PlaylistTrack> = spotifyApi.getPlaylistsItems(playlistId)
            .build()
            .execute()

        logger.info { "loadPlaylist [ Loading playlist with size: " + playlist.total + " ]" }

        val playlistFinal = ArrayList(Arrays.stream(playlist.items).toList())
        while (playlist.total > playlistFinal.size) {
            val playlistPagination = spotifyApi.getPlaylistsItems(playlistId)
                .offset(playlistFinal.size)
                .build()
                .execute()
            playlistFinal.addAll(Arrays.stream(playlistPagination.items).toList())
        }

        logger.info { "loadPlaylist [ Extracting track-IDs ... ]" }
        val playlistNoMetaData: List<MusicTrack> = playlistParser.getTrackListNoMetaData(playlistFinal)



        logger.info { "loadPlaylist [ Loading tracks-data ... ]" }
        val tracksAnalyzed: MutableList<MusicTrack>? = playlistNoMetaData.stream().map { track ->
            val trackInfo =
                loadTrackInfo(track.id!!)
            val trackAnalysis = this.loadTrackAnalysis(track.id!!)
            val trackToParse = playlistParser.parseTrackInfo(trackAnalysis, trackInfo)
            playlistParser.parseTrackAnalysisKey(trackToParse)
        }.toList()
        this.playlistParsed = tracksAnalyzed
        logger.info { "loadPlaylist [ OK ]" }
        return this.playlistParsed
    }

    /**
     * Creates new playlist in User´s library by passed List of Song IDs.
     * <br></br>
     * Async
     * @param songIds IDs of the songs to add
     * @return playlist URI
     */
    fun createNewPlaylist(songIds: SongIds): URI {

        logger.info { "createNewPlaylist [ Creating new playlist ... ]" }

        val createPlaylistRequest = spotifyApi.createPlaylist(this.currentUser!!.id, "mixbuddy playlist")
        createPlaylistRequest.public_(true)
        createPlaylistRequest.description(playlistDescription)
        val playlist = createPlaylistRequest.build().execute()
        val songUris = songIds.songIds?.map { track -> spotifyApi.getTrack(track).build().execute().uri}

        if (songUris != null) {
            spotifyApi.addItemsToPlaylist(playlist.id, songUris.toTypedArray()).build().execute()
        }

        logger.info { "createNewPlaylist [ OK ]" }

        logger.info { playlist.toString() }
        return URI.create(playlist.externalUrls.get("spotify"))

    }

    private fun convertFileToByteArray(filePath: String): ByteArray {
        val file = File(filePath)
        val contentBuilder = StringBuilder()

        file.reader().use { reader ->
            reader.forEachLine { line ->
                contentBuilder.append(line).append("\n")
            }
        }

        return contentBuilder.toString().toByteArray()
    }

    /**
     * Loads song info by passed track ID.
     * <br></br>
     * Async
     * @param trackId ID of the track
     * @return Track song info
     */
    private fun loadTrackInfo(trackId: String): Track {
        logger.info { "loadTrackInfo [ Loading song info ... ]" }
        val getTrackRequest = spotifyApi.getTrack(trackId).build()
        val trackFuture = getTrackRequest.executeAsync()
        val track = trackFuture.join()
        logger.info { "loadTrackInfo [ OK ]" }
        return track
    }

    /**
     * Loads audio analysis by passed track ID.
     * <br></br>
     * Async
     * @param trackId ID of the track
     * @return MusicTrack audio analysis
     */
    private fun loadTrackAnalysis(trackId: String): MusicTrack {
        logger.info { "loadTrackAnalysis [ Loading audio analysis ... ]" }
        val getTrackAnalysisRequest = spotifyApi.getAudioAnalysisForTrack(trackId).build()
        val audioAnalysisFuture = getTrackAnalysisRequest.executeAsync()
        val audioAnalysis = audioAnalysisFuture.join()
        logger.info { "loadTrackAnalysis [ OK ]" }
        return playlistParser.convertTrackAnalysisToTrack(audioAnalysis)
    }

    /**
     * Sorts current playlist via Camelot wheel by passed track id.
     * <br></br>
     * Sync
     * @param trackId String id of current song
     * @return sorted playlist
     */
    fun sortPlaylist(trackId: String?): List<MusicTrack> {
        logger.info { "sortPlaylist [ Sorting playlist ... ]" }
        logger.info { "sortPlaylist [ Removing all matched tags ... ]" }

        for (track in playlistParsed!!) {
            track.matched = false
        }

        logger.info { "sortPlaylist [ Sorting playlist ... ]" }
        logger.info { "sortPlaylist [ OK ]" }

        return playlistParser.sortPlaylist(this.playlistParsed!!, trackId!!)
    }

    /**
     * Loads current session Spotify user's data
     * <br></br>
     * Sync
     * @return current user's data
     */
    fun loadUserData(): SpotifyUser? {
        logger.info { "loadUserData [ Loading user data ...]" }

        if (this.currentUser == null) {
            val getCurrentUsersProfileRequest = spotifyApi.currentUsersProfile.build()
            val user = getCurrentUsersProfileRequest.execute()
            logger.info { "loadUserData []" }
            this.currentUser = SpotifyUser(
                user.id,
                user.email,
                user.displayName,
                user.images[0].url,
                user.uri
            )
        }

        logger.info { "loadUserData [ OK ]" }

        return this.currentUser
    }

}