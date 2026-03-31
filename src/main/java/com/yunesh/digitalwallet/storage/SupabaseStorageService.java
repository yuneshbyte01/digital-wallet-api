package com.yunesh.digitalwallet.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Service
@Profile("prod")
@RequiredArgsConstructor
public class SupabaseStorageService implements StorageService {

    private final StorageProperties storageProperties;

    @Override
    public String uploadFile(MultipartFile file,
                             String folder,
                             String fileName) {
        try {
            String objectPath = folder + "/" + fileName;
            String uploadUrl = storageProperties.getSupabaseUrl()
                    + "/storage/v1/object/"
                    + storageProperties.getBucket()
                    + "/" + objectPath;

            RestClient restClient = RestClient.create();

            restClient.post()
                    .uri(uploadUrl)
                    .header("Authorization",
                            "Bearer " + storageProperties.getSupabaseKey())
                    .header("x-upsert", "true")
                    .contentType(MediaType.parseMediaType(
                            file.getContentType() != null
                                    ? file.getContentType()
                                    : "application/octet-stream"))
                    .body(file.getBytes())
                    .retrieve()
                    .toBodilessEntity();

            log.info("File uploaded to Supabase: {}", objectPath);
            return objectPath;

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to upload file to Supabase: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFile(String filePath) {
        String deleteUrl = storageProperties.getSupabaseUrl()
                + "/storage/v1/object/"
                + storageProperties.getBucket()
                + "/" + filePath;

        RestClient restClient = RestClient.create();

        restClient.delete()
                .uri(deleteUrl)
                .header("Authorization",
                        "Bearer " + storageProperties.getSupabaseKey())
                .retrieve()
                .toBodilessEntity();

        log.info("File deleted from Supabase: {}", filePath);
    }

    @Override
    public String getFileUrl(String filePath) {
        return storageProperties.getSupabaseUrl()
                + "/storage/v1/object/public/"
                + storageProperties.getBucket()
                + "/" + filePath;
    }
}