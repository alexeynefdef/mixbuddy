package org.anefdev.mixbuddy.service

import org.anefdev.mixbuddy.model.MusicPlaylist
import org.anefdev.mixbuddy.model.MusicTrack
import org.anefdev.mixbuddy.model.SpotifyUser
import lombok.NoArgsConstructor
import lombok.SneakyThrows
import org.anefdev.mixbuddy.conf.SpotifyConfig
import org.anefdev.mixbuddy.util.PlaylistParser
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import se.michaelthelin.spotify.SpotifyApi
import se.michaelthelin.spotify.SpotifyHttpManager
import se.michaelthelin.spotify.enums.AuthorizationScope
import se.michaelthelin.spotify.model_objects.specification.Paging
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack
import se.michaelthelin.spotify.model_objects.specification.Track
import java.net.URI
import java.util.*

@Service
@NoArgsConstructor
class MixBuddyService(
    private val config: SpotifyConfig,
    private val playlistParser: PlaylistParser
) {
    private val LOGGER: Logger = LoggerFactory.getLogger(MixBuddyService::class.java)

    private var allPlaylists: List<MusicPlaylist>? = null
    private var playlistParsed: List<MusicTrack>? = null
    private var currentUser: SpotifyUser? = null
    private lateinit var spotifyApi: SpotifyApi

    /**
     * Gets URI for authorization with Spotify.
     * <br></br>
     * Sync
     * @return Authorization code URI
     */
    fun getAuthorizationCodeUri(): URI {
        LOGGER.info("getAuthorisationCodeURI [ Get authorization code URI ... ]")
        val CALLBACK_URI = SpotifyHttpManager.makeUri(config.callbackUrl)

        this.spotifyApi = SpotifyApi.Builder()
            .setClientId(config.clientId)
            .setClientSecret(config.clientSecret)
            .setRedirectUri(CALLBACK_URI)
            .build()

        val authorizationCodeUriRequest =
            spotifyApi.authorizationCodeUri()
                .scope(
                    AuthorizationScope.USER_LIBRARY_READ,
                    AuthorizationScope.USER_READ_PRIVATE,
                    AuthorizationScope.USER_READ_EMAIL
                )
                .show_dialog(true)
                .build()
        val uri = authorizationCodeUriRequest.execute()
        LOGGER.info("getAuthorisationCodeURI [ URI: $uri ]")
        LOGGER.info("getAuthorisationCodeURI [ OK ]")
        return uri
    }

    /**
     * Gets and saves the access and refresh tokens in SpotifyApi object.
     * <br></br>
     * Sync
     * @param code Authorization code for Spotify
     */
    @SneakyThrows
    fun setAuthorizationToken(code: String) {
        LOGGER.info("setAuthorizationToken [ Get authorization token ...")
        LOGGER.info("setAuthorizationToken [ Authorization code: $code ]")

        val authorizationCodeRequest = spotifyApi.authorizationCode(code).build()
        val authorizationCodeCredentials = authorizationCodeRequest.execute()
        // Set access and refresh token for further "spotifyApi" object usage
        spotifyApi.accessToken = authorizationCodeCredentials.accessToken
        spotifyApi.refreshToken = authorizationCodeCredentials.refreshToken

        LOGGER.info("setAuthorizationToken [ Access token: " + spotifyApi.accessToken + " ]")
        LOGGER.info("setAuthorizationToken [ Refresh token: " + spotifyApi.refreshToken + " ]")
    }

    /**
     * Loads all playlists of the current user.
     * <br></br>
     * Sync
     * @return List of user's playlists
     */
    @SneakyThrows
    fun loadAllUsersPlaylists(): List<MusicPlaylist>? {
        LOGGER.info("loadAllUsersPlaylists [ Loading all user's playlists ... ]")
        val allUserPlaylists: List<MusicPlaylist>
        val currentUserID = currentUser?.id
        val getListOfUsersPlaylistsRequest = spotifyApi
            .getListOfUsersPlaylists(currentUserID)
            .build()
        val playlistSimplifiedPaging = getListOfUsersPlaylistsRequest.execute()
        val allUserPlaylistsSimple = Arrays.stream(playlistSimplifiedPaging.items).toList()
        allUserPlaylists = playlistParser.getAllPlaylists(allUserPlaylistsSimple)

        this.allPlaylists = allUserPlaylists
        LOGGER.info("loadAllUsersPlaylists [ Playlists count: " + allPlaylists!!.size + " ]")
        LOGGER.info("loadAllUsersPlaylists [ OK ]")

        return this.allPlaylists
    }

    /**
     * Loads playlist by passed ID.
     * <br></br>
     * Async
     * @param playlistId ID of the playlist
     * @return List of MusicTrack playlist
     */
    @SneakyThrows
    fun loadPlaylist(playlistId: String): List<MusicTrack>? {
        LOGGER.info("loadPlaylist [ Loading playlist ... ]")
        LOGGER.info("loadPlaylist [ Playlist with ID: $playlistId ]")

        val playlist: Paging<PlaylistTrack> = spotifyApi.getPlaylistsItems(playlistId)
        .build()
            .execute()

        LOGGER.info("loadPlaylist [ Loading playlist with size: " + playlist.total + " ]")

        val playlistFinal = ArrayList(Arrays.stream(playlist.items).toList())
        while (playlist.total > playlistFinal.size) {
            val playlistPagination = spotifyApi.getPlaylistsItems(playlistId)
                .offset(playlistFinal.size)
                .build()
                .execute()
            playlistFinal.addAll(Arrays.stream(playlistPagination.items).toList())
        }

        LOGGER.info("loadPlaylist [ Extracting track-IDs ... ]")
        val playlistNoMetaData: List<MusicTrack> = playlistParser.getTrackListNoMetaData(playlistFinal)



        LOGGER.info("loadPlaylist [ Loading tracks-data ... ]")
        val tracksAnalyzed: MutableList<MusicTrack>? = playlistNoMetaData.stream().map { track ->
            val trackInfo =
                loadTrackInfo(track.id!!)
            val trackAnalysis = this.loadTrackAnalysis(track.id!!)
            val trackToParse = playlistParser.parseTrackInfo(trackAnalysis, trackInfo)
            playlistParser.parseTrackAnalysisKey(trackToParse)
        }.toList()
        this.playlistParsed = tracksAnalyzed
        LOGGER.info("loadPlaylist [ OK ]")
        return this.playlistParsed
    }

    /**
     * Loads song info by passed track ID.
     * <br></br>
     * Async
     * @param trackId ID of the track
     * @return Track song info
     */
    @SneakyThrows
    private fun loadTrackInfo(trackId: String): Track {
        LOGGER.info("loadTrackInfo [ Loading song info ... ]")
        val getTrackRequest = spotifyApi.getTrack(trackId).build()
        val trackFuture = getTrackRequest.executeAsync()
        val track = trackFuture.join()
        LOGGER.info("loadTrackInfo [ Song: $track ]")
        LOGGER.info("loadTrackInfo [ OK ]")
        return track
    }

    /**
     * Loads audio analysis by passed track ID.
     * <br></br>
     * Async
     * @param trackId ID of the track
     * @return MusicTrack audio analysis
     */
    @SneakyThrows
    private fun loadTrackAnalysis(trackId: String): MusicTrack {
        LOGGER.info("loadTrackAnalysis [ Loading audio analysis ... ]")
        val getTrackAnalysisRequest = spotifyApi.getAudioAnalysisForTrack(trackId).build()
        val audioAnalysisFuture = getTrackAnalysisRequest.executeAsync()
        val audioAnalysis = audioAnalysisFuture.join()
        LOGGER.info("loadTrackAnalysis [ Audio analysis: $audioAnalysis ]")
        LOGGER.info("loadTrackAnalysis [ OK ]")
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
        LOGGER.info("sortPlaylist [ Sorting playlist ... ]")
        LOGGER.info("sortPlaylist [ Removing all matched tags ... ]")

        for (track in playlistParsed!!) {
            track.matched = false
        }

        LOGGER.info("sortPlaylist [ Sorting playlist ... ]")
        LOGGER.info("sortPlaylist [ OK ]")

        return playlistParser.sortPlaylist(this.playlistParsed!!, trackId!!)
    }

    /**
     * Loads current session Spotify user's data
     * <br></br>
     * Sync
     * @return current user's data
     */
    @SneakyThrows
    fun loadUserData(): SpotifyUser? {
        LOGGER.info("loadUserData [ Loading user data ...]")

        if (this.currentUser == null) {
            val getCurrentUsersProfileRequest = spotifyApi.currentUsersProfile.build()
            val user = getCurrentUsersProfileRequest.execute()
            LOGGER.info("loadUserData []")
            this.currentUser = SpotifyUser(
                user.id,
                user.email,
                user.displayName,
                user.images[0].url,
                user.uri
            )
        }

        LOGGER.info("loadUserData [ Current user: " + this.currentUser + "]")
        LOGGER.info("loadUserData [ OK ]")

        return this.currentUser
    }

}