package com.web.lab.auth.dto.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record YandexTokenResponse(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("refresh_token") String refreshToken,
    @JsonProperty("token_type") String tokenType,
    @JsonProperty("expires_in") Long expiresIn
){}
