package com.medsimple.media.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

/**
 * Builds the {@link S3Client} for the object-store media backend, only when
 * media.storage.adapter=s3. Points at any S3-compatible endpoint; for Supabase Storage the
 * endpoint is https://&lt;project-ref&gt;.storage.supabase.co/storage/v1/s3.
 *
 * <p>Path-style access is forced: S3-compatible stores (Supabase, MinIO) serve buckets as a path
 * segment, not a vhost subdomain, so virtual-host addressing would resolve the wrong host.
 */
@Configuration
@ConditionalOnProperty(name = "media.storage.adapter", havingValue = "s3")
public class MediaStorageS3Config {

    @Bean
    S3Client mediaS3Client(
            @Value("${media.storage.s3.endpoint}") String endpoint,
            @Value("${media.storage.s3.region}") String region,
            @Value("${media.storage.s3.access-key}") String accessKey,
            @Value("${media.storage.s3.secret-key}") String secretKey) {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }
}
