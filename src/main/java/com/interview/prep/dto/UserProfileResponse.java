package com.interview.prep.dto;

import com.interview.prep.model.AppUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class UserProfileResponse {

    private final Long id;
    private final String username;
    private final String role;
    private final LocalDateTime createdDate;

    public static UserProfileResponse from(AppUser user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole().name())
                .createdDate(user.getCreatedDate())
                .build();
    }
}
