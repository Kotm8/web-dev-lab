package com.web.lab.post.repository;

import com.web.lab.post.entity.PostEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PostRepository extends JpaRepository<PostEntity, UUID> {

    Page<PostEntity> findByDeletedFalse(Pageable pageable);

    Optional<PostEntity> findByIdAndDeletedFalse(UUID id);
}
