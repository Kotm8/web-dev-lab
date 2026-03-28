package com.web.lab.Jwt;

import com.web.lab.Jwt.entity.AccessToken;
import com.web.lab.Jwt.entity.RefreshToken;
import com.web.lab.Jwt.repository.AccessTokenRepository;
import com.web.lab.Jwt.repository.RefreshTokenRepository;
import com.web.lab.user.entity.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;


@Service
public class JwtService {

    private final BCryptPasswordEncoder passwordEncoder;
    @Value("${jwt.secret.access}")
    private String access_secret;
    @Value("${jwt.secret.access.expiration}")
    private long access_secret_expiration;
    @Value("${jwt.secret.refresh}")
    private String refresh_secret;
    @Value("${jwt.secret.refresh.expiration}")
    private long refresh_secret_expiration;

    private final RefreshTokenRepository refreshTokenRepository;
    private final AccessTokenRepository accessTokenRepository;

    public JwtService(RefreshTokenRepository refreshTokenRepository,
                      AccessTokenRepository accessTokenRepository, BCryptPasswordEncoder passwordEncoder) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.accessTokenRepository = accessTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }
    private SecretKey getSigningKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UserEntity user) {
        String token =  Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + access_secret_expiration))
                .signWith(getSigningKey(access_secret))
                .compact();
        AccessToken tokenEntity = new AccessToken();
        tokenEntity.setToken(token);
        tokenEntity.setUser(user);
        tokenEntity.setExpiryDate(Instant.now().plusMillis(access_secret_expiration));
        tokenEntity.setRevoked(false);
        accessTokenRepository.save(tokenEntity);
        return token;
    }

    public String generateRefreshToken(UserEntity user) {
        String token = Jwts.builder()
                        .subject(user.getEmail())
                        .claim("role", user.getRole())
                        .issuedAt(new Date())
                        .expiration(new Date(System.currentTimeMillis() + refresh_secret_expiration))
                        .signWith(getSigningKey(refresh_secret))
                        .compact();

        RefreshToken tokenEntity = new RefreshToken();
        tokenEntity.setToken(token);
        tokenEntity.setUser(user);
        tokenEntity.setExpiryDate(Instant.now().plusMillis(refresh_secret_expiration));
        tokenEntity.setRevoked(false);
        refreshTokenRepository.save(tokenEntity);
        return token;
    }

    public String extractEmailAccess(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey(access_secret))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }
    public String extractEmailRefresh(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey(refresh_secret))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    public boolean isTokenValidAccess(String token, String email) {
        String tokenEmail = extractEmailAccess(token);
        return tokenEmail.equals(email);
    }

    public boolean isTokenValidRefresh(String token, String email) {
        String tokenEmail = extractEmailRefresh(token);
        return tokenEmail.equals(email);
    }
}
