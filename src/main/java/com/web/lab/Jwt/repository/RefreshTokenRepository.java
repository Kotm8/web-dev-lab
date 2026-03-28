package com.web.lab.Jwt.repository;

import com.web.lab.Jwt.entity.RefreshToken;
import com.web.lab.user.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByUserAndRevokedFalse(UserEntity user);
    @Modifying
    @Query("UPDATE RefreshToken a SET a.revoked = true WHERE a.user = :user AND a.revoked = false")
    void revokeAllByUser(@Param("user") UserEntity user);

}