package com.web.lab2.user.controller;

import com.web.lab2.user.dto.*;
import com.web.lab2.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

    @Operation(summary = "Register a new user", description = "Creates a new user account")
    @PostMapping("/register")
    public UserRegisterResponse register(@Valid @RequestBody UserRegisterRequest dto) {
        return userService.register(dto);
    }

    @Operation(summary = "Get user by ID")
    @GetMapping("/{id}")
    public UserRegisterResponse getUser(@PathVariable UUID id) {
        return userService.getUser(id);
    }

    @Operation(summary = "Get all users with pagination")
    @GetMapping("/")
    public UserPagedResponse<UserRegisterResponse> getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return userService.getUsers(page, limit);
    }

    @Operation(summary = "Update user fully")
    @PutMapping("/{id}")
    public UserRegisterResponse putUser(@PathVariable UUID id, @Valid @RequestBody UserPutRequest dto){
        return userService.putUser(id, dto);
    }

    @Operation(summary = "Update user partially")
    @PatchMapping("/{id}")
    public UserRegisterResponse patchUser(@PathVariable UUID id, @Valid @RequestBody UserPatchRequest dto){
        return userService.patchUser(id, dto);
    }

    @Operation(summary = "Delete user")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable UUID id) {
        userService.softDeleteUser(id);
    }
}