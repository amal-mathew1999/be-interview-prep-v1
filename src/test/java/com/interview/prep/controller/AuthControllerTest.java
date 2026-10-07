package com.interview.prep.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.prep.dto.LoginRequest;
import com.interview.prep.dto.RegisterRequest;
import com.interview.prep.model.AppUser;
import com.interview.prep.model.Role;
import com.interview.prep.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("POST /api/auth/register")
    class Register {

        @Test
        @DisplayName("should register a new user and return token")
        void shouldRegisterNewUser() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .username("testuser")
                    .password("password123")
                    .build();

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.username").value("testuser"))
                    .andExpect(jsonPath("$.role").value("USER"));

            assertThat(userRepository.existsByUsername("testuser")).isTrue();
        }

        @Test
        @DisplayName("should reject duplicate username")
        void shouldRejectDuplicateUsername() throws Exception {
            userRepository.save(AppUser.builder()
                    .username("existing")
                    .password(passwordEncoder.encode("password"))
                    .role(Role.USER)
                    .build());

            RegisterRequest request = RegisterRequest.builder()
                    .username("existing")
                    .password("password123")
                    .build();

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("should store password as bcrypt hash")
        void shouldStorePasswordHashed() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .username("hashtest")
                    .password("plaintext")
                    .build();

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            AppUser saved = userRepository.findByUsername("hashtest").orElseThrow();
            assertThat(saved.getPassword()).isNotEqualTo("plaintext");
            assertThat(passwordEncoder.matches("plaintext", saved.getPassword())).isTrue();
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("should login with valid credentials")
        void shouldLoginWithValidCredentials() throws Exception {
            userRepository.save(AppUser.builder()
                    .username("loginuser")
                    .password(passwordEncoder.encode("secret"))
                    .role(Role.USER)
                    .build());

            LoginRequest request = LoginRequest.builder()
                    .username("loginuser")
                    .password("secret")
                    .build();

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.username").value("loginuser"));
        }

        @Test
        @DisplayName("should reject invalid password")
        void shouldRejectInvalidPassword() throws Exception {
            userRepository.save(AppUser.builder()
                    .username("loginuser")
                    .password(passwordEncoder.encode("correct"))
                    .role(Role.USER)
                    .build());

            LoginRequest request = LoginRequest.builder()
                    .username("loginuser")
                    .password("wrong")
                    .build();

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Authorization")
    class Authorization {

        @Test
        @DisplayName("should return 401 JSON for unauthenticated request to protected endpoint")
        void shouldReturn401ForUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/me"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.errorType").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.message").value("Authentication required"));
        }

        @Test
        @DisplayName("should access own profile with valid token")
        void shouldAccessProfileWithToken() throws Exception {
            String token = registerAndGetToken("profileuser", "password123");

            mockMvc.perform(get("/api/me")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("profileuser"))
                    .andExpect(jsonPath("$.role").value("USER"));
        }

        @Test
        @DisplayName("should return 403 JSON when USER accesses admin endpoint")
        void shouldReturn403ForUserAccessingAdminEndpoint() throws Exception {
            String token = registerAndGetToken("regularuser", "password123");

            mockMvc.perform(get("/api/admin/users")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.errorType").value("FORBIDDEN"))
                    .andExpect(jsonPath("$.message").value("Access denied"));
        }

        @Test
        @DisplayName("should allow ADMIN to list all users")
        void shouldAllowAdminToListUsers() throws Exception {
            userRepository.save(AppUser.builder()
                    .username("adminuser")
                    .password(passwordEncoder.encode("adminpass"))
                    .role(Role.ADMIN)
                    .build());

            LoginRequest login = LoginRequest.builder()
                    .username("adminuser")
                    .password("adminpass")
                    .build();

            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(login)))
                    .andExpect(status().isOk())
                    .andReturn();

            String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                    .get("token").asText();

            mockMvc.perform(get("/api/admin/users")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].username").value("adminuser"));
        }

        @Test
        @DisplayName("should return 401 for unauthenticated admin endpoint access")
        void shouldReturn401ForUnauthenticatedAdminAccess() throws Exception {
            mockMvc.perform(get("/api/admin/users"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorType").value("UNAUTHORIZED"));
        }
    }

    private String registerAndGetToken(String username, String password) throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username(username)
                .password(password)
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }
}
