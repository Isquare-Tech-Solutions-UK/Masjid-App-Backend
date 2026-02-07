package com.masjidapp.service.impl;

import com.masjidapp.dto.request.ChangePasswordRequest;
import com.masjidapp.dto.request.LoginRequest;
import com.masjidapp.dto.response.AdminUserResponse;
import com.masjidapp.dto.response.LoginResponse;
import com.masjidapp.dto.response.RefreshTokenResponse;
import com.masjidapp.entity.AdminUser;
import com.masjidapp.entity.RefreshToken;
import com.masjidapp.exception.InvalidCredentialsException;
import com.masjidapp.exception.ResourceNotFoundException;
import com.masjidapp.exception.TokenExpiredException;
import com.masjidapp.repository.AdminUserRepository;
import com.masjidapp.repository.RefreshTokenRepository;
import com.masjidapp.security.JwtTokenProvider;
import com.masjidapp.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final AdminUserRepository adminUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        log.debug("Attempting login for email: {}", request.getEmail());

        // Find user by email
        AdminUser adminUser = adminUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        // Check if account is locked
        if (adminUser.isLocked()) {
            throw new InvalidCredentialsException("Account is locked. Please try again later.");
        }

        // Check if account is active
        if (!adminUser.isActive()) {
            throw new InvalidCredentialsException("Account is deactivated. Please contact administrator.");
        }

        try {
            // Authenticate using Spring Security
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for email: {}", request.getEmail());
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // Create UserDetails for token generation
        UserDetails userDetails = User.builder()
                .username(adminUser.getEmail())
                .password(adminUser.getPasswordHash())
                .authorities(Collections.emptyList())
                .build();

        // Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
        String refreshTokenString = jwtTokenProvider.generateRefreshToken(userDetails);

        // Save refresh token to database
        saveRefreshToken(adminUser, refreshTokenString);

        // Update last login timestamp
        adminUserRepository.updateLastLoginAt(adminUser.getId(), LocalDateTime.now());

        log.info("Successful login for user: {}", adminUser.getEmail());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenString)  // Include for cookie setting
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration() / 1000) // Convert to seconds
                .user(AdminUserResponse.fromEntity(adminUser))
                .build();
    }

    @Override
    @Transactional
    public RefreshTokenResponse refreshToken(String refreshTokenString) {
        log.debug("Attempting to refresh access token");

        // Find and validate refresh token
        RefreshToken refreshToken = refreshTokenRepository
                .findValidToken(refreshTokenString, LocalDateTime.now())
                .orElseThrow(() -> new TokenExpiredException("Invalid or expired refresh token"));

        AdminUser adminUser = refreshToken.getAdminUser();

        // Check if account is still active
        if (!adminUser.isActive()) {
            throw new InvalidCredentialsException("Account is deactivated");
        }

        // Create UserDetails for token generation
        UserDetails userDetails = User.builder()
                .username(adminUser.getEmail())
                .password(adminUser.getPasswordHash())
                .authorities(Collections.emptyList())
                .build();

        // Generate new access token
        String newAccessToken = jwtTokenProvider.generateAccessToken(userDetails);

        log.debug("Access token refreshed for user: {}", adminUser.getEmail());

        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration() / 1000)
                .build();
    }

    @Override
    @Transactional
    public void logout(UUID userId) {
        log.debug("Logging out user: {}", userId);

        // Revoke all refresh tokens for the user
        refreshTokenRepository.revokeAllTokensByUserId(userId, LocalDateTime.now());

        log.info("User logged out: {}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserResponse getCurrentUser(UUID userId) {
        AdminUser adminUser = adminUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return AdminUserResponse.fromEntity(adminUser);
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        log.debug("Changing password for user: {}", userId);

        AdminUser adminUser = adminUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), adminUser.getPasswordHash())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        // Update password
        adminUser.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        adminUserRepository.save(adminUser);

        // Revoke all existing refresh tokens (security measure)
        refreshTokenRepository.revokeAllTokensByUserId(userId, LocalDateTime.now());

        log.info("Password changed for user: {}", userId);
    }

    /**
     * Save refresh token to database
     */
    private void saveRefreshToken(AdminUser adminUser, String token) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .adminUser(adminUser)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtTokenProvider.getRefreshTokenExpiration() / 1000))
                .build();

        refreshTokenRepository.save(refreshToken);
    }

}
