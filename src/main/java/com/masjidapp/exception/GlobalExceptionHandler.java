package com.masjidapp.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

        /**
         * Handle validation errors
         */
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
                Map<String, String> errors = new HashMap<>();
                ex.getBindingResult().getAllErrors().forEach(error -> {
                        String fieldName = ((FieldError) error).getField();
                        String errorMessage = error.getDefaultMessage();
                        errors.put(fieldName, errorMessage);
                });

                log.warn("Validation error: {}", errors);

                ErrorResponse response = ErrorResponse.builder()
                                .error(ErrorDetails.builder()
                                                .code("VALIDATION_ERROR")
                                                .message("Validation failed")
                                                .details(errors)
                                                .build())
                                .meta(Meta.now())
                                .build();

                return ResponseEntity.badRequest().body(response);
        }

        /**
         * Handle invalid credentials
         */
        @ExceptionHandler(InvalidCredentialsException.class)
        public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
                log.warn("Invalid credentials: {}", ex.getMessage());

                ErrorResponse response = ErrorResponse.builder()
                                .error(ErrorDetails.builder()
                                                .code("INVALID_CREDENTIALS")
                                                .message(ex.getMessage())
                                                .build())
                                .meta(Meta.now())
                                .build();

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        /**
         * Handle bad credentials from Spring Security
         */
        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
                log.warn("Bad credentials: {}", ex.getMessage());

                ErrorResponse response = ErrorResponse.builder()
                                .error(ErrorDetails.builder()
                                                .code("INVALID_CREDENTIALS")
                                                .message("Invalid email or password")
                                                .build())
                                .meta(Meta.now())
                                .build();

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        /**
         * Handle token expired
         */
        @ExceptionHandler(TokenExpiredException.class)
        public ResponseEntity<ErrorResponse> handleTokenExpired(TokenExpiredException ex) {
                log.warn("Token expired: {}", ex.getMessage());

                ErrorResponse response = ErrorResponse.builder()
                                .error(ErrorDetails.builder()
                                                .code("TOKEN_EXPIRED")
                                                .message(ex.getMessage())
                                                .build())
                                .meta(Meta.now())
                                .build();

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        /**
         * Handle resource not found
         */
        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
                log.warn("Resource not found: {}", ex.getMessage());

                ErrorResponse response = ErrorResponse.builder()
                                .error(ErrorDetails.builder()
                                                .code("NOT_FOUND")
                                                .message(ex.getMessage())
                                                .build())
                                .meta(Meta.now())
                                .build();

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        /**
         * Handle access denied
         */
        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
                log.warn("Access denied: {}", ex.getMessage());

                ErrorResponse response = ErrorResponse.builder()
                                .error(ErrorDetails.builder()
                                                .code("FORBIDDEN")
                                                .message("You don't have permission to access this resource")
                                                .build())
                                .meta(Meta.now())
                                .build();

                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        /**
         * Handle illegal argument / bad request errors
         */
        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
                log.warn("Bad request: {}", ex.getMessage());

                ErrorResponse response = ErrorResponse.builder()
                                .error(ErrorDetails.builder()
                                                .code("BAD_REQUEST")
                                                .message(ex.getMessage())
                                                .build())
                                .meta(Meta.now())
                                .build();

                return ResponseEntity.badRequest().body(response);
        }

        /**
         * Handle all other exceptions
         */
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
                log.error("Unexpected error: {}", ex.getMessage(), ex);

                ErrorResponse response = ErrorResponse.builder()
                                .error(ErrorDetails.builder()
                                                .code("INTERNAL_ERROR")
                                                .message("An unexpected error occurred")
                                                .build())
                                .meta(Meta.now())
                                .build();

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        // ============================================
        // Response DTOs
        // ============================================

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class ErrorResponse {
                private ErrorDetails error;
                private Meta meta;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class ErrorDetails {
                private String code;
                private String message;
                private Map<String, String> details;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Meta {
                private String timestamp;
                private String requestId;

                public static Meta now() {
                        return Meta.builder()
                                        .timestamp(Instant.now().toString())
                                        .requestId("req_" + UUID.randomUUID().toString().substring(0, 8))
                                        .build();
                }
        }

        /**
         * Handle request not process exception
         */
        @ExceptionHandler(MARequestException.class)
        public ResponseEntity<ErrorResponse> handleMARequest(MARequestException ex) {
                log.warn("Request unable to processed : {}", ex.getMessage());

                ErrorResponse response = ErrorResponse.builder()
                                .error(ErrorDetails.builder()
                                                .code("BAD_REQUEST")
                                                .message(ex.getMessage())
                                                .build())
                                .meta(Meta.now())
                                .build();

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

}
