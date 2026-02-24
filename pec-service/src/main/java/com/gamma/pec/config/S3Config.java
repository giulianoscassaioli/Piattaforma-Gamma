package com.gamma.pec.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

import java.net.URI;

@Configuration
@Slf4j
public class S3Config {

    @Value("${minio.url:http://localhost:9000}")
    private String minioUrl;

    @Value("${minio.access-key:minioadmin}")
    private String accessKey;

    @Value("${minio.secret-key:minioadmin}")
    private String secretKey;

    @Value("${minio.bucket:gamma-allegati}")
    private String bucket;

    @Bean
    public S3Client s3Client() {
        S3Client client = S3Client.builder()
                .endpointOverride(URI.create(minioUrl))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .region(Region.US_EAST_1)
                .forcePathStyle(true)
                .httpClient(UrlConnectionHttpClient.create())
                .build();

        // crea il bucket se non esiste già
        try {
            client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            log.info("Bucket MinIO creato: {}", bucket);
        } catch (Exception e) {
            log.debug("Bucket MinIO già esistente: {}", bucket);
        }

        return client;
    }
}
