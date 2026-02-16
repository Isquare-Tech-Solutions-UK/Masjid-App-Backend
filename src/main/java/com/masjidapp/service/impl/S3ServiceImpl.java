package com.masjidapp.service.impl;

import com.masjidapp.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3ServiceImpl implements S3Service {

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.credentials.access-key}")
    private String accessKeyId;

    @Value("${aws.credentials.secret-key}")
    private String secretAccessKey;

    /**
     * Upload event images to S3.
     * The S3 key format is: events/{year}/{month}/{uuid}-{index}.{ext}
     */
    @Override
    public List<String> uploadEventImages(List<MultipartFile> files) {
        List<String> urls = new ArrayList<>();

        if (files == null || files.isEmpty()) {
            log.debug("No event images provided for upload");
            return urls;
        }

        S3Client s3Client = buildClient();

        LocalDate today = LocalDate.now();
        String year = today.format(DateTimeFormatter.ofPattern("yyyy"));
        String month = today.format(DateTimeFormatter.ofPattern("MM"));

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            if (file.isEmpty()) {
                log.warn("Skipping empty image file at index {}", i);
                continue;
            }

            String originalFilename = file.getOriginalFilename();
            String extension = extractExtension(originalFilename);
            String key = String.format("events/%s/%s/%s-%d%s",
                    year,
                    month,
                    UUID.randomUUID(),
                    i + 1,
                    extension);

            try {
                log.info("Uploading event image to S3. key={}, size={} bytes", key, file.getSize());

                PutObjectRequest putRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType(file.getContentType())
                        .build();

                s3Client.putObject(putRequest, RequestBody.fromBytes(file.getBytes()));

                String url = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);
                urls.add(url);

                log.debug("Successfully uploaded event image. key={}, url={}", key, url);
            } catch (IOException e) {
                log.error("Failed to upload event image to S3 for key {}", key, e);
                throw new RuntimeException("Failed to upload image to S3", e);
            }
        }

        return urls;
    }

    private S3Client buildClient() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }

    private String extractExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1) {
            return "";
        }
        return filename.substring(dotIndex);
    }
}


