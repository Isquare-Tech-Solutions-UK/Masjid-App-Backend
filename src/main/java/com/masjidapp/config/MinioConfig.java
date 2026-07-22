package com.masjidapp.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.secure}")
    private boolean secure;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(resolveEndpointUrl(endpoint, secure))
                .credentials(accessKey, secretKey)
                .build();
    }

    /**
     * Accepts either a bare host[:port] (scheme derived from minio.secure) or a
     * full URL that already includes http:// or https://.
     */
    public static String resolveEndpointUrl(String endpoint, boolean secure) {
        if (endpoint.startsWith("http://") || endpoint.startsWith("https://")) {
            return endpoint;
        }
        return String.format("%s://%s", secure ? "https" : "http", endpoint);
    }
}