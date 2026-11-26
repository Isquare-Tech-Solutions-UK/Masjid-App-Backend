package com.masjidapp.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Ensures the configured MinIO bucket exists when the application starts.
 * <p>
 * A newly created bucket is given anonymous read access to its objects only
 * (s3:GetObject), because event image URLs are loaded directly by browsers and
 * the mobile app. Listing the bucket is deliberately not granted. An existing
 * bucket is left exactly as it is.
 * <p>
 * Failure is logged, not fatal: MinIO may still be starting (compose only waits
 * for the container to start), and the rest of the API does not need storage.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MinioBucketInitializer implements ApplicationRunner {

    private static final String PUBLIC_READ_POLICY = """
            {
              "Version": "2012-10-17",
              "Statement": [
                {
                  "Effect": "Allow",
                  "Principal": {"AWS": ["*"]},
                  "Action": ["s3:GetObject"],
                  "Resource": ["arn:aws:s3:::%s/*"]
                }
              ]
            }
            """;

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucketName;

    @Override
    public void run(ApplicationArguments args) {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (exists) {
                log.info("MinIO bucket present: bucket={}", bucketName);
                return;
            }

            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                    .bucket(bucketName)
                    .config(PUBLIC_READ_POLICY.formatted(bucketName))
                    .build());
            log.info("MinIO bucket created with public-read objects: bucket={}", bucketName);

        } catch (Exception ex) {
            log.error("MinIO bucket check/creation failed; image uploads will fail until it exists. bucket={}, error={}",
                    bucketName, ex.getMessage(), ex);
        }
    }
}
