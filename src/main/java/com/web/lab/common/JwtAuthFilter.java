package com.web.lab.common;

import com.web.lab.Jwt.JwtService;
import com.web.lab.Jwt.entity.AccessToken;
import com.web.lab.Jwt.repository.AccessTokenRepository;
import com.web.lab.common.redis.RedisService;
import com.web.lab.user.entity.UserEntity;
import com.web.lab.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final RedisService redisService;
    private final AccessTokenRepository accessTokenRepository;
    private final UserRepository userRepository;

    public JwtAuthFilter(
            JwtService jwtService,
            RedisService redisService,
            AccessTokenRepository accessTokenRepository,
            UserRepository userRepository
    ) {
        this.jwtService = jwtService;
        this.redisService = redisService;
        this.accessTokenRepository = accessTokenRepository;
        this.userRepository = userRepository;
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

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserEntity user = userRepository.findByEmailAndDeletedFalse(email)
                        .orElseThrow(() -> new RuntimeException("User not found"));
                String stored = redisService.get(String.format("lab:auth:user:%s:access:jti", email));

                if (stored != null) {
                    if (!stored.equals(jti)) {
                        throw new RuntimeException("Invalid Access token");
                    }

                    SecurityContextHolder.getContext().setAuthentication(buildAuthentication(request, user));
                } else {
                    AccessToken tokenEntity = accessTokenRepository
                            .findByTokenAndRevokedFalse(token)
                            .orElseThrow(() -> new RuntimeException("Invalid Access token"));

                    if (!tokenEntity.getUser().getId().equals(user.getId())) {
                        throw new RuntimeException("Invalid Access token");
                    }

                    jwtService.saveToJtiToRedis(email, jti);
                    SecurityContextHolder.getContext().setAuthentication(buildAuthentication(request, user));
                }
            }
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private UsernamePasswordAuthenticationToken buildAuthentication(HttpServletRequest request, UserEntity user) {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user.getEmail(), null, authorities);
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        return auth;
    }

}
