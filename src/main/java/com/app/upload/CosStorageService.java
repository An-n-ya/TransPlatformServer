package com.app.upload;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.model.DeleteObjectRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.util.UUID;

/**
 * Tencent Cloud COS (Cloud Object Storage) implementation.
 * Uses the native Tencent COS SDK. Selected when storage.provider=cos.
 */
@Service
@ConditionalOnProperty(name = "storage.provider", havingValue = "cos")
@RequiredArgsConstructor
@Slf4j
public class CosStorageService implements StorageService {

    private final COSClient cosClient;
    private final StorageProperties properties;

    @Override
    public UploadResult upload(UploadRequest request) {
        String key = buildKey(request);
        try (InputStream is = request.inputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            if (request.size() > 0) {
                metadata.setContentLength(request.size());
            }
            if (StringUtils.hasText(request.contentType())) {
                metadata.setContentType(request.contentType());
            }
            PutObjectRequest putReq = new PutObjectRequest(
                    properties.getCos().getBucket(), key, is, metadata);
            cosClient.putObject(putReq);
        } catch (CosClientException e) {
            log.error("COS upload failed for key: {}", key, e);
            throw new RuntimeException("文件上传失败", e);
        } catch (Exception e) {
            log.error("COS upload IO error for key: {}", key, e);
            throw new RuntimeException("文件上传失败", e);
        }
        String url = buildUrl(key);
        log.debug("COS upload ok, key={}, url={}", key, url);
        return new UploadResult(url, key, getProvider(), request.size());
    }

    @Override
    public void delete(String storageKey) {
        try {
            cosClient.deleteObject(new DeleteObjectRequest(
                    properties.getCos().getBucket(), storageKey));
        } catch (CosClientException e) {
            log.error("COS delete failed for key: {}", storageKey, e);
            throw new RuntimeException("文件删除失败", e);
        }
    }

    @Override
    public String getProvider() {
        return "cos";
    }

    private String buildUrl(String key) {
        StorageProperties.Cos cos = properties.getCos();

        // 1. 如果配置了公共基础 URL，直接拼接
        if (StringUtils.hasText(cos.getPublicBaseUrl())) {
            String base = cos.getPublicBaseUrl();
            return base.endsWith("/") ? base + key : base + "/" + key;
        }

        // 2. 如果有自定义 endpoint
        String endpoint = cos.getEndpoint();
        if (StringUtils.hasText(endpoint)) {
            // 2a. endpoint 已是完整 URL（含 https:// 和 bucket）
            if (endpoint.startsWith("http://") || endpoint.startsWith("https://")) {
                String base = endpoint.endsWith("/") ? endpoint : endpoint + "/";
                return base + key;
            }
            // 2b. endpoint 只是 host（如 "cos.ap-nanjing.myqcloud.com"）
            return "https://" + cos.getBucket() + "." + endpoint + "/" + key;
        }

        // 3. 默认：cos.{region}.myqcloud.com
        return "https://" + cos.getBucket() + ".cos." + cos.getRegion() + ".myqcloud.com/" + key;
    }

    private String buildKey(UploadRequest request) {
        String ext = extractExtension(request.originalFilename());
        StringBuilder key = new StringBuilder();
        if (StringUtils.hasText(properties.getCos().getPathPrefix())) {
            key.append(properties.getCos().getPathPrefix()).append('/');
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
