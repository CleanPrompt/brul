package nl.brul.backend.features.authentication.mapper

import nl.brul.backend.features.authentication.dto.AuthenticationResponse
import nl.brul.backend.features.user.mapper.toResponse
import nl.brul.backend.features.user.model.User

fun User.toAuthenticationResponse(accessToken: String, expiresInSeconds: Long): AuthenticationResponse =
    AuthenticationResponse(
        accessToken = accessToken,
        tokenType = "Bearer",
        expiresIn = expiresInSeconds,
        user = toResponse()
    )
