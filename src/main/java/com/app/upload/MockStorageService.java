package com.app.upload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Mock storage implementation for dev/test. Persists files to a local
 * {@code ./uploads/} directory and returns a URL that can be accessed
 * via the application's static resource handler at {@code /uploads/**}.
 * <p>
 * Selected when {@code storage.provider=mock} (also the default).
 */
@Service
@ConditionalOnProperty(name = "storage.provider", havingValue = "mock", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class MockStorageService implements StorageService {

    /** Uploaded files are stored under the project's ./uploads/ directory. */
    private static final Path LOCAL_DIR = Paths.get("uploads");

    private final StorageProperties properties;

    /** Injected from server.port so the returned URL uses the correct port. */
    @Value("${server.port:8081}")
    private int serverPort;

    @Override
    public UploadResult upload(UploadRequest request) {
        String key = buildKey(request);
        try (InputStream is = request.inputStream()) {
            Path target = LOCAL_DIR.resolve(key);
            Files.createDirectories(target.getParent());
            Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Mock upload failed for key: {}", key, e);
            throw new RuntimeException("文件上传失败", e);
        }
        String url = String.format("http://localhost:%d/uploads/%s", serverPort, key);
        log.debug("Mock upload ok, key={}, url={}", key, url);
        return new UploadResult(url, key, getProvider(), request.size());
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(LOCAL_DIR.resolve(storageKey));
        } catch (IOException e) {
            log.warn("Mock delete failed for key: {}", storageKey, e);
        }
    }

    @Override
    public String getProvider() {
        return "mock";
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
