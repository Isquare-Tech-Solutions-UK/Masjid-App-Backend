package com.masjidapp.service.impl;

import com.masjidapp.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * S3ServiceImpl
 *
 * Handles Amazon S3 operations related to event images.
 *
 * Responsibilities:
 * - Upload multiple images
 * - Generate structured S3 object keys
 * - Return public object URLs
 *
 * Key Structure:
 * events/{year}/{month}/{uuid}-{index}.{extension}
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class S3ServiceImpl implements S3Service {

    private final S3Client s3Client;

    @Value("${app.s3.bucket-name}")
    private String bucketName;

    @Value("${spring.cloud.aws.region.static}")
    private String region;


    /**
     * Uploads event images to S3.
     *
     * @param files List of uploaded multipart files
     * @return List of public S3 URLs
     */
    @Override
    public List<String> uploadEventImages(List<MultipartFile> files) {

        List<String> uploadedUrls = new ArrayList<>();

        if (files == null || files.isEmpty()) {
            log.debug("S3 Upload Skipped: No files provided.");
            return uploadedUrls;
        }

        log.info("S3 Upload Initiated: totalFiles={}, bucket={}", files.size(), bucketName);

        String basePath = generateBasePath();

        for (int index = 0; index < files.size(); index++) {

            MultipartFile file = files.get(index);

            if (file == null || file.isEmpty()) {
                log.warn("S3 Upload Skipped: Empty file at index={}", index);
                continue;
            }

            String key = buildObjectKey(basePath, file.getOriginalFilename(), index);

            try {
                uploadToS3(key, file);
                String fileUrl = generatePublicUrl(key);

                uploadedUrls.add(fileUrl);

                log.info("S3 Upload Success: key={}, size={} bytes",
                        key, file.getSize());

            } catch (IOException ex) {
                log.error("S3 Upload Failed: key={}, error={}",
                        key, ex.getMessage(), ex);

                throw new RuntimeException("Failed to upload image to S3", ex);
            }
        }

        log.info("S3 Upload Completed: successCount={}", uploadedUrls.size());

        return uploadedUrls;
    }

    /**
     * Upload single file to S3.
     */
    private void uploadToS3(String key, MultipartFile file) throws IOException {

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(
                putRequest,
                RequestBody.fromBytes(file.getBytes())
        );
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
     * Build unique S3 object key.
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
     * Generate public URL for uploaded object.
     */
    private String generatePublicUrl(String key) {
        return String.format(
                "https://%s.s3.%s.amazonaws.com/%s",
                bucketName,
                region,
                key
        );
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
