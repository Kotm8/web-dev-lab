package com.web.lab.Jwt;

import com.web.lab.Jwt.entity.AccessToken;
import com.web.lab.Jwt.entity.RefreshToken;
import com.web.lab.Jwt.repository.AccessTokenRepository;
import com.web.lab.Jwt.repository.RefreshTokenRepository;
import com.web.lab.common.redis.RedisService;
import com.web.lab.user.entity.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


@Service
public class JwtService {

    @Value("${jwt.secret.access}")
    private String access_secret;
    @Value("${jwt.secret.access.expiration}")
    private long access_secret_expiration;
    @Value("${jwt.secret.refresh}")
    private String refresh_secret;
    @Value("${jwt.secret.refresh.expiration}")
    private long refresh_secret_expiration;

    private final AccessTokenRepository accessTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RedisService redisService;

    public JwtService(RefreshTokenRepository refreshTokenRepository,
                      AccessTokenRepository accessTokenRepository, RedisService redisService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.accessTokenRepository = accessTokenRepository;
        this.redisService = redisService;
    }
    private SecretKey getSigningKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    public void saveToJtiToRedis(String email, String jti) {
        redisService.save(String.format("wp:auth:user:%s:access:jti", email), jti, access_secret_expiration, TimeUnit.MILLISECONDS);

    }
    public String generateAccessToken(UserEntity user) {
        String jti = UUID.randomUUID().toString();
        String token =  Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole())
                .id(jti)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + access_secret_expiration))
                .signWith(getSigningKey(access_secret))
                .compact();
        AccessToken tokenEntity = new AccessToken();
        tokenEntity.setToken(token);
        tokenEntity.setUser(user);
        tokenEntity.setExpiryDate(Instant.now().plusMillis(access_secret_expiration));
        tokenEntity.setRevoked(false);
        saveToJtiToRedis(user.getEmail(), jti);
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

    public record AccessTokenClaims(String email, String jti, String role) {}

    public AccessTokenClaims extractAccessClaims(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey(access_secret))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return new AccessTokenClaims(
                claims.getSubject(),
                claims.getId(),
                claims.get("role", String.class)
        );
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

    public String extractJtiAccess(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey(access_secret))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getId();
    }

    public String extractRoleAccess(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey(access_secret))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("role", String.class);
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
