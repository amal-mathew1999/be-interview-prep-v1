package com.interview.prep.service;

import com.interview.prep.dto.ShortenRequest;
import com.interview.prep.dto.ShortenResponse;
import com.interview.prep.dto.UrlStatsResponse;
import com.interview.prep.exception.ShortCodeExpiredException;
import com.interview.prep.exception.ShortCodeNotFoundException;
import com.interview.prep.model.ShortenedUrl;
import com.interview.prep.repository.ShortenedUrlRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UrlShortenerServiceTest {

    @Mock
    private ShortenedUrlRepository repository;

    @InjectMocks
    private UrlShortenerService service;

    private static final String BASE_URL = "http://localhost:8080/s";

    @Nested
    @DisplayName("shorten")
    class Shorten {

        @Test
        @DisplayName("should create a shortened URL with valid input")
        void shouldCreateShortenedUrl() {
            ShortenRequest request = ShortenRequest.builder()
                    .url("https://www.example.com/long/path")
                    .build();

            when(repository.findByShortCode(anyString())).thenReturn(Optional.empty());
            when(repository.save(any(ShortenedUrl.class))).thenAnswer(invocation -> {
                ShortenedUrl entity = invocation.getArgument(0);
                entity.setId(1L);
                return entity;
            });

            ShortenResponse response = service.shorten(request, BASE_URL);

            assertThat(response.getOriginalUrl()).isEqualTo("https://www.example.com/long/path");
            assertThat(response.getShortCode()).hasSize(8);
            assertThat(response.getShortCode()).matches("[A-Za-z0-9]+");
            assertThat(response.getShortUrl()).startsWith(BASE_URL + "/");

            ArgumentCaptor<ShortenedUrl> captor = ArgumentCaptor.forClass(ShortenedUrl.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getOriginalUrl()).isEqualTo("https://www.example.com/long/path");
        }

        @Test
        @DisplayName("should save expiry date when provided")
        void shouldSaveExpiryDate() {
            LocalDate expiry = LocalDate.now().plusDays(7);
            ShortenRequest request = ShortenRequest.builder()
                    .url("https://www.example.com")
                    .expiresAt(expiry)
                    .build();

            when(repository.findByShortCode(anyString())).thenReturn(Optional.empty());
            when(repository.save(any(ShortenedUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ShortenResponse response = service.shorten(request, BASE_URL);

            assertThat(response.getExpiresAt()).isEqualTo(expiry);
        }

        @Test
        @DisplayName("should reject invalid URL")
        void shouldRejectInvalidUrl() {
            ShortenRequest request = ShortenRequest.builder()
                    .url("not-a-valid-url")
                    .build();

            assertThatThrownBy(() -> service.shorten(request, BASE_URL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid URL");
        }

        @Test
        @DisplayName("should reject URL without protocol")
        void shouldRejectUrlWithoutProtocol() {
            ShortenRequest request = ShortenRequest.builder()
                    .url("www.example.com")
                    .build();

            assertThatThrownBy(() -> service.shorten(request, BASE_URL))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("resolve")
    class Resolve {

        @Test
        @DisplayName("should return original URL and increment visit count")
        void shouldResolveAndIncrement() {
            ShortenedUrl entity = ShortenedUrl.builder()
                    .shortCode("abc12345")
                    .originalUrl("https://www.example.com")
                    .visitCount(5L)
                    .build();

            when(repository.findByShortCode("abc12345")).thenReturn(Optional.of(entity));
            when(repository.incrementVisitCount("abc12345")).thenReturn(1);

            String result = service.resolve("abc12345");

            assertThat(result).isEqualTo("https://www.example.com");
            verify(repository).incrementVisitCount("abc12345");
        }

        @Test
        @DisplayName("should throw when short code not found")
        void shouldThrowWhenNotFound() {
            when(repository.findByShortCode("unknown1")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.resolve("unknown1"))
                    .isInstanceOf(ShortCodeNotFoundException.class)
                    .hasMessageContaining("unknown1");
        }

        @Test
        @DisplayName("should throw when short code is expired")
        void shouldThrowWhenExpired() {
            ShortenedUrl entity = ShortenedUrl.builder()
                    .shortCode("expir123")
                    .originalUrl("https://www.example.com")
                    .expiresAt(LocalDate.now().minusDays(1))
                    .build();

            when(repository.findByShortCode("expir123")).thenReturn(Optional.of(entity));

            assertThatThrownBy(() -> service.resolve("expir123"))
                    .isInstanceOf(ShortCodeExpiredException.class)
                    .hasMessageContaining("expir123");

            verify(repository, never()).incrementVisitCount(anyString());
        }

        @Test
        @DisplayName("should allow non-expired code")
        void shouldAllowNonExpiredCode() {
            ShortenedUrl entity = ShortenedUrl.builder()
                    .shortCode("future12")
                    .originalUrl("https://www.example.com")
                    .expiresAt(LocalDate.now().plusDays(1))
                    .build();

            when(repository.findByShortCode("future12")).thenReturn(Optional.of(entity));
            when(repository.incrementVisitCount("future12")).thenReturn(1);

            String result = service.resolve("future12");
            assertThat(result).isEqualTo("https://www.example.com");
        }
    }

    @Nested
    @DisplayName("getStats")
    class GetStats {

        @Test
        @DisplayName("should return stats for existing code")
        void shouldReturnStats() {
            ShortenedUrl entity = ShortenedUrl.builder()
                    .shortCode("stats123")
                    .originalUrl("https://www.example.com")
                    .visitCount(42L)
                    .createdDate(LocalDateTime.of(2026, 10, 1, 12, 0))
                    .build();

            when(repository.findByShortCode("stats123")).thenReturn(Optional.of(entity));

            UrlStatsResponse stats = service.getStats("stats123");

            assertThat(stats.getShortCode()).isEqualTo("stats123");
            assertThat(stats.getOriginalUrl()).isEqualTo("https://www.example.com");
            assertThat(stats.getVisitCount()).isEqualTo(42L);
            assertThat(stats.getCreatedDate()).isEqualTo(LocalDateTime.of(2026, 10, 1, 12, 0));
        }

        @Test
        @DisplayName("should throw when code not found")
        void shouldThrowWhenNotFound() {
            when(repository.findByShortCode("noexist1")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getStats("noexist1"))
                    .isInstanceOf(ShortCodeNotFoundException.class);
        }
    }
}
