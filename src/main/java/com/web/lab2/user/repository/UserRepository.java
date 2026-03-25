package com.web.lab2.user.repository;

import com.web.lab2.user.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {


    Optional<UserEntity> findByEmail(String email);

    Page<UserEntity> findByDeletedFalse(Pageable pageable);

    Optional<UserEntity> findByIdAndDeletedFalse(UUID id);

}