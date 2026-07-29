package com.app.upload;

/**
 * Result of a successful upload.
 *
 * @param url        publicly accessible URL (or CDN URL)
 * @param storageKey key under which the file was stored (for deletion)
 * @param provider   storage provider that handled the upload
 * @param size       uploaded size in bytes
 */
public record UploadResult(
        String url,
        String storageKey,
        String provider,
        long size
) {}
