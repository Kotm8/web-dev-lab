package com.web.lab.auth.service;

import com.web.lab.Jwt.JwtService;
import com.web.lab.Jwt.entity.RefreshToken;
import com.web.lab.Jwt.repository.AccessTokenRepository;
import com.web.lab.auth.dto.AuthLoginRequest;
import com.web.lab.auth.dto.AuthResponse;
import com.web.lab.auth.dto.AuthRegisterRequest;
import com.web.lab.Jwt.repository.RefreshTokenRepository;
import com.web.lab.auth.dto.WhoamiResponse;
import com.web.lab.common.mapper.UserMapper;
import com.web.lab.common.redis.RedisService;
import com.web.lab.user.dto.UserRegisterRequest;
import com.web.lab.user.entity.UserEntity;
import com.web.lab.user.repository.UserRepository;
import com.web.lab.user.service.UserService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;


@Service
public class AuthService {

    private final BCryptPasswordEncoder passwordEncoder;
    private final UserService userService;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AccessTokenRepository accessTokenRepository;
    private final UserRepository userRepository;
    private final RedisService redisService;

    public AuthService(BCryptPasswordEncoder passwordEncoder,
                       UserService userService,
                       JwtService jwtService,
                       RefreshTokenRepository refreshTokenRepository,
                       AccessTokenRepository accessTokenRepository,
                       UserRepository userRepository,
                       RedisService redisService)
    {
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.accessTokenRepository = accessTokenRepository;
        this.userRepository = userRepository;
        this.redisService = redisService;
    }

    public AuthResponse register(AuthRegisterRequest dto) {

        if (userService.findByEmail(dto.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        UserRegisterRequest userDto = UserMapper.fromAuth(dto, dto.getPassword());
        UserEntity user =  userService.createUser(userDto);


        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        return new AuthResponse(
                newAccessToken,
                newRefreshToken
        );
    }
    public AuthResponse login(AuthLoginRequest dto) {
        UserEntity user = userRepository.findByEmailAndDeletedFalse(dto.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        jwtService.revokeUserSessions(user);

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        return new AuthResponse(
                newAccessToken,
                newRefreshToken
        );
    };

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        String email;
        try {
            email = jwtService.extractEmailRefresh(refreshToken);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
        UserEntity user;
        try {
            user = userRepository.findByEmailAndDeletedFalse(email)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        RefreshToken storedRefreshToken = refreshTokenRepository
                .findByTokenAndRevokedFalse(refreshToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (!storedRefreshToken.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        jwtService.revokeUserSessions(user);

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        return new AuthResponse(
                newAccessToken,
                newRefreshToken
        );
    };

    @Transactional
    public void logout(String refreshToken) {
        String email;
        try {
            email = jwtService.extractEmailRefresh(refreshToken);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        UserEntity user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        RefreshToken storedRefreshToken = refreshTokenRepository
                .findByTokenAndRevokedFalse(refreshToken)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (!storedRefreshToken.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        storedRefreshToken.setRevoked(true);
        refreshTokenRepository.save(storedRefreshToken);

        accessTokenRepository.revokeAllByUser(user);

        jwtService.deleteAccessJti(user.getEmail());
        invalidateWhoamiCache(user.getEmail());
    }

    @Transactional
    public void logoutAll(String refreshToken) {
        String email;
        try {
            email = jwtService.extractEmailRefresh(refreshToken);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        UserEntity user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        RefreshToken currentToken = refreshTokenRepository
                .findByTokenAndRevokedFalse(refreshToken)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (!currentToken.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        refreshTokenRepository.revokeAllByUser(user);
        accessTokenRepository.revokeAllByUser(user);
        jwtService.deleteAccessJti(user.getEmail());
        invalidateWhoamiCache(user.getEmail());
    }

    public WhoamiResponse whoami(String email) {
        WhoamiResponse cachedUser = redisService.getJson(generateWhoamiCacheKey(email), WhoamiResponse.class);
        if (cachedUser != null) {
            return cachedUser;
        }

        UserEntity user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        WhoamiResponse response = new WhoamiResponse(
                user.getUsername(),
                user.getEmail(),
                user.getRole()
        );

        redisService.saveJson(generateWhoamiCacheKey(email), response);
        return response;
    }

    private String generateWhoamiCacheKey(String email) {
        return String.format("lab:auth:user:%s:profile", email);
    }

    private void invalidateWhoamiCache(String email) {
        redisService.delete(generateWhoamiCacheKey(email));
    }

}
