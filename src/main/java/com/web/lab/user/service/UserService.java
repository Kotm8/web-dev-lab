package com.web.lab.user.service;

import com.web.lab.user.entity.Role;
import com.web.lab.user.entity.UserEntity;
import com.web.lab.user.repository.UserRepository;
import com.web.lab.common.redis.RedisService;
import com.web.lab.user.dto.*;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RedisService redisService;

    public UserService(UserRepository userRepository,
                       BCryptPasswordEncoder passwordEncoder,
                       RedisService redisService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.redisService = redisService;
    }


    public UserEntity createUser(UserRegisterRequest dto) {

        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        UserEntity user = new UserEntity();
        user.setEmail(dto.getEmail());
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(Role.USER);

        return userRepository.save(user);
    }

    public UserRegisterResponse putUser(UUID id, UserPutRequest dto) {
        UserEntity user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        String oldEmail = user.getEmail();

        if (!user.getEmail().equals(dto.getEmail())
                && userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        user.setEmail(dto.getEmail());
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(dto.getRole());

        UserEntity savedUser = userRepository.save(user);
        evictWhoamiCache(oldEmail);
        evictWhoamiCache(savedUser.getEmail());
        return returnUserRegisterResponse(savedUser);
    }

    public UserRegisterResponse patchUser(UUID id, UserPatchRequest dto) {
        UserEntity user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        String oldEmail = user.getEmail();

        if (dto.getEmail() != null) {
            Optional<UserEntity> existingUser = userRepository.findByEmail(dto.getEmail());
            if (existingUser.isPresent() && !existingUser.get().getId().equals(user.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
            }
            user.setEmail(dto.getEmail());
        }

        if (dto.getUsername() != null) {
            user.setUsername(dto.getUsername());
        }

        if (dto.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        if (dto.getRole() != null) {
            user.setRole(dto.getRole());
        }

        UserEntity savedUser = userRepository.save(user);
        evictWhoamiCache(oldEmail);
        evictWhoamiCache(savedUser.getEmail());
        return returnUserRegisterResponse(savedUser);
    }

    public void softDeleteUser(UUID id) {
        UserEntity user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setDeleted(true);
        userRepository.save(user);
        evictWhoamiCache(user.getEmail());
    }

    public UserRegisterResponse getUser(UUID id) {
        UserEntity user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return returnUserRegisterResponse(user);
    }

    public UserPagedResponse<UserRegisterResponse> getUsers(int page, int limit) {
        Pageable pageable = PageRequest.of(page - 1, limit);

        Page<UserEntity> userPage = userRepository.findByDeletedFalse(pageable);

        List<UserRegisterResponse> users = userPage.getContent()
                .stream()
                .map(this::mapToDto)
                .toList();

        PaginationMeta meta = new PaginationMeta();
        meta.setTotal(userPage.getTotalElements());
        meta.setPage(page);
        meta.setLimit(limit);
        meta.setTotalPages(userPage.getTotalPages());

        UserPagedResponse<UserRegisterResponse> response = new UserPagedResponse<>();
        response.setData(users);
        response.setMeta(meta);

        return response;
    }

    private UserRegisterResponse mapToDto(UserEntity user) {
        return returnUserRegisterResponse(user);
    }

    private void evictWhoamiCache(String email) {
        redisService.delete(String.format("lab:auth:user:%s:profile", email));
    }

    public Optional<UserEntity> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @NonNull
    private UserRegisterResponse returnUserRegisterResponse(UserEntity savedUser) {
        UserRegisterResponse response = new UserRegisterResponse();
        response.setId(savedUser.getId());
        response.setEmail(savedUser.getEmail());
        response.setUsername(savedUser.getUsername());
        response.setRole(savedUser.getRole());
        response.setCreatedAt(savedUser.getCreatedAt());
        response.setUpdatedAt(savedUser.getUpdatedAt());

        return response;
    }
}
