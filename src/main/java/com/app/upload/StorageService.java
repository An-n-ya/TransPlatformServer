package com.app.upload;

/**
 * Storage abstraction that decouples upload logic from the underlying
 * storage protocol (S3-compatible, Cloudflare, Aliyun OSS, etc.).
 * New providers implement this interface and are selected via
 * the {@code storage.provider} config property.
 */
public interface StorageService {

    /**
     * Upload a file stream to cloud storage and return the accessible URL
     * together with the storage key (for later deletion).
     *
     * @param request upload metadata + content stream
     * @return upload result containing the public URL and storage key
     */
    UploadResult upload(UploadRequest request);

    /**
     * Delete a file by its storage key.
     *
     * @param storageKey the key returned by {@link #upload}
     */
    void delete(String storageKey);

    /**
     * @return the provider identifier, e.g. "mock", "s3", "r2"
     */
    String getProvider();
}
