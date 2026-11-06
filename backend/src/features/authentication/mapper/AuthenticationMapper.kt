package nl.example.backend.features.authentication.mapper

import nl.example.backend.features.authentication.dto.AuthenticationResponse
import nl.example.backend.features.user.mapper.toResponse
import nl.example.backend.features.user.model.User

fun User.toAuthenticationResponse(accessToken: String, expiresInSeconds: Long): AuthenticationResponse =
    AuthenticationResponse(
        accessToken = accessToken,
        tokenType = "Bearer",
        expiresIn = expiresInSeconds,
        user = toResponse()
    )
