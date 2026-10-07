package com.interview.prep.controller;

import com.interview.prep.dto.UserProfileResponse;
import com.interview.prep.service.UserService;
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

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getProfile(@AuthenticationPrincipal String username) {
        return ResponseEntity.ok(userService.getProfile(username));
    }

    @GetMapping("/admin/users")
    public ResponseEntity<List<UserProfileResponse>> listAllUsers() {
        return ResponseEntity.ok(userService.listAllUsers());
    }
}
