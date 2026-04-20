package com.mining.soft.infrastructure.input;

import com.mining.soft.application.usecase.StorageUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
public class StorageController {

    private final StorageUseCase storageService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String key = storageService.uploadFile(
                file.getOriginalFilename(),
                file.getBytes(),
                file.getContentType()
            );

            Map<String, String> response = new HashMap<>();
            response.put("key", key);
            response.put("message", "File uploaded successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error uploading file", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error uploading file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/upload-with-thumbnail")
    public ResponseEntity<Map<String, String>> uploadFileWithThumbnail(@RequestParam("file") MultipartFile file) {
        try {
            String key = storageService.uploadWithMiniature(
                file.getOriginalFilename(),
                file.getBytes(),
                file.getContentType()
            );

            Map<String, String> response = new HashMap<>();
            response.put("key", key);
            response.put("message", "File uploaded successfully with thumbnail");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error uploading file with thumbnail", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error uploading file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/download/{key}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String key) {
        try {
            byte[] data = storageService.downloadFile(key);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", key);

            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error downloading file: {}", key, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping("/url/{key}")
    public ResponseEntity<Map<String, String>> getFileUrl(@PathVariable String key) {
        try {
            String url = storageService.getFileUrl(key);

            Map<String, String> response = new HashMap<>();
            response.put("url", url);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting file URL: {}", key, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error getting file URL: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Map<String, String>> deleteFile(@PathVariable String key) {
        try {
            storageService.deleteFile(key);

            Map<String, String> response = new HashMap<>();
            response.put("message", "File deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting file: {}", key, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error deleting file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}

