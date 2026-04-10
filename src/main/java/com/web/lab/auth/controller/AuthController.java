package com.web.lab.auth.controller;


import com.web.lab.auth.dto.AuthLoginRequest;
import com.web.lab.auth.dto.AuthResponse;
import com.web.lab.auth.dto.WhoamiResponse;
import com.web.lab.auth.service.AuthService;
import com.web.lab.auth.dto.AuthRegisterRequest;
import com.web.lab.auth.service.OAuthService;
import com.web.lab.common.CookieUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@Tag(name = "Auth", description = "Auth Services")
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Value("${app.frontend.oauth-success-url}")
    private String frontendSuccessUrl;
    private final AuthService authService;
    private final OAuthService oAuthService;
    public AuthController(AuthService authService, OAuthService oAuthService) {

        this.authService = authService;
        this.oAuthService = oAuthService;
    }

    @Operation(summary = "Register a new user", description = "Creates a new user account")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody AuthRegisterRequest dto,
                                                 HttpServletRequest request,
                                                 HttpServletResponse response) {
        AuthResponse authResponse = authService.register(dto);

        CookieUtils.addAccessTokenCookie(response, authResponse.getAccess_token(), request.isSecure());
        CookieUtils.addRefreshTokenCookie(response, authResponse.getRefresh_token(), request.isSecure());

        return ResponseEntity.ok(authResponse);
    }

    @Operation(summary = "Login a new user", description = "Login a user with jwt token")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthLoginRequest dto,
                                              HttpServletRequest request,
                                              HttpServletResponse response) {
        AuthResponse authResponse = authService.login(dto);

        CookieUtils.addAccessTokenCookie(response, authResponse.getAccess_token(), request.isSecure());
        CookieUtils.addRefreshTokenCookie(response, authResponse.getRefresh_token(), request.isSecure());

        return ResponseEntity.ok(authResponse);
    }

    @Operation(summary = "Refresh access token", description = "Refresh a user's access token")
    @SecurityRequirement(name = "cookieAuth")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request,
                                                HttpServletResponse response) {
        String refreshToken = CookieUtils.getCookie(request, "refreshToken");

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        AuthResponse authResponse = authService.refresh(refreshToken);

        CookieUtils.addAccessTokenCookie(response, authResponse.getAccess_token(), request.isSecure());
        CookieUtils.addRefreshTokenCookie(response, authResponse.getRefresh_token(), request.isSecure());

        return ResponseEntity.ok(authResponse);
    }

    @Operation(summary = "Log user out", description = "Revoke current session")
    @SecurityRequirement(name = "cookieAuth")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request,
                       HttpServletResponse response) {

        String refreshToken = CookieUtils.getCookie(request, "refreshToken");

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No refresh token");
        }

        authService.logout(refreshToken);

        CookieUtils.clearCookie(response, "accessToken", request.isSecure());
        CookieUtils.clearCookie(response, "refreshToken", request.isSecure());
    }

    @Operation(summary = "Log user sessions out", description = "Revoke all sessions")
    @SecurityRequirement(name = "cookieAuth")
    @PostMapping("/logout-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutAll(HttpServletRequest request,
                          HttpServletResponse response) {

        String refreshToken = CookieUtils.getCookie(request, "refreshToken");

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No refresh token");
        }

        authService.logoutAll(refreshToken);

        CookieUtils.clearCookie(response, "accessToken", request.isSecure());
        CookieUtils.clearCookie(response, "refreshToken", request.isSecure());
    }


    @Operation(summary = "OAuth init", description = "Initiate OAuth login")
    @GetMapping("/oauth/{provider}")
    public void oauthInit(
            @PathVariable String provider,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        String redirectUrl = oAuthService.buildAuthorizationUrl(provider, request, response);
        response.sendRedirect(redirectUrl);
    }

    @Operation(summary = "OAuth callback", description = "Handle OAuth provider callback")
    @GetMapping("/oauth/{provider}/callback")
    public void oauthCallback(
            @PathVariable String provider,
            @RequestParam("code") String code,
            @RequestParam(value = "state", required = false) String state,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        AuthResponse authResponse = oAuthService.handleCallback(provider, code, state, request, response);

        CookieUtils.addAccessTokenCookie(response, authResponse.getAccess_token(), request.isSecure());
        CookieUtils.addRefreshTokenCookie(response, authResponse.getRefresh_token(), request.isSecure());

        response.sendRedirect(frontendSuccessUrl);
    }

    @Operation(summary = "Who am I?")
    @SecurityRequirement(name = "cookieAuth")
    @GetMapping("/whoami")
    public WhoamiResponse whoami(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }


        return authService.whoami(authentication.getName());
    }


}
