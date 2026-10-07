package com.interview.prep.controller;

import com.interview.prep.dto.ShortenRequest;
import com.interview.prep.dto.ShortenResponse;
import com.interview.prep.dto.UrlStatsResponse;
import com.interview.prep.service.UrlShortenerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@Validated
public class UrlShortenerController {

    private final UrlShortenerService urlShortenerService;

    @PostMapping("/api/urls/shorten")
    public ResponseEntity<ShortenResponse> shorten(@Valid @RequestBody ShortenRequest request) {
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/s")
                .toUriString();
        ShortenResponse response = urlShortenerService.shorten(request, baseUrl);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/s/{shortCode}")
    public ResponseEntity<Void> redirect(
            @PathVariable @Pattern(regexp = "[A-Za-z0-9]{1,8}") String shortCode) {
        String originalUrl = urlShortenerService.resolve(shortCode);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(originalUrl));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @GetMapping("/api/urls/{shortCode}/stats")
    public ResponseEntity<UrlStatsResponse> getStats(
            @PathVariable @Pattern(regexp = "[A-Za-z0-9]{1,8}") String shortCode) {
        return ResponseEntity.ok(urlShortenerService.getStats(shortCode));
    }
}
