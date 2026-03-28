package com.web.lab.user.dto;

import com.web.lab.user.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserPutRequest {

    @Email
    @NotBlank
    private String email;

    @Size(min = 1, max = 50)
    @NotBlank
    private String username;

    @Size(min = 4, max = 100)
    @NotBlank
    private String password;

    @NotNull
    private Role role;
}
