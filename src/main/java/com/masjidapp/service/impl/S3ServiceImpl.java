
package com.masjidapp.service.impl;

import com.masjidapp.config.MinioConfig;
import com.masjidapp.service.S3Service;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * S3ServiceImpl
 *
 * Handles MinIO object storage operations related to event images.
 * (Class name retained for compatibility with existing callers.)
 *
 * Responsibilities:
 * - Upload multiple images
 * - Generate structured object keys
 * - Return public object URLs
 *
 * Key Structure:
 * events/{year}/{month}/{uuid}-{index}.{extension}
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class S3ServiceImpl implements S3Service {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucketName;

    // Public-facing URL, reachable from the browser/frontend, used only to build
    // the object URLs returned in API responses. The MinioClient bean (used for
    // the actual SDK upload/delete calls) is configured separately from
    // minio.endpoint, which may be a backend-only/Docker-internal address.
    @Value("${minio.public-url}")
    private String publicUrl;

    @Value("${minio.secure}")
    private boolean secure;

    // Legacy AWS S3 config, retained only to recognize and delete images uploaded
    // before the MinIO migration. Object keys are looked up in the current MinIO
    // bucket, since existing objects are expected to have been migrated there.
    @Value("${app.s3.legacy-bucket-name}")
    private String legacyBucketName;

    @Value("${app.s3.legacy-region}")
    private String legacyRegion;


    /**
     * Uploads event images to MinIO.
     *
     * @param files List of uploaded multipart files
     * @return List of public object URLs
     */
    @Override
    public List<String> uploadEventImages(List<MultipartFile> files) {

        List<String> uploadedUrls = new ArrayList<>();

        if (files == null || files.isEmpty()) {
            log.debug("MinIO Upload Skipped: No files provided.");
            return uploadedUrls;
        }

        log.info("MinIO Upload Initiated: totalFiles={}, bucket={}", files.size(), bucketName);

        String basePath = generateBasePath();

        for (int index = 0; index < files.size(); index++) {

            MultipartFile file = files.get(index);

            if (file == null || file.isEmpty()) {
                log.warn("MinIO Upload Skipped: Empty file at index={}", index);
                continue;
            }

            String key = buildObjectKey(basePath, file.getOriginalFilename(), index);

            try {
                uploadToMinio(key, file);
                String fileUrl = generatePublicUrl(key);

                uploadedUrls.add(fileUrl);

                log.info("MinIO Upload Success: key={}, size={} bytes",
                        key, file.getSize());

            } catch (Exception ex) {
                log.error("MinIO Upload Failed: key={}, error={}",
                        key, ex.getMessage(), ex);

                throw new RuntimeException("Failed to upload image to MinIO", ex);
            }
        }

        log.info("MinIO Upload Completed: successCount={}", uploadedUrls.size());

        return uploadedUrls;
    }

    /**
     * Deletes a list of event images from MinIO using their public URLs.
     * Extracts the object key from each URL by stripping either the current
     * MinIO URL prefix or the legacy AWS S3 URL prefix (for images uploaded
     * before the migration).
     *
     * Example MinIO URL: http://minio.internal:9000/masjid-app-media/events/2025/01/uuid-1.jpg
     * Example legacy URL: https://masjid-app-media.s3.ap-south-1.amazonaws.com/events/2025/01/uuid-1.jpg
     * Extracted key (both cases): events/2025/01/uuid-1.jpg
     */
    @Override
    public void deleteEventImages(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            log.debug("MinIO Delete Skipped: No image URLs provided.");
            return;
        }

        log.info("MinIO Delete Initiated: totalFiles={}, bucket={}", imageUrls.size(), bucketName);

        String minioUrlPrefix = generatePublicUrl("");
        String legacyUrlPrefix = String.format("https://%s.s3.%s.amazonaws.com/", legacyBucketName, legacyRegion);

        for (String url : imageUrls) {
            if (url == null || url.isBlank()) {
                log.warn("MinIO Delete Skipped: Blank URL encountered.");
                continue;
            }

            String key = extractObjectKey(url, minioUrlPrefix, legacyUrlPrefix);
            if (key == null) {
                log.warn("MinIO Delete Skipped: URL does not match expected bucket prefix. url={}", url);
                continue;
            }

            try {
                RemoveObjectArgs removeRequest = RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(key)
                        .build();

                minioClient.removeObject(removeRequest);
                log.info("MinIO Delete Success: key={}", key);

            } catch (Exception ex) {
                log.error("MinIO Delete Failed: key={}, error={}", key, ex.getMessage(), ex);
                throw new RuntimeException("Failed to delete image from MinIO: " + key, ex);
            }
        }

        log.info("MinIO Delete Completed: totalDeleted={}", imageUrls.size());
    }

    /**
     * Upload single file to MinIO.
     */
    private void uploadToMinio(String key, MultipartFile file) throws Exception {
        try (InputStream inputStream = file.getInputStream()) {
            PutObjectArgs putRequest = PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(key)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build();

            minioClient.putObject(putRequest);
        }
    }

    /**
     * Generate base folder path based on current year & month.
     */
    private String generateBasePath() {
        LocalDate today = LocalDate.now();
        String year = today.format(DateTimeFormatter.ofPattern("yyyy"));
        String month = today.format(DateTimeFormatter.ofPattern("MM"));
        return String.format("events/%s/%s", year, month);
    }

    /**
     * Build unique object key.
     */
    private String buildObjectKey(String basePath, String originalFilename, int index) {
        String extension = extractExtension(originalFilename);
        return String.format("%s/%s-%d%s",
                basePath,
                UUID.randomUUID(),
                index + 1,
                extension
        );
    }

    /**
     * Generate public URL for an uploaded object.
     */
    private String generatePublicUrl(String key) {
        return String.format(
                "%s/%s/%s",
                MinioConfig.resolveEndpointUrl(publicUrl, secure),
                bucketName,
                key
        );
    }

    /**
     * Extract the object key from a stored URL, matching either the current
     * MinIO URL prefix or the legacy AWS S3 URL prefix.
     */
    private String extractObjectKey(String url, String minioUrlPrefix, String legacyUrlPrefix) {
        if (url.startsWith(minioUrlPrefix)) {
            return url.substring(minioUrlPrefix.length());
        }
        if (url.startsWith(legacyUrlPrefix)) {
            return url.substring(legacyUrlPrefix.length());
        }
        return null;
    }

    /**
     * Extract file extension from filename.
     */
    private String extractExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex == -1 ? "" : filename.substring(lastDotIndex);
    }
}