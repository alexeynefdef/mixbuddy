package org.anefdev.mixbuddy.conf

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class SpotifyConfig {

    @Value("\${spotify.client-secret}")
    lateinit var clientId: String

    @Value("\${spotify.client-secret}")
    lateinit var clientSecret: String

    @Value("\${spotify.callback-url}")
    lateinit var callbackUrl: String

}

