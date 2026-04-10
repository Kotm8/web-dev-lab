package com.web.lab.Jwt.repository;

import com.web.lab.Jwt.entity.AccessToken;
import com.web.lab.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AccessTokenRepository extends JpaRepository<AccessToken, UUID> {
    Optional<AccessToken> findByTokenAndRevokedFalse(String token);
    @Modifying
    @Query("UPDATE AccessToken a SET a.revoked = true WHERE a.user = :user AND a.revoked = false")
    void revokeAllByUser(@Param("user") UserEntity user);
}