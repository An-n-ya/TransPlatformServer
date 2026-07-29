package com.app.upload;

import com.app.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * File upload controller - server-side upload to cloud object storage.
 * Frontend submits the image via multipart/form-data; the backend
 * validates, uploads via StorageService and returns the accessible URL.
 */
@RestController
@RequestMapping("/api/v1/upload")
@Tag(name = "文件上传", description = "服务端上传图片至云端对象存储")
@RequiredArgsConstructor
public class UploadController {

    private final StorageService storageService;
    private final ImageValidator imageValidator;

    /**
     * 上传单张图片。前端以 multipart/form-data 提交，
     * 后端校验后上传至云端并返回可访问 URL。
     */
    @PostMapping("/image")
    @Operation(summary = "上传图片")
    public ApiResponse<UploadResult> uploadImage(
            @AuthenticationPrincipal Long userId,
            @RequestParam("file") MultipartFile file) throws IOException {

        imageValidator.validate(file);
        UploadRequest request = new UploadRequest(
                file.getInputStream(),
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                null);
        UploadResult result = storageService.upload(request);
        return ApiResponse.success(result);
    }
}
