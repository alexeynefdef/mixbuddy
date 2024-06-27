package org.anefdev.mixbuddy.controller

import org.anefdev.mixbuddy.model.MusicPlaylist
import org.anefdev.mixbuddy.conf.SpotifyConfig
import org.anefdev.mixbuddy.model.MusicTrack
import org.anefdev.mixbuddy.model.SpotifyUser
import org.anefdev.mixbuddy.service.MixBuddyService
import lombok.SneakyThrows
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.ModelAndView


@RestController
@RequestMapping("/flowtherock/api")
class MixBuddyController(private val config: SpotifyConfig,
                         private val service: MixBuddyService) {

    private val LOGGER: Logger = LoggerFactory.getLogger(MixBuddyController::class.java)

    @GetMapping(path = ["/authorize"])
    @SneakyThrows
    fun authorize(): ModelAndView {
        LOGGER.info("New login with Spotify ...")
        return ModelAndView("redirect:" + service.getAuthorizationCodeUri())
    }

    @GetMapping(path = ["/callback"])
    fun callback(@RequestParam(value = "code") code: String?): ModelAndView {
        LOGGER.info("Get authorization token ...")
        if (code != null) {
            service.setAuthorizationToken(code)
        }
        return ModelAndView("redirect:${config.callbackUrl}")
    }

    @GetMapping(path = ["/user"])
    fun getUserData(): SpotifyUser? {
        LOGGER.info("Get user data ...")
        return service.loadUserData()
    }

    @GetMapping(path = ["/playlists"])
    fun getPlaylists(): List<MusicPlaylist>? {
        return service.loadAllUsersPlaylists()
    }

    @GetMapping(path = ["/playlist/load"])
    @SneakyThrows
    fun loadPlaylist(@RequestParam(value = "playlistId") playlistId: String?): List<MusicTrack>? {
        val trackList = service.loadPlaylist(playlistId.toString()).orEmpty()
        return trackList
    }

    @GetMapping(path = ["/playlist/sort"])
    @SneakyThrows
    fun sort(@RequestParam(value = "trackId") trackId: String?): List<MusicTrack> {
        return service.sortPlaylist(trackId)
    }

}