package com.web.lab.auth.service;

import com.web.lab.Jwt.JwtService;
import com.web.lab.Jwt.entity.AccessToken;
import com.web.lab.Jwt.entity.RefreshToken;
import com.web.lab.Jwt.repository.AccessTokenRepository;
import com.web.lab.auth.dto.AuthLoginRequest;
import com.web.lab.auth.dto.AuthResponse;
import com.web.lab.auth.dto.AuthRegisterRequest;
import com.web.lab.Jwt.repository.RefreshTokenRepository;
import com.web.lab.auth.dto.WhoamiResponse;
import com.web.lab.common.mapper.UserMapper;
import com.web.lab.user.dto.UserRegisterRequest;
import com.web.lab.user.entity.UserEntity;
import com.web.lab.user.repository.UserRepository;
import com.web.lab.user.service.UserService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class AuthService {

    private final BCryptPasswordEncoder passwordEncoder;
    private final UserService userService;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AccessTokenRepository accessTokenRepository;
    private final UserRepository userRepository;

    public AuthService(BCryptPasswordEncoder passwordEncoder,
                       UserService userService,
                       JwtService jwtService,
                       RefreshTokenRepository refreshTokenRepository,
                       AccessTokenRepository accessTokenRepository, UserRepository userRepository) {
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.accessTokenRepository = accessTokenRepository;
        this.userRepository = userRepository;
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
        UserEntity user = userService.findByEmail(dto.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

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
            user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        RefreshToken storedRefreshToken = refreshTokenRepository
                .findByUserAndRevokedFalse(user)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));
        if (!passwordEncoder.matches(refreshToken, storedRefreshToken.getToken())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
        storedRefreshToken.setRevoked(true);
        refreshTokenRepository.save(storedRefreshToken);
        accessTokenRepository.revokeAllByUser(user);

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

        UserEntity user;
        try {
            user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        RefreshToken storedRefreshToken = refreshTokenRepository
                .findByUserAndRevokedFalse(user)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));
        if (!passwordEncoder.matches(refreshToken, storedRefreshToken.getToken())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }

        storedRefreshToken.setRevoked(true);
        refreshTokenRepository.save(storedRefreshToken);
        accessTokenRepository.revokeAllByUser(user);
    }

    @Transactional
    public void logoutAll(String refreshToken) {
        String email;
        try {
            email = jwtService.extractEmailRefresh(refreshToken);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        UserEntity user;
        try {
            user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        RefreshToken storedRefreshToken = refreshTokenRepository
                .findByUserAndRevokedFalse(user)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));
        if (!passwordEncoder.matches(refreshToken, storedRefreshToken.getToken())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }

        refreshTokenRepository.revokeAllByUser(user);
        accessTokenRepository.revokeAllByUser(user);
    }

    public WhoamiResponse whoami(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return new WhoamiResponse(
                user.getUsername(),
                user.getEmail(),
                user.getRole()
        );
    }

}
