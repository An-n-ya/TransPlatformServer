package com.app.upload;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/**
 * Validates uploaded image files at the system boundary.
 * Throws IllegalArgumentException on validation failure (handled as 400
 * by GlobalExceptionHandler).
 */
@Component
public class ImageValidator {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "bmp");

    private static final long MAX_SIZE = 10L * 1024 * 1024; // 10MB

    /**
     * Validate that the multipart file is a non-empty image within size limit.
     */
    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("文件大小不能超过 10MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("仅允许上传图片文件");
        }
        String extension = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("不支持的图片格式: " + extension);
        }
    }

    private String extractExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            throw new IllegalArgumentException("文件缺少扩展名");
        }
        return filename.substring(dot + 1).toLowerCase();
    }
}
