package com.yunesh.digitalwallet.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final StorageService storageService;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/jpg", "application/pdf"
    );

    public String uploadProfilePhoto(MultipartFile file, UUID userId) {
        validate(file);
        String fileName = userId + "_profile_" + UUID.randomUUID()
                + getExtension(file);
        return storageService.uploadFile(file, "profile-photos", fileName);
    }

    public String uploadDocumentPhoto(MultipartFile file, UUID userId) {
        validate(file);
        String fileName = userId + "_document_" + UUID.randomUUID()
                + getExtension(file);
        return storageService.uploadFile(file, "kyc-documents", fileName);
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "File size must not exceed 5MB");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException(
                    "File type not allowed. Allowed: JPEG, PNG, PDF");
        }
    }

    private String getExtension(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName != null && originalName.contains(".")) {
            return originalName.substring(
                    originalName.lastIndexOf("."));
        }
        return ".jpg";
    }
}