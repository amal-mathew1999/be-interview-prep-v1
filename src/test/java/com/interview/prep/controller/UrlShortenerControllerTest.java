package com.interview.prep.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.prep.dto.ShortenRequest;
import com.interview.prep.model.ShortenedUrl;
import com.interview.prep.repository.ShortenedUrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UrlShortenerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ShortenedUrlRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Nested
    @DisplayName("POST /api/urls/shorten")
    class Shorten {

        @Test
        @DisplayName("should shorten a valid URL and return 201")
        void shouldShortenValidUrl() throws Exception {
            ShortenRequest request = ShortenRequest.builder()
                    .url("https://www.example.com/some/long/path")
                    .build();

            mockMvc.perform(post("/api/urls/shorten")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.shortCode").isNotEmpty())
                    .andExpect(jsonPath("$.shortCode").isString())
                    .andExpect(jsonPath("$.shortUrl").isNotEmpty())
                    .andExpect(jsonPath("$.originalUrl").value("https://www.example.com/some/long/path"));
        }

        @Test
        @DisplayName("should shorten URL with expiry date")
        void shouldShortenWithExpiry() throws Exception {
            LocalDate expiry = LocalDate.now().plusDays(30);
            ShortenRequest request = ShortenRequest.builder()
                    .url("https://www.example.com")
                    .expiresAt(expiry)
                    .build();

            mockMvc.perform(post("/api/urls/shorten")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.expiresAt").value(expiry.toString()));
        }

        @Test
        @DisplayName("should generate short code of at most 8 characters")
        void shouldGenerateCodeMaxLength8() throws Exception {
            ShortenRequest request = ShortenRequest.builder()
                    .url("https://www.example.com")
                    .build();

            MvcResult result = mockMvc.perform(post("/api/urls/shorten")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn();

            String shortCode = objectMapper.readTree(result.getResponse().getContentAsString())
                    .get("shortCode").asText();
            assertThat(shortCode).hasSizeLessThanOrEqualTo(8);
            assertThat(shortCode).matches("[A-Za-z0-9]+");
        }

        @Test
        @DisplayName("should create different codes for same URL")
        void shouldCreateDifferentCodesForSameUrl() throws Exception {
            ShortenRequest request = ShortenRequest.builder()
                    .url("https://www.example.com/duplicate")
                    .build();
            String body = objectMapper.writeValueAsString(request);

            MvcResult result1 = mockMvc.perform(post("/api/urls/shorten")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andReturn();

            MvcResult result2 = mockMvc.perform(post("/api/urls/shorten")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andReturn();

            String code1 = objectMapper.readTree(result1.getResponse().getContentAsString())
                    .get("shortCode").asText();
            String code2 = objectMapper.readTree(result2.getResponse().getContentAsString())
                    .get("shortCode").asText();
            assertThat(code1).isNotEqualTo(code2);
        }

        @Test
        @DisplayName("should reject blank URL")
        void shouldRejectBlankUrl() throws Exception {
            ShortenRequest request = ShortenRequest.builder()
                    .url("")
                    .build();

            mockMvc.perform(post("/api/urls/shorten")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should reject invalid URL")
        void shouldRejectInvalidUrl() throws Exception {
            ShortenRequest request = ShortenRequest.builder()
                    .url("not-a-url")
                    .build();

            mockMvc.perform(post("/api/urls/shorten")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorType").value("INVALID_INPUT"));
        }

        @Test
        @DisplayName("should reject past expiry date")
        void shouldRejectPastExpiry() throws Exception {
            ShortenRequest request = ShortenRequest.builder()
                    .url("https://www.example.com")
                    .expiresAt(LocalDate.now().minusDays(1))
                    .build();

            mockMvc.perform(post("/api/urls/shorten")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /s/{shortCode}")
    class Redirect {

        @Test
        @DisplayName("should redirect to original URL")
        void shouldRedirect() throws Exception {
            ShortenedUrl entity = repository.save(ShortenedUrl.builder()
                    .originalUrl("https://www.example.com/target")
                    .shortCode("abc12345")
                    .build());

            mockMvc.perform(get("/s/" + entity.getShortCode()))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location", "https://www.example.com/target"));
        }

        @Test
        @DisplayName("should increment visit count on redirect")
        void shouldIncrementVisitCount() throws Exception {
            ShortenedUrl entity = repository.save(ShortenedUrl.builder()
                    .originalUrl("https://www.example.com")
                    .shortCode("count123")
                    .build());

            mockMvc.perform(get("/s/" + entity.getShortCode()))
                    .andExpect(status().isFound());
            mockMvc.perform(get("/s/" + entity.getShortCode()))
                    .andExpect(status().isFound());
            mockMvc.perform(get("/s/" + entity.getShortCode()))
                    .andExpect(status().isFound());

            Optional<ShortenedUrl> updated = repository.findByShortCode(entity.getShortCode());
            assertThat(updated).isPresent();
            assertThat(updated.get().getVisitCount()).isEqualTo(3L);
        }

        @Test
        @DisplayName("should return 404 for unknown short code")
        void shouldReturn404ForUnknownCode() throws Exception {
            mockMvc.perform(get("/s/unknown1"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorType").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("should return 410 for expired short code")
        void shouldReturn410ForExpiredCode() throws Exception {
            repository.save(ShortenedUrl.builder()
                    .originalUrl("https://www.example.com")
                    .shortCode("expir123")
                    .expiresAt(LocalDate.now().minusDays(1))
                    .build());

            mockMvc.perform(get("/s/expir123"))
                    .andExpect(status().isGone())
                    .andExpect(jsonPath("$.errorType").value("EXPIRED"))
                    .andExpect(jsonPath("$.message").value("Short code has expired: expir123"));
        }
    }

    @Nested
    @DisplayName("GET /api/urls/{shortCode}/stats")
    class Stats {

        @Test
        @DisplayName("should return stats for a short code")
        void shouldReturnStats() throws Exception {
            ShortenedUrl entity = repository.save(ShortenedUrl.builder()
                    .originalUrl("https://www.example.com/stats")
                    .shortCode("stats123")
                    .build());

            mockMvc.perform(get("/api/urls/" + entity.getShortCode() + "/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.shortCode").value("stats123"))
                    .andExpect(jsonPath("$.originalUrl").value("https://www.example.com/stats"))
                    .andExpect(jsonPath("$.visitCount").value(0))
                    .andExpect(jsonPath("$.createdDate").isNotEmpty());
        }

        @Test
        @DisplayName("should return 404 for unknown code stats")
        void shouldReturn404ForUnknownStats() throws Exception {
            mockMvc.perform(get("/api/urls/noexist1/stats"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should reflect visit count after redirects")
        void shouldReflectVisitCountAfterRedirects() throws Exception {
            repository.save(ShortenedUrl.builder()
                    .originalUrl("https://www.example.com")
                    .shortCode("visit123")
                    .build());

            mockMvc.perform(get("/s/visit123")).andExpect(status().isFound());
            mockMvc.perform(get("/s/visit123")).andExpect(status().isFound());

            mockMvc.perform(get("/api/urls/visit123/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.visitCount").value(2));
        }
    }
}
