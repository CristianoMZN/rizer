package br.com.rizermarketplaces.core.marketplace.service;

import br.com.rizermarketplaces.core.marketplace.dto.MediaPresignResponse;
import br.com.rizermarketplaces.core.marketplace.dto.MediaUploadResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class S3StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${app.s3.endpoint:}")
    private String s3Endpoint;

    @Value("${app.s3.public.bucket}")
    private String publicBucket;

    @Value("${app.s3.public.key-prefix:uploads}")
    private String publicPrefix;

    @Value("${app.s3.private.bucket}")
    private String privateBucket;

    @Value("${app.s3.private.key-prefix:docs}")
    private String privatePrefix;

    @Value("${app.s3.private.presigned-duration-minutes:15}")
    private long presignedDurationMinutes;

    public S3StorageService(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    /**
     * Upload de imagem para o bucket público.
     * Retorna URL pública direta (sem assinatura).
     */
    public MediaUploadResponse uploadPublicImage(MultipartFile file, String context) throws IOException {
        String key = buildKey(publicPrefix, context, file.getOriginalFilename());

        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(publicBucket)
                .key(key)
                .contentType(file.getContentType())
                .build(),
            RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );

        String url = buildPublicUrl(key);

        return new MediaUploadResponse(
            url,
            key,
            publicBucket,
            file.getContentType(),
            file.getSize()
        );
    }

    /**
     * Upload de documento para o bucket privado.
     * Retorna presigned URL temporário.
     */
    public MediaUploadResponse uploadPrivateDocument(MultipartFile file, String context) throws IOException {
        String key = buildKey(privatePrefix, context, file.getOriginalFilename());

        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(privateBucket)
                .key(key)
                .contentType(file.getContentType())
                .build(),
            RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );

        String presignedUrl = generatePresignedUrl(privateBucket, key);

        return new MediaUploadResponse(
            presignedUrl,
            key,
            privateBucket,
            file.getContentType(),
            file.getSize()
        );
    }

    /**
     * Gera presigned URL para um documento privado existente.
     */
    public MediaPresignResponse getPresignedUrl(String key) {
        String presignedUrl = generatePresignedUrl(privateBucket, key);
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(presignedDurationMinutes));

        return new MediaPresignResponse(presignedUrl, expiresAt);
    }

    /**
     * Remove arquivo de qualquer bucket.
     */
    public void deleteFile(String key, String bucket) {
        s3Client.deleteObject(
            DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build()
        );
    }

    /**
     * Upload de bytes brutos (útil para exports LGPD gerados em memória).
     */
    public void uploadBytes(byte[] bytes, String key, String contentType, String prefix) {
        String finalKey = prefix + "/" + key;
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(privateBucket)
                .key(finalKey)
                .contentType(contentType)
                .build(),
            software.amazon.awssdk.core.sync.RequestBody.fromBytes(bytes)
        );
    }

    private String generatePresignedUrl(String bucket, String key) {
        var presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(presignedDurationMinutes))
            .getObjectRequest(b -> b.bucket(bucket).key(key))
            .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    private String buildKey(String prefix, String context, String originalFilename) {
        String ext = extractExtension(originalFilename);
        String id = UUID.randomUUID().toString();
        String ctx = (context != null && !context.isBlank()) ? context : "default";
        return prefix + "/" + ctx + "/" + id + ext;
    }

    private String buildPublicUrl(String key) {
        String endpoint = (s3Endpoint != null && !s3Endpoint.isBlank())
            ? s3Endpoint
            : "https://" + publicBucket + ".s3.amazonaws.com";
        return endpoint + "/" + key;
    }

    private String extractExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return (dot >= 0) ? filename.substring(dot) : "";
    }
}
