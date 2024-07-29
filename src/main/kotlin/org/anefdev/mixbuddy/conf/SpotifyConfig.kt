package org.anefdev.mixbuddy.conf

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
data class SpotifyConfig(
    @Value("\${spotify.client-id}")
    val clientId: String,

    @Value("\${spotify.client-secret}")
    val clientSecret: String,

    @Value("\${spotify.callback-url}")
    val callbackUrl: String,

    @Value("\${spotify.redirect-url-web-client}")
    val redirectWebClient: String
)

