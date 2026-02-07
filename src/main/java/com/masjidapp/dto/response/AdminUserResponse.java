package com.masjidapp.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.masjidapp.entity.AdminRole;
import com.masjidapp.entity.AdminUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("username")
    private String username;

    @JsonProperty("email")
    private String email;

    @JsonProperty("fullName")
    private String fullName;

    @JsonProperty("role")
    private AdminRole role;

    @JsonProperty("lastLoginAt")
    private LocalDateTime lastLoginAt;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    /**
     * Convert AdminUser entity to AdminUserResponse DTO
     */
    public static AdminUserResponse fromEntity(AdminUser adminUser) {
        return AdminUserResponse.builder()
                .id(adminUser.getId())
                .username(adminUser.getUsername())
                .email(adminUser.getEmail())
                .fullName(adminUser.getFullName())
                .role(adminUser.getRole())
                .lastLoginAt(adminUser.getLastLoginAt())
                .createdAt(adminUser.getCreatedAt())
                .build();
    }

}
