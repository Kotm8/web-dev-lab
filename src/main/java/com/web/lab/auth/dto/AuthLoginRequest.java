package com.web.lab.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthLoginRequest {
    @Email
    @NotBlank
    private String email;

    @NotBlank
    @Size(min = 4, max = 100)
    private String password;
}
