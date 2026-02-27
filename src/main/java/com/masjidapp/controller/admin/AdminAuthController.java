package com.masjidapp.controller.admin;

import com.masjidapp.dto.request.ChangePasswordRequest;
import com.masjidapp.dto.request.LoginRequest;
import com.masjidapp.dto.response.AdminUserResponse;
import com.masjidapp.dto.response.ApiResponse;
import com.masjidapp.dto.response.LoginResponse;
import com.masjidapp.dto.response.MessageResponse;
import com.masjidapp.dto.response.RefreshTokenResponse;
import com.masjidapp.entity.AdminUser;
import com.masjidapp.repository.AdminUserRepository;
import com.masjidapp.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
@RequestMapping("/admin/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Authentication", description = "Endpoints for admin user authentication")
public class AdminAuthController {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final int REFRESH_TOKEN_COOKIE_MAX_AGE = 7 * 24 * 60 * 60; // 7 days in seconds

    private final AuthService authService;
    private final AdminUserRepository adminUserRepository;

    @Operation(summary = "Admin Login", description = "Authenticate an admin user with email and password. Returns a JWT access token in the response body and sets a refresh token as an HttpOnly cookie.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content(schema = @Schema(implementation = com.masjidapp.exception.GlobalExceptionHandler.ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(implementation = com.masjidapp.exception.GlobalExceptionHandler.ErrorResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        log.info("Login request received for email: {}", request.getEmail());

        LoginResponse loginResponse = authService.login(request);

        // Set refresh token as HttpOnly cookie
        setRefreshTokenCookie(response, loginResponse.getRefreshToken());

        return ResponseEntity.ok(ApiResponse.success(loginResponse));
    }

    /**
     * POST /admin/auth/refresh
     * Get new access token using refresh token cookie
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshToken(HttpServletRequest request) {
        String refreshToken = extractRefreshTokenFromCookie(request);

        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(null);
        }

        RefreshTokenResponse refreshResponse = authService.refreshToken(refreshToken);

        return ResponseEntity.ok(ApiResponse.success(refreshResponse));
    }

    /**
     * POST /admin/auth/logout
     * Invalidate refresh token and clear cookies
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<MessageResponse>> logout(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletResponse response) {

        if (userDetails != null) {
            String email = userDetails.getUsername();
            log.info("Processing logout for user: {}", email);
            adminUserRepository.findByEmail(email)
                    .ifPresent(u -> authService.logout(u.getId()));
        } else {
            log.info("Processing anonymous logout (clearing cookies)");
        }

        // Always clear refresh token cookie
        clearRefreshTokenCookie(response);

        return ResponseEntity.ok(ApiResponse.success(MessageResponse.of("Logged out successfully")));
    }

    /**
     * GET /admin/auth/me
     * Get details of currently authenticated admin user
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AdminUserResponse>> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {
        AdminUser adminUser = adminUserRepository.findByEmail(userDetails.getUsername())
                .orElseThrow();

        AdminUserResponse userResponse = authService.getCurrentUser(adminUser.getId());

        return ResponseEntity.ok(ApiResponse.success(userResponse));
    }

    /**
     * PUT /admin/auth/change-password
     * Change current user's password
     */
    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<MessageResponse>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletResponse response) {
        AdminUser adminUser = adminUserRepository.findByEmail(userDetails.getUsername())
                .orElseThrow();

        authService.changePassword(adminUser.getId(), request);

        // Clear refresh token cookie (user needs to login again)
        clearRefreshTokenCookie(response);

        return ResponseEntity.ok(ApiResponse.success(MessageResponse.of("Password changed successfully")));
    }

    /**
     * Set refresh token as HttpOnly secure cookie
     */
    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true); // Must be true for SameSite=None
        cookie.setPath("/"); // Must be "/" so frontend middleware can read it
        cookie.setMaxAge(REFRESH_TOKEN_COOKIE_MAX_AGE);
        cookie.setAttribute("SameSite", "None"); // Required for cross-site (Vercel -> EC2)
        response.addCookie(cookie);
    }

    /**
     * Clear refresh token cookie
     */
    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true); // Must be true for SameSite=None
        cookie.setPath("/"); // Must match the creation path
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", "None");
        response.addCookie(cookie);
    }

    /**
     * Extract refresh token from cookie
     */
    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        return Arrays.stream(cookies)
                .filter(cookie -> REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

}
