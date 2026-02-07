package com.masjidapp.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    @JsonProperty("accessToken")
    private String accessToken;

    @JsonIgnore  // Don't expose in JSON response - sent via HttpOnly cookie
    private String refreshToken;

    @JsonProperty("tokenType")
    @Builder.Default
    private String tokenType = "Bearer";

    @JsonProperty("expiresIn")
    private long expiresIn;

    @JsonProperty("user")
    private AdminUserResponse user;

}
