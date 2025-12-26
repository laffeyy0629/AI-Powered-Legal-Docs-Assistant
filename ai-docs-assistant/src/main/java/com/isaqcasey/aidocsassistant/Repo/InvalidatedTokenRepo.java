package com.isaqcasey.aidocsassistant.Repo;

import com.isaqcasey.aidocsassistant.Model.InvalidatedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface InvalidatedTokenRepo extends JpaRepository<InvalidatedToken, Long> {

    boolean existsByToken(String token);

    @Modifying
    @Query("DELETE FROM InvalidatedToken it WHERE it.expiresAt < :now")
    void deleteExpiredTokens(LocalDateTime now);
}

