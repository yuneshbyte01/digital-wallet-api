package com.yunesh.digitalwallet.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Slf4j
@Service
@Profile("dev")
@RequiredArgsConstructor
public class LocalStorageService implements StorageService {

    private final StorageProperties storageProperties;

    @Override
    public String uploadFile(MultipartFile file,
                             String folder,
                             String fileName) {
        try {
            Path uploadDir = Paths.get(
                    storageProperties.getLocalUploadDir(), folder);
            Files.createDirectories(uploadDir);

            Path filePath = uploadDir.resolve(fileName);
            Files.copy(file.getInputStream(), filePath,
                    StandardCopyOption.REPLACE_EXISTING);

            String relativePath = folder + "/" + fileName;
            log.info("File saved locally: {}", relativePath);
            return relativePath;

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to store file locally: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFile(String filePath) {
        try {
            Path path = Paths.get(
                    storageProperties.getLocalUploadDir(), filePath);
            Files.deleteIfExists(path);
            log.info("File deleted locally: {}", filePath);
        } catch (IOException e) {
            log.error("Failed to delete file: {}", e.getMessage());
        }
    }

    @Override
    public String getFileUrl(String filePath) {
        return "/uploads/" + filePath;
    }
}