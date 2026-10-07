package com.interview.prep.service;

import com.interview.prep.dto.UserProfileResponse;
import com.interview.prep.model.AppUser;
import com.interview.prep.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final AppUserRepository userRepository;

    public UserProfileResponse getProfile(String username) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
        return UserProfileResponse.from(user);
    }

    public List<UserProfileResponse> listAllUsers() {
        return userRepository.findAll().stream()
                .map(UserProfileResponse::from)
                .toList();
    }
}
