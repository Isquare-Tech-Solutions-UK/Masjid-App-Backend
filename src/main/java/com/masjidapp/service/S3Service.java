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
}


