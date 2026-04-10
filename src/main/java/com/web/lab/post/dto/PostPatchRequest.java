package com.web.lab.post.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostPatchRequest {

    @Size(min = 1, max = 255)
    private String title;
}
