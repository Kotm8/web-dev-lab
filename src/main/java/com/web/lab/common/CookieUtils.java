package com.web.lab.common;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

public class CookieUtils {

    private static final String SAME_SITE_LAX = "Lax";

    private CookieUtils() {
    }

    public static String getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    public static void addAccessTokenCookie(HttpServletResponse response, String token, boolean secure) {
        addCookie(response, "accessToken", token, 60 * 15, secure);
    }

    public static void addRefreshTokenCookie(HttpServletResponse response, String token, boolean secure) {
        addCookie(response, "refreshToken", token, 60 * 60 * 24 * 7, secure);
    }

    public static void addOauthStateCookie(HttpServletResponse response, String token, boolean secure) {
        addCookie(response, "oauth_state", token, 300, secure);
    }

    public static void clearCookie(HttpServletResponse response, String name, boolean secure) {
        addCookie(response, name, "", 0, secure);
    }

    private static void addCookie(HttpServletResponse response, String name, String value, long maxAgeSeconds, boolean secure) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .sameSite(SAME_SITE_LAX)
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
