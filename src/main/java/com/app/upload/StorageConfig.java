package com.app.upload;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;

import java.net.URI;

/**
 * Registers the typed StorageProperties and builds the S3Client bean
 * (only when storage.provider=s3). Client construction is kept here so
 * the S3StorageService stays focused on business logic.
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    @Bean
    @ConditionalOnProperty(name = "storage.provider", havingValue = "s3")
    public S3Client s3Client(StorageProperties props) {
        String region = StringUtils.hasText(props.getRegion()) ? props.getRegion() : "us-east-1";
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                props.getAccessKey(), props.getSecretKey())));
        if (StringUtils.hasText(props.getEndpoint())) {
            builder.endpointOverride(URI.create(props.getEndpoint()))
                    .serviceConfiguration(s -> s.pathStyleAccessEnabled(true));
        }
        return builder.build();
    }

    /**
     * Tencent Cloud COS client. Only created when storage.provider=cos.
     */
    @Bean
    @ConditionalOnProperty(name = "storage.provider", havingValue = "cos")
    public COSClient cosClient(StorageProperties props) {
        StorageProperties.Cos cos = props.getCos();
        COSCredentials cred = new BasicCOSCredentials(cos.getSecretId(), cos.getSecretKey());
        ClientConfig clientConfig = new ClientConfig(new com.qcloud.cos.region.Region(cos.getRegion()));
        return new COSClient(cred, clientConfig);
    }
}
