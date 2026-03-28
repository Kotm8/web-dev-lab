package com.web.lab.auth.service;


import com.web.lab.Jwt.JwtService;
import com.web.lab.Jwt.repository.AccessTokenRepository;
import com.web.lab.Jwt.repository.RefreshTokenRepository;
import com.web.lab.auth.dto.AuthResponse;
import com.web.lab.auth.dto.oauth.YandexTokenResponse;
import com.web.lab.auth.dto.oauth.YandexUserInfoResponse;
import com.web.lab.common.CookieUtils;
import com.web.lab.user.entity.UserEntity;
import com.web.lab.user.repository.UserRepository;
import com.web.lab.user.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class OAuthServiceImpl implements OAuthService {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RestClient restClient = RestClient.create();

    @Value("${jwt.oauth.client.id}")
    private String clientId;

    @Value("${jwt.oauth.client.secret}")
    private String clientSecret;

    @Value("${jwt.oauth.callback_url}")
    private String callbackUrl;

    @Value("${app.frontend.oauth-success-url}")
    private String frontendSuccessUrl;

    public OAuthServiceImpl(UserRepository userRepository,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Override
    public String buildAuthorizationUrl(String provider, HttpServletResponse response) {
        validateProvider(provider);

        String state = UUID.randomUUID().toString();

        Cookie cookie = new Cookie("oauth_state", state);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(300);

        response.addCookie(cookie);

        return UriComponentsBuilder
                .fromUriString("https://oauth.yandex.ru/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", callbackUrl)
                .queryParam("state", state)
                .toUriString();
    }

    @Override
    public AuthResponse handleCallback(String provider, String code, String state,
                                       HttpServletRequest request,
                                       HttpServletResponse response) {
        validateProvider(provider);

        String storedState = CookieUtils.getCookie(request, "oauth_state");

        if (storedState == null || state == null || !storedState.equals(state)) {
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid OAuth state");
        }

        CookieUtils.clearCookie(response, "oauth_state");

        YandexTokenResponse tokenResponse = exchangeCodeForToken(code);
        if (tokenResponse == null || tokenResponse.accessToken() == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Failed to obtain Yandex access token");
        }

        YandexUserInfoResponse yandexUser = fetchYandexUser(tokenResponse.accessToken());
        if (yandexUser == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Failed to fetch Yandex user profile");
        }

        String email = yandexUser.defaultEmail();
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Yandex account did not provide email");
        }

        UserEntity user = findOrCreateUser(yandexUser);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken);
    }

    private void validateProvider(String provider) {
        if (!"yandex".equalsIgnoreCase(provider)) {
            throw new ResponseStatusException(BAD_REQUEST, "Unsupported OAuth provider: " + provider);
        }
    }

    private YandexTokenResponse exchangeCodeForToken(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        return restClient.post()
                .uri("https://oauth.yandex.com/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(YandexTokenResponse.class);
    }

    private YandexUserInfoResponse fetchYandexUser(String yandexAccessToken) {
        return restClient.get()
                .uri("https://login.yandex.ru/info?format=json")
                .header("Authorization", "OAuth " + yandexAccessToken)
                .retrieve()
                .body(YandexUserInfoResponse.class);
    }


    private UserEntity findOrCreateUser(YandexUserInfoResponse yandexUser) {
        Optional<UserEntity> existing = userRepository.findByEmail(yandexUser.defaultEmail());
        if (existing.isPresent()) {
            return existing.get();
        }

        UserEntity user = new UserEntity();
        user.setEmail(yandexUser.defaultEmail());
        user.setUsername(
                yandexUser.login() != null && !yandexUser.login().isBlank()
                        ? yandexUser.login()
                        : yandexUser.defaultEmail()
        );
        user.setPassword(null);

        return userRepository.save(user);
    }
}