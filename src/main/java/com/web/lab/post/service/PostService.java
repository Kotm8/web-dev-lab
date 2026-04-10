package com.web.lab.post.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.web.lab.common.redis.RedisService;
import com.web.lab.post.dto.PostCreateRequest;
import com.web.lab.post.dto.PostPagedResponse;
import com.web.lab.post.dto.PostPatchRequest;
import com.web.lab.post.dto.PostPutRequest;
import com.web.lab.post.dto.PostResponse;
import com.web.lab.post.entity.PostEntity;
import com.web.lab.post.repository.PostRepository;
import com.web.lab.user.dto.PaginationMeta;
import com.web.lab.user.entity.UserEntity;
import com.web.lab.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class PostService {

    private static final String POST_LIST_CACHE_PATTERN = "lab:posts:list:*";

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final RedisService redisService;

    public PostService(PostRepository postRepository, UserRepository userRepository, RedisService redisService) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.redisService = redisService;
    }

    public PostResponse createPost(String requesterEmail, PostCreateRequest dto) {
        UserEntity author = getActiveUserByEmail(requesterEmail);

        PostEntity post = new PostEntity();
        post.setTitle(dto.getTitle());
        post.setCreatedBy(author);

        PostResponse response = mapToResponse(postRepository.save(post));
        invalidatePostListCaches();
        return response;
    }

    public PostPagedResponse<PostResponse> getPosts(int page, int limit) {
        String cacheKey = generatePostListCacheKey(page, limit);
        PostPagedResponse<PostResponse> cachedPosts = redisService.getJson(
                cacheKey,
                new TypeReference<PostPagedResponse<PostResponse>>() {}
        );
        if (cachedPosts != null) {
            return cachedPosts;
        }

        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<PostEntity> postPage = postRepository.findByDeletedFalse(pageable);

        List<PostResponse> posts = postPage.getContent()
                .stream()
                .map(this::mapToResponse)
                .toList();

        PaginationMeta meta = new PaginationMeta();
        meta.setTotal(postPage.getTotalElements());
        meta.setPage(page);
        meta.setLimit(limit);
        meta.setTotalPages(postPage.getTotalPages());

        PostPagedResponse<PostResponse> response = new PostPagedResponse<>();
        response.setData(posts);
        response.setMeta(meta);

        redisService.saveJson(cacheKey, response);
        return response;
    }

    public PostResponse getPost(UUID id) {
        String cacheKey = generatePostItemCacheKey(id);
        PostResponse cachedPost = redisService.getJson(cacheKey, PostResponse.class);
        if (cachedPost != null) {
            return cachedPost;
        }

        PostEntity post = getActivePost(id);
        PostResponse response = mapToResponse(post);
        redisService.saveJson(cacheKey, response);
        return response;
    }

    public PostResponse putPost(UUID id, String requesterEmail, boolean admin, PostPutRequest dto) {
        PostEntity post = getActivePost(id);
        validateAuthorAccess(post, requesterEmail, admin);

        post.setTitle(dto.getTitle());
        PostResponse response = mapToResponse(postRepository.save(post));
        invalidatePostCaches(post.getId());
        return response;
    }

    public PostResponse patchPost(UUID id, String requesterEmail, boolean admin, PostPatchRequest dto) {
        PostEntity post = getActivePost(id);
        validateAuthorAccess(post, requesterEmail, admin);

        if (dto.getTitle() != null) {
            post.setTitle(dto.getTitle());
        }

        PostResponse response = mapToResponse(postRepository.save(post));
        invalidatePostCaches(post.getId());
        return response;
    }

    public void softDeletePost(UUID id, String requesterEmail, boolean admin) {
        PostEntity post = getActivePost(id);
        validateAuthorAccess(post, requesterEmail, admin);

        post.setDeleted(true);
        postRepository.save(post);
        invalidatePostCaches(post.getId());
    }

    private PostEntity getActivePost(UUID id) {
        return postRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
    }

    private UserEntity getActiveUserByEmail(String email) {
        return userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private void validateAuthorAccess(PostEntity post, String requesterEmail, boolean admin) {
        if (admin) {
            return;
        }

        if (!post.getCreatedBy().getEmail().equals(requesterEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot modify this post");
        }
    }

    private PostResponse mapToResponse(PostEntity post) {
        PostResponse response = new PostResponse();
        response.setId(post.getId());
        response.setTitle(post.getTitle());
        response.setCreatedById(post.getCreatedBy().getId());
        response.setCreatedByEmail(post.getCreatedBy().getEmail());
        response.setDeleted(post.getDeleted());
        response.setCreatedAt(post.getCreatedAt());
        response.setUpdatedAt(post.getUpdatedAt());
        return response;
    }

    private String generatePostListCacheKey(int page, int limit) {
        return String.format("lab:posts:list:page:%d:limit:%d", page, limit);
    }

    private String generatePostItemCacheKey(UUID id) {
        return String.format("lab:posts:item:%s", id);
    }

    private void invalidatePostListCaches() {
        redisService.deleteByPattern(POST_LIST_CACHE_PATTERN);
    }

    private void invalidatePostCaches(UUID id) {
        invalidatePostListCaches();
        redisService.delete(generatePostItemCacheKey(id));
    }
}
