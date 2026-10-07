package com.interview.prep.service;

import com.interview.prep.dto.ShortenRequest;
import com.interview.prep.dto.ShortenResponse;
import com.interview.prep.dto.UrlStatsResponse;
import com.interview.prep.exception.ShortCodeExpiredException;
import com.interview.prep.exception.ShortCodeNotFoundException;
import com.interview.prep.model.ShortenedUrl;
import com.interview.prep.repository.ShortenedUrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class UrlShortenerService {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int CODE_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ShortenedUrlRepository repository;

    @Transactional
    public ShortenResponse shorten(ShortenRequest request, String baseUrl) {
        validateUrl(request.getUrl());

        String shortCode = generateUniqueCode();
        ShortenedUrl entity = ShortenedUrl.builder()
                .originalUrl(request.getUrl())
                .shortCode(shortCode)
                .expiresAt(request.getExpiresAt())
                .build();

        ShortenedUrl saved = repository.save(entity);
        return ShortenResponse.from(saved, baseUrl);
    }

    @Transactional
    public String resolve(String shortCode) {
        ShortenedUrl entity = repository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortCodeNotFoundException(shortCode));

        if (entity.getExpiresAt() != null && entity.getExpiresAt().isBefore(LocalDate.now())) {
            throw new ShortCodeExpiredException(shortCode);
        }

        repository.incrementVisitCount(shortCode);
        return entity.getOriginalUrl();
    }

    @Transactional(readOnly = true)
    public UrlStatsResponse getStats(String shortCode) {
        ShortenedUrl entity = repository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortCodeNotFoundException(shortCode));
        return UrlStatsResponse.from(entity);
    }

    private void validateUrl(String url) {
        try {
            URI uri = new URI(url);
            uri.toURL();
        } catch (URISyntaxException | MalformedURLException | IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid URL: " + url);
        }
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String code = generateCode();
            if (repository.findByShortCode(code).isEmpty()) {
                return code;
            }
        }
        throw new IllegalStateException("Failed to generate a unique short code after 10 attempts");
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
