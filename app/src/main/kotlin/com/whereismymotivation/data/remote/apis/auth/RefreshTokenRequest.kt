package com.whereismymotivation.data.remote.apis.auth

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RefreshTokenRequest(
    @Json(name = "refreshToken")
    var refreshToken: String
)