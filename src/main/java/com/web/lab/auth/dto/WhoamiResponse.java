package com.web.lab.auth.dto;

import com.web.lab.user.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WhoamiResponse {
    private String username;
    private String email;
    private Role role;
}
