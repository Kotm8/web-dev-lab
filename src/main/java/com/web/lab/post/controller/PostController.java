package com.web.lab.post.controller;

import com.web.lab.post.dto.PostCreateRequest;
import com.web.lab.post.dto.PostPagedResponse;
import com.web.lab.post.dto.PostPatchRequest;
import com.web.lab.post.dto.PostPutRequest;
import com.web.lab.post.dto.PostResponse;
import com.web.lab.post.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Posts", description = "Operations about posts")
@RestController
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @SecurityRequirement(name = "cookieAuth")
    @Operation(summary = "Create post")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse createPost(Authentication authentication,
                                   @Valid @RequestBody PostCreateRequest dto) {
        return postService.createPost(authentication.getName(), dto);
    }

    @SecurityRequirement(name = "cookieAuth")
    @Operation(summary = "Get all posts with pagination")
    @GetMapping
    public PostPagedResponse<PostResponse> getPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit
    ) {
        if (limit > 100) {
            limit = 100;
        }

        return postService.getPosts(page, limit);
    }

    @SecurityRequirement(name = "cookieAuth")
    @Operation(summary = "Get post by ID")
    @GetMapping("/{id}")
    public PostResponse getPost(@PathVariable UUID id) {
        return postService.getPost(id);
    }

    @SecurityRequirement(name = "cookieAuth")
    @Operation(summary = "Update post fully")
    @PutMapping("/{id}")
    public PostResponse putPost(Authentication authentication,
                                @PathVariable UUID id,
                                @Valid @RequestBody PostPutRequest dto) {
        return postService.putPost(id, authentication.getName(), isAdmin(authentication), dto);
    }

    @SecurityRequirement(name = "cookieAuth")
    @Operation(summary = "Update post partially")
    @PatchMapping("/{id}")
    public PostResponse patchPost(Authentication authentication,
                                  @PathVariable UUID id,
                                  @Valid @RequestBody PostPatchRequest dto) {
        return postService.patchPost(id, authentication.getName(), isAdmin(authentication), dto);
    }

    @SecurityRequirement(name = "cookieAuth")
    @Operation(summary = "Delete post")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(Authentication authentication, @PathVariable UUID id) {
        postService.softDeletePost(id, authentication.getName(), isAdmin(authentication));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
