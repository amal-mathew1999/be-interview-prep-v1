package com.interview.prep.controller;

import com.interview.prep.dto.UserProfileResponse;
import com.interview.prep.model.AppUser;
import com.interview.prep.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final AppUserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getProfile(@AuthenticationPrincipal String username) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
        return ResponseEntity.ok(UserProfileResponse.from(user));
    }

    @GetMapping("/admin/users")
    public ResponseEntity<List<UserProfileResponse>> listAllUsers() {
        List<UserProfileResponse> users = userRepository.findAll().stream()
                .map(UserProfileResponse::from)
                .toList();
        return ResponseEntity.ok(users);
    }
}
