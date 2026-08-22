package com.app.admin;

import com.app.common.ApiResponse;
import com.app.upload.ImageValidator;
import com.app.upload.StorageService;
import com.app.upload.UploadRequest;
import com.app.upload.UploadResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 管理后台图片上传控制器 — 上传图片至云端对象存储
 */
@RestController
@RequestMapping("/admin/v1/upload")
@RequiredArgsConstructor
@Tag(name = "管理后台-上传", description = "管理员上传图片")
public class AdminUploadController {

    private final StorageService storageService;
    private final ImageValidator imageValidator;

    @PostMapping("/image")
    @Operation(summary = "管理员上传图片")
    public ApiResponse<UploadResult> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        imageValidator.validate(file);
        UploadRequest request = new UploadRequest(
                file.getInputStream(),
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                null);
        return ApiResponse.success(storageService.upload(request));
    }
}
