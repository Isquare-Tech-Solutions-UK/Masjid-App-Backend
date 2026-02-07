package com.masjidapp.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Standard API response wrapper matching OpenAPI specification.
 * All responses follow the format: { data: T, meta: Meta }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private T data;
    private Meta meta;

    /**
     * Create a successful response with data
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .data(data)
                .meta(Meta.now())
                .build();
    }

    /**
     * Create a successful response with data and custom request ID
     */
    public static <T> ApiResponse<T> success(T data, String requestId) {
        return ApiResponse.<T>builder()
                .data(data)
                .meta(Meta.builder()
                        .timestamp(Instant.now().toString())
                        .requestId(requestId)
                        .build())
                .build();
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

}
