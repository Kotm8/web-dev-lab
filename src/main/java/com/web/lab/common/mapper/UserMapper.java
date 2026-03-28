package com.web.lab.common.mapper;

import com.web.lab.auth.dto.AuthRegisterRequest;
import com.web.lab.user.dto.UserRegisterRequest;

public class UserMapper {
    public static UserRegisterRequest fromAuth(AuthRegisterRequest dto, String encodedPassword) {
        UserRegisterRequest userDto = new UserRegisterRequest();
        userDto.setEmail(dto.getEmail());
        userDto.setPassword(encodedPassword);
        userDto.setUsername(dto.getUsername());
        return userDto;
    }
}
