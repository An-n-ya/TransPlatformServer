package com.app.upload;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed binding for the {@code storage.*} config keys.
 * Loaded from storage.yml (imported via spring.config.import).
 */
@Data
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    /** Provider selector: mock | s3 (future: r2, oss, cos, ...). */
    private String provider = "mock";

    /** S3-compatible endpoint. Empty for AWS default. */
    private String endpoint;

    private String accessKey;
    private String secretKey;
    private String bucket;
    private String region;

    /** Base URL used to build the public URL (CDN/custom domain). */
    private String publicBaseUrl;

    /** Default sub-directory under the bucket root. */
    private String pathPrefix = "uploads";

    /** Tencent Cloud COS specific configuration (used when provider=cos). */
    private Cos cos = new Cos();

    @Data
    public static class Cos {
        /** Tencent Cloud API SecretId. */
        private String secretId;
        /** Tencent Cloud API SecretKey. */
        private String secretKey;
        /** Bucket region, e.g. ap-guangzhou. */
        private String region;
        /** Bucket name in the form {name}-{appid}. */
        private String bucket;
        /** Optional COS endpoint, auto-derived from region if absent. */
        private String endpoint;
        /** Optional CDN/custom domain for building the public URL. */
        private String publicBaseUrl;
        /** Default sub-directory under the bucket root. */
        private String pathPrefix = "uploads";
    }
}
