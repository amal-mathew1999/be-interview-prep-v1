package com.interview.prep.repository;

import com.interview.prep.model.ShortenedUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface ShortenedUrlRepository extends JpaRepository<ShortenedUrl, Long> {

    Optional<ShortenedUrl> findByShortCode(String shortCode);

    @Modifying
    @Query("UPDATE ShortenedUrl s SET s.visitCount = s.visitCount + 1 WHERE s.shortCode = :shortCode AND (s.expiresAt IS NULL OR s.expiresAt >= :today)")
    int incrementVisitCountIfNotExpired(@Param("shortCode") String shortCode, @Param("today") LocalDate today);
}
