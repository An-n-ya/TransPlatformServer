package com.app.upload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.InputStream;
import java.util.UUID;

/**
 * Generic S3-compatible object storage implementation. Covers AWS S3,
 * Cloudflare R2, MinIO, Aliyun OSS (S3-compatible mode) and Tencent COS
 * via a single AWS SDK v2 dependency. Selected when storage.provider=s3.
 */
@Service
@ConditionalOnProperty(name = "storage.provider", havingValue = "s3")
@RequiredArgsConstructor
@Slf4j
public class S3StorageService implements StorageService {

    private final S3Client s3Client;
    private final StorageProperties properties;

    @Override
    public UploadResult upload(UploadRequest request) {
        String key = buildKey(request);
        try (InputStream is = request.inputStream()) {
            PutObjectRequest putReq = PutObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .contentType(request.contentType())
                    .build();
            s3Client.putObject(putReq, RequestBody.fromInputStream(is, request.size()));
        } catch (S3Exception e) {
            log.error("S3 upload failed for key: {}", key, e);
            throw new RuntimeException("文件上传失败", e);
        } catch (Exception e) {
            log.error("S3 upload IO error for key: {}", key, e);
            throw new RuntimeException("文件上传失败", e);
        }
        String url = buildUrl(key);
        log.debug("S3 upload ok, key={}, url={}", key, url);
        return new UploadResult(url, key, getProvider(), request.size());
    }

    @Override
    public void delete(String storageKey) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(storageKey)
                    .build());
        } catch (S3Exception e) {
            log.error("S3 delete failed for key: {}", storageKey, e);
            throw new RuntimeException("文件删除失败", e);
        }
    }

    @Override
    public String getProvider() {
        return "s3";
    }

    private String buildUrl(String key) {
        if (StringUtils.hasText(properties.getPublicBaseUrl())) {
            String base = properties.getPublicBaseUrl();
            return base.endsWith("/") ? base + key : base + "/" + key;
        }
        return s3Client.utilities()
                .getUrl(GetUrlRequest.builder()
                        .bucket(properties.getBucket())
                        .key(key)
                        .build())
                .toString();
    }

    private String buildKey(UploadRequest request) {
        String ext = extractExtension(request.originalFilename());
        StringBuilder key = new StringBuilder();
        if (StringUtils.hasText(properties.getPathPrefix())) {
            key.append(properties.getPathPrefix()).append('/');
        }
        if (StringUtils.hasText(request.directory())) {
            key.append(request.directory()).append('/');
        }
        key.append(UUID.randomUUID()).append('.').append(ext);
        return key.toString();
    }

    private String extractExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "jpg";
        }
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "jpg" : filename.substring(dot + 1).toLowerCase();
    }
}
