package com.web.lab.auth.dto;

import com.web.lab.user.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WhoamiResponse {
    private String username;
    private String email;
    private Role role;
}
