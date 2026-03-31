package com.yunesh.digitalwallet.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    String uploadFile(MultipartFile file, String folder, String fileName);

    void deleteFile(String filePath);

    String getFileUrl(String filePath);
}