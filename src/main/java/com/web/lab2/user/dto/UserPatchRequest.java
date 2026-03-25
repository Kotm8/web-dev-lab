package com.web.lab2.user.dto;

import com.web.lab2.user.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserPatchRequest {

    @Email
    private String email;

    @Size(min = 1, max = 50)
    private String username;

    @Size(min = 4, max = 100)
    private String password;

    private Role role;
}
