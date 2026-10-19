package com.mining.soft.application.usecase;

import com.mining.soft.application.spi.StoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageUseCase {

    private final StoragePort storagePort;
    private final com.mining.soft.application.spi.IImageSanitizerPort imageSanitizer;

    public String uploadFile(String fileName, byte[] content, String contentType) {
        log.info("Uploading file: {}", fileName);
        return storagePort.upload(fileName, imageSanitizer.stripMetadata(content, contentType), contentType);
    }

    public byte[] downloadFile(String key) {
        log.info("Downloading file: {}", key);
        return storagePort.download(key);
    }

    public String getFileUrl(String key) {
        return storagePort.getUrl(key);
    }

    public void deleteFile(String key) {
        log.info("Deleting file: {}", key);
        storagePort.delete(key);
    }

    public byte[] resizeImageToMiniature(byte[] content) {
        log.info("Resizing image to miniature");
        return storagePort.resizeToMiniature(content);
    }

    public String uploadWithMiniature(String fileName, byte[] content, String contentType) {
        log.info("Uploading file with miniature: {}", fileName);
        content = imageSanitizer.stripMetadata(content, contentType);
        String mainKey = storagePort.upload(fileName, content, contentType);

        if (contentType != null && contentType.startsWith("image/")) {
            try {
                byte[] miniature = storagePort.resizeToMiniature(content);
                String miniatureFileName = "thumbnails/" + fileName;
                storagePort.upload(miniatureFileName, miniature, contentType);
            } catch (Exception e) {
                log.error("Error creating miniature for: {}", fileName, e);
            }
        }

        return mainKey;
    }
}

