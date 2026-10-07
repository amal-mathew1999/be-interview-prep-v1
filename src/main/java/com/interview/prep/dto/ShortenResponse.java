package com.interview.prep.dto;

import com.interview.prep.model.ShortenedUrl;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShortenResponse {

    private String shortCode;
    private String shortUrl;
    private String originalUrl;
    private LocalDate expiresAt;

    /**
     * Creates a ShortenResponse from a ShortenedUrl entity and base URL.
     */
    public static ShortenResponse from(ShortenedUrl entity, String baseUrl) {
        return ShortenResponse.builder()
                .shortCode(entity.getShortCode())
                .shortUrl(baseUrl + "/" + entity.getShortCode())
                .originalUrl(entity.getOriginalUrl())
                .expiresAt(entity.getExpiresAt())
                .build();
    }
}
