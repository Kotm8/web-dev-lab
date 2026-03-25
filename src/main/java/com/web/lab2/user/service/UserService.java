package com.web.lab2.user.service;

import com.web.lab2.user.entity.UserEntity;
import com.web.lab2.user.repository.UserRepository;
import com.web.lab2.user.dto.*;
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

    public UserService(UserRepository userRepository,
                       BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserRegisterResponse register(UserRegisterRequest dto) {

        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        UserEntity user = new UserEntity();
        user.setEmail(dto.getEmail());
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));

        UserEntity savedUser = userRepository.save(user);

        return returnUserRegisterResponse(savedUser);
    }

    public UserRegisterResponse putUser(UUID id, UserPutRequest dto) {
        UserEntity user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setEmail(dto.getEmail());
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(dto.getRole());

        UserEntity savedUser = userRepository.save(user);
        return returnUserRegisterResponse(savedUser);
    }

    public UserRegisterResponse patchUser(UUID id, UserPatchRequest dto){
        UserEntity user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (dto.getEmail() != null) {
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
        return returnUserRegisterResponse(savedUser);
    }

    public void softDeleteUser(UUID id) {
        UserEntity user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setDeleted(true);
        userRepository.save(user);
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