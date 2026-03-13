package com.masjidapp.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface S3Service {

    /**
     * Upload a list of event images to S3 and return their public URLs.
     *
     * @param files list of image files (0..n)
     * @return list of public URLs corresponding to uploaded images
     */
    List<String> uploadEventImages(List<MultipartFile> files);

    /**
     * Delete a list of event images from S3 by their public URLs.
     * Extracts the S3 object key from each URL and deletes it.
     *
     * @param imageUrls list of public S3 URLs to delete
     */
    void deleteEventImages(List<String> imageUrls);
}
