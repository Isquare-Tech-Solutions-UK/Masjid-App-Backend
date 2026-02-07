package com.masjidapp.service;

import com.masjidapp.dto.request.ChangePasswordRequest;
import com.masjidapp.dto.request.LoginRequest;
import com.masjidapp.dto.response.AdminUserResponse;
import com.masjidapp.dto.response.LoginResponse;
import com.masjidapp.dto.response.RefreshTokenResponse;

import java.util.UUID;

public interface AuthService {

    /**
     * Authenticate admin user and generate tokens
     */
    LoginResponse login(LoginRequest request);

    /**
     * Refresh access token using refresh token
     */
    RefreshTokenResponse refreshToken(String refreshToken);

    /**
     * Logout user and invalidate refresh tokens
     */
    void logout(UUID userId);

    /**
     * Get current authenticated user details
     */
    AdminUserResponse getCurrentUser(UUID userId);

    /**
     * Change user password
     */
    void changePassword(UUID userId, ChangePasswordRequest request);

}
