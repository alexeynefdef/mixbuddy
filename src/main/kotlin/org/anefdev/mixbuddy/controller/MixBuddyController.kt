package org.anefdev.mixbuddy.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import org.anefdev.mixbuddy.conf.SpotifyConfig
import org.anefdev.mixbuddy.model.MusicPlaylist
import org.anefdev.mixbuddy.model.MusicTrack
import org.anefdev.mixbuddy.model.SpotifyUser
import org.anefdev.mixbuddy.service.MixBuddyService
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.ModelAndView

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/flowtherock/api")
@CrossOrigin("*")
class MixBuddyController(private val config: SpotifyConfig,
                         private val service: MixBuddyService) {

    @GetMapping(path = ["/authorize"])
    fun authorize(): ModelAndView {
        logger.info { "New login with Spotify ..." }
        return ModelAndView("redirect:" + service.getAuthorizationCodeUri().toString())
    }

    @GetMapping(path = ["/callback"])
    fun callback(@RequestParam(value = "code") code: String?): ModelAndView {
        logger.info { "Get authorization token ..." }
        if (code != null) {
            service.setAuthorizationToken(code)
        }
        return ModelAndView("redirect:${config.redirectWebClient}")
    }

    @GetMapping(path = ["/user"])
    fun getUserData(): SpotifyUser? {
        logger.info { "Get user data ..." }
        return service.loadUserData()
    }

    @GetMapping(path = ["/playlists"])
    fun getPlaylists(): List<MusicPlaylist>? {
        logger.info { "Get user playlists ..." }
        return service.loadAllUsersPlaylists()
    }

    @GetMapping(path = ["/playlist/load"])
    fun loadPlaylist(@RequestParam(value = "playlistId") playlistId: String?): List<MusicTrack>? {
        logger.info { "Load playlist ..." }
        val trackList = service.loadPlaylist(playlistId.toString()).orEmpty()
        return trackList
    }

    @GetMapping(path = ["/playlist/sort"])
    fun sort(@RequestParam(value = "trackId") trackId: String?): List<MusicTrack> {
        logger.info { "Sort playlist ..." }
        return service.sortPlaylist(trackId)
    }

}