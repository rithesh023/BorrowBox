package com.borrowbox.dto;

import com.borrowbox.entity.AppUser;
import java.time.Instant;

public record UserResponse(Long id, String name, String email, String phone, String role, Instant createdAt, boolean enabled) {
    public static UserResponse from(AppUser user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getPhone(),
                user.getRole().name(), user.getCreatedAt(), user.isEnabled());
    }
}
