package br.com.rizermarketplaces.core.marketplace.tools;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

public class PresignDemo {

    public static void main(String[] args) {
        String accessKey = Optional.ofNullable(System.getenv("AWS_ACCESS_KEY_ID")).orElse("");
        String secretKey = Optional.ofNullable(System.getenv("AWS_SECRET_ACCESS_KEY")).orElse("");
        String region = Optional.ofNullable(System.getenv("APP_S3_REGION")).orElse("us-east-1");
        String endpoint = Optional.ofNullable(System.getenv("APP_S3_ENDPOINT")).orElse("");
        boolean pathStyle = Boolean.parseBoolean(Optional.ofNullable(System.getenv("APP_S3_PATH_STYLE")).orElse("true"));
        String bucket = Optional.ofNullable(System.getenv("APP_S3_BUCKET")).orElse("rizer");
        String objectKey = args.length > 0 ? args[0] : "uploads/announce-gallery/demo.webp";
        long durationMinutes = 60;

        S3Presigner.Builder presignerBuilder = S3Presigner.builder().region(Region.of(region));

        if (!accessKey.isBlank() && !secretKey.isBlank()) {
            presignerBuilder.credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))
            );
        }

        if (!endpoint.isBlank()) {
            presignerBuilder
                .endpointOverride(URI.create(endpoint))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(pathStyle).build());
        }

        try (S3Presigner presigner = presignerBuilder.build()) {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(durationMinutes))
                .getObjectRequest(getObjectRequest)
                .build();

            PresignedGetObjectRequest presigned = presigner.presignGetObject(presignRequest);

            System.out.println("=== Presigned URL (demo) ===");
            System.out.println(presigned.url().toString());
            System.out.println();
            System.out.println("=== Query string / token ===");
            System.out.println(presigned.url().getQuery());
        } catch (Exception e) {
            System.err.println("Failed to generate presigned URL: " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
    }
}
