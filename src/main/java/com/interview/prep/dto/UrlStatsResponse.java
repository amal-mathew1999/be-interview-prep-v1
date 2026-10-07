package com.interview.prep.dto;

import com.interview.prep.model.ShortenedUrl;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UrlStatsResponse {

    private String shortCode;
    private String originalUrl;
    private long visitCount;
    private LocalDateTime createdDate;

    public static UrlStatsResponse from(ShortenedUrl entity) {
        return UrlStatsResponse.builder()
                .shortCode(entity.getShortCode())
                .originalUrl(entity.getOriginalUrl())
                .visitCount(entity.getVisitCount())
                .createdDate(entity.getCreatedDate())
                .build();
    }
}
