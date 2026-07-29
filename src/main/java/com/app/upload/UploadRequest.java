package com.app.upload;

import java.io.InputStream;

/**
 * Carrier for a single file upload. Web-agnostic so the StorageService
 * stays reusable outside the controller layer.
 *
 * @param inputStream      file content stream (closed by the service impl)
 * @param originalFilename original file name, e.g. "photo.jpg"
 * @param contentType      MIME type, e.g. "image/jpeg"
 * @param size             content length in bytes (-1 if unknown)
 * @param directory        optional sub-directory under the bucket root,
 *                         e.g. "images/posts"; may be null
 */
public record UploadRequest(
        InputStream inputStream,
        String originalFilename,
        String contentType,
        long size,
        String directory
) {}
