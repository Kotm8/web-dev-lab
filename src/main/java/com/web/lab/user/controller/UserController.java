package com.web.lab.user.controller;

import com.web.lab.user.dto.*;
import com.web.lab.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Users", description = "Operations about user")
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @SecurityRequirement(name = "cookieAuth")
    @Operation(summary = "Get current user")
    @GetMapping("/me")
    public UserRegisterResponse getMe(Authentication authentication) {
        return userService.getByEmail(authentication.getName());
    }

    @SecurityRequirement(name = "cookieAuth")
    @Operation(summary = "Get all users with pagination")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public UserPagedResponse<UserRegisterResponse> getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit
    ) {
        if (limit > 100) {
            limit = 100;
        }

        return userService.getUsers(page, limit);
    }

    @SecurityRequirement(name = "cookieAuth")
    @Operation(summary = "Get user by ID")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public UserRegisterResponse getUser(@PathVariable UUID id) {
        return userService.getUser(id);
    }

    @SecurityRequirement(name = "cookieAuth")
    @Operation(summary = "Update current user fully")
    @PutMapping("/me")
    public UserRegisterResponse putMe(Authentication authentication,
                                      @Valid @RequestBody UserPutRequest dto) {
        return userService.putByEmail(authentication.getName(), dto);
    }

    @SecurityRequirement(name = "cookieAuth")
    @Operation(summary = "Update user fully")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public UserRegisterResponse putUser(@PathVariable UUID id,
                                        @Valid @RequestBody UserPutRequest dto) {
        return userService.putUser(id, dto);
    }

    @SecurityRequirement(name = "cookieAuth")
    @Operation(summary = "Update current user partially")
    @PatchMapping("/me")
    public UserRegisterResponse patchMe(Authentication authentication,
                                        @Valid @RequestBody UserPatchRequest dto) {
        return userService.patchByEmail(authentication.getName(), dto);
    }

    @SecurityRequirement(name = "cookieAuth")
    @Operation(summary = "Update user partially")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}")
    public UserRegisterResponse patchUser(@PathVariable UUID id,
                                          @Valid @RequestBody UserPatchRequest dto) {
        return userService.patchUser(id, dto);
    }

    @SecurityRequirement(name = "cookieAuth")
    @Operation(summary = "Delete current user")
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMe(Authentication authentication) {
        userService.softDeleteByEmail(authentication.getName());
    }

    @SecurityRequirement(name = "cookieAuth")
    @Operation(summary = "Delete user")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable UUID id) {
        userService.softDeleteUser(id);
    }
}