package com.mining.soft.infrastructure.out.jpa.adapter.s3;


import com.mining.soft.application.spi.StoragePort;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Slf4j
@Service
public class S3StorageAdapter implements StoragePort {

    private final S3Client s3;
    private final String bucket;
    private static final int MINIATURE_SIZE = 96;

    public S3StorageAdapter(
        S3Client s3,
        @Value("${aws.s3.bucketName}") String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
    }

    @Override
    public String upload(String key, byte[] content, String contentType) {
        String uniqueKey = generateUniqueKey(key);
        PutObjectRequest put = PutObjectRequest.builder()
            .bucket(bucket)
            .key(uniqueKey)
            .contentType(contentType)
            .build();

        s3.putObject(put, RequestBody.fromBytes(content));
        log.info("File uploaded to S3: {}", uniqueKey);
        return uniqueKey;
    }

    private String generateUniqueKey(String originalKey) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        int dotIndex = originalKey.lastIndexOf('.');
        if (dotIndex > 0) {
            String name = originalKey.substring(0, dotIndex);
            String extension = originalKey.substring(dotIndex);
            return name + "_" + timestamp + extension;
        }
        return originalKey + "_" + timestamp;
    }

    @Override
    public byte[] download(String key) {
        try {
            ResponseBytes<GetObjectResponse> resp = s3.getObjectAsBytes(
                GetObjectRequest.builder().bucket(bucket).key(key).build()
            );
            log.info("File downloaded from S3: {}", key);
            return resp.asByteArray();
        } catch (NoSuchKeyException e) {
            log.error("File not found in S3: {}", key);
            throw new S3NotFoundException(
                BusinessErrorCodes.S3_NOT_FOUND.message(),
                BusinessErrorCodes.S3_NOT_FOUND.code());
        }
    }

    @Override
    public String getUrl(String key) {
        return String.format("https://%s.s3.amazonaws.com/%s", bucket, key);
    }

    @Override
    public void delete(String key) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        log.info("File deleted from S3: {}", key);
    }

    @Override
    public byte[] resizeToMiniature(byte[] content) {
        try (ByteArrayInputStream in = new ByteArrayInputStream(content);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Thumbnails.of(in)
                    .size(MINIATURE_SIZE, MINIATURE_SIZE)
                    .outputFormat("png")
                    .toOutputStream(out);

            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error generating miniature", e);
            throw new RuntimeException("Error generating miniature", e);
        }
    }
}

