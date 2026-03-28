package com.web.lab.auth.service;

import com.web.lab.auth.dto.AuthResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

public interface OAuthService {
    String buildAuthorizationUrl(String provider, HttpServletResponse response);
    AuthResponse handleCallback(String provider, String code, String state,
                                HttpServletRequest request,
                                HttpServletResponse response);
}