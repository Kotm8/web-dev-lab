package com.web.lab.common;

import com.web.lab.Jwt.JwtService;
import com.web.lab.Jwt.entity.AccessToken;
import com.web.lab.Jwt.repository.AccessTokenRepository;
import com.web.lab.common.redis.RedisService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final RedisService redisService;
    private final AccessTokenRepository accessTokenRepository;

    public JwtAuthFilter(JwtService jwtService, RedisService redisService, AccessTokenRepository accessTokenRepository) {
        this.jwtService = jwtService;
        this.redisService = redisService;
        this.accessTokenRepository = accessTokenRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        return path.startsWith("/auth/oauth/")
                || path.startsWith("/auth/login")
                || path.startsWith("/auth/register")
                || path.startsWith("/auth/refresh")
                || path.startsWith("/swagger-ui/")
                || path.startsWith("/v3/api-docs")
                || path.equals("/swagger-ui.html")
                || path.equals("/test-success");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = CookieUtils.getCookie(request, "accessToken");

        if (token == null || token.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            JwtService.AccessTokenClaims claims = jwtService.extractAccessClaims(token);
            String email = claims.email();
            String jti = claims.jti();
            String role = claims.role();

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                String stored = redisService.get(String.format("wp:auth:user:%s:access:jti", email));

                if (stored != null && stored.equals(jti)) {
                    // cache hit
                    List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(email, null, authorities);
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else {
                    // cache miss fallback to DB
                    AccessToken tokenEntity = accessTokenRepository
                            .findByTokenAndRevokedFalse(token)
                            .orElseThrow(() -> new RuntimeException("Invalid Access token"));

                    jwtService.saveToJtiToRedis(email, jti);
                    List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(email, null, authorities);
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

}