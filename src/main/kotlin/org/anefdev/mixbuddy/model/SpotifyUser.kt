package org.anefdev.mixbuddy.model

import lombok.Data

@Data
data class SpotifyUser(
    var id: String,
    var email: String,
    var name: String,
    var img: String,
    var url: String) {

}