package com.web.lab.auth.dto.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record YandexUserInfoResponse(
        String id,
        String login,
        @JsonProperty("default_email") String defaultEmail,
        @JsonProperty("real_name") String realName,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name") String lastName
) {
}
