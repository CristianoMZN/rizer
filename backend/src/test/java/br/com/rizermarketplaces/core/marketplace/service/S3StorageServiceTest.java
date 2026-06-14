package br.com.rizermarketplaces.core.marketplace.service;

import br.com.rizermarketplaces.core.marketplace.dto.MediaPresignResponse;
import br.com.rizermarketplaces.core.marketplace.dto.MediaUploadResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3StorageServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    private S3StorageService service;

    private static final String PUBLIC_BUCKET = "rizer-pic";
    private static final String PRIVATE_BUCKET = "rizer-storage";
    private static final String PUBLIC_PREFIX = "uploads";
    private static final String PRIVATE_PREFIX = "docs";
    private static final long PRESIGNED_DURATION = 15;
    private static final String S3_ENDPOINT = "https://br-se1.magaluobjects.com";

    @BeforeEach
    void setUp() {
        service = spy(new S3StorageService(s3Client, s3Presigner));
        ReflectionTestUtils.setField(service, "s3Endpoint", S3_ENDPOINT);
        ReflectionTestUtils.setField(service, "publicBucket", PUBLIC_BUCKET);
        ReflectionTestUtils.setField(service, "publicPrefix", PUBLIC_PREFIX);
        ReflectionTestUtils.setField(service, "privateBucket", PRIVATE_BUCKET);
        ReflectionTestUtils.setField(service, "privatePrefix", PRIVATE_PREFIX);
        ReflectionTestUtils.setField(service, "presignedDurationMinutes", PRESIGNED_DURATION);
    }

    @Test
    void uploadPublicImage_delegatesToS3Client() throws IOException {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file", "car.webp", "image/webp", "fake-image-data".getBytes()
        );
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);

        // Act
        MediaUploadResponse response = service.uploadPublicImage(file, "vehicles");

        // Assert
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        PutObjectRequest captured = requestCaptor.getValue();
        assertEquals(PUBLIC_BUCKET, captured.bucket());
        assertTrue(captured.key().startsWith(PUBLIC_PREFIX + "/vehicles/"));
        assertEquals("image/webp", captured.contentType());
        assertEquals(PUBLIC_BUCKET, response.bucket());
        assertEquals("image/webp", response.contentType());
        assertEquals(file.getSize(), response.sizeBytes());
    }

    @Test
    void uploadPublicImage_buildsCorrectKey() throws IOException {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file", "photo.jpg", "image/jpeg", "data".getBytes()
        );
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);

        // Act
        service.uploadPublicImage(file, "stores");

        // Assert
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        String key = requestCaptor.getValue().key();
        assertTrue(key.matches("^uploads/stores/[0-9a-f-]+\\.jpg$"), "Key should match pattern: " + key);
    }

    @Test
    void uploadPublicImage_buildsCorrectUrl() throws IOException {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file", "img.png", "image/png", "data".getBytes()
        );

        // Act
        MediaUploadResponse response = service.uploadPublicImage(file, "ctx");

        // Assert
        assertTrue(response.url().startsWith(S3_ENDPOINT + "/" + PUBLIC_BUCKET + "/"),
            "URL should start with endpoint + bucket: " + response.url());
        assertTrue(response.url().endsWith(".png"));
    }

    @Test
    void uploadPublicImage_withNullContext_usesDefault() throws IOException {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file", "pic.jpg", "image/jpeg", "data".getBytes()
        );
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);

        // Act
        service.uploadPublicImage(file, null);

        // Assert
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        assertTrue(requestCaptor.getValue().key().startsWith(PUBLIC_PREFIX + "/default/"),
            "Null context should produce 'default' in key: " + requestCaptor.getValue().key());
    }

    @Test
    void uploadPublicImage_preservesExtension() throws IOException {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file", "photo.webp", "image/webp", "data".getBytes()
        );
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);

        // Act
        MediaUploadResponse response = service.uploadPublicImage(file, "ctx");

        // Assert
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        assertTrue(requestCaptor.getValue().key().endsWith(".webp"));
        assertTrue(response.key().endsWith(".webp"));
        assertTrue(response.url().endsWith(".webp"));
    }

    @Test
    void uploadPublicImage_noExtension_noDot() throws IOException {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file", "Makefile", "application/octet-stream", "data".getBytes()
        );
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);

        // Act
        MediaUploadResponse response = service.uploadPublicImage(file, "ctx");

        // Assert
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        String key = requestCaptor.getValue().key();
        assertFalse(key.endsWith("."), "Key should not end with a dot: " + key);
        assertFalse(response.key().endsWith("."));
    }

    @Test
    void uploadPublicImage_s3Throws_propagatesException() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file", "img.jpg", "image/jpeg", "data".getBytes()
        );
        RuntimeException s3Exception = new RuntimeException("S3 connection timeout");
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenThrow(s3Exception);

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class,
            () -> service.uploadPublicImage(file, "ctx"));
        assertEquals("S3 connection timeout", thrown.getMessage());
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void uploadPrivateDocument_usesPrivateBucket() throws IOException {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file", "contract.pdf", "application/pdf", "pdf-content".getBytes()
        );
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        doReturn("https://presigned.example.com/contract.pdf")
            .when(service).generatePresignedUrl(anyString(), anyString());

        // Act
        service.uploadPrivateDocument(file, "contracts");

        // Assert
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        PutObjectRequest captured = requestCaptor.getValue();
        assertEquals(PRIVATE_BUCKET, captured.bucket());
        assertTrue(captured.key().startsWith(PRIVATE_PREFIX + "/contracts/"));
        assertEquals("application/pdf", captured.contentType());
    }

    @Test
    void uploadPrivateDocument_generatesPresignedUrl() throws IOException {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file", "report.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "xlsx-content".getBytes()
        );
        String expectedPresignedUrl = "https://presigned.example.com/report.xlsx?X-Amz-Signature=abc";
        doReturn(expectedPresignedUrl)
            .when(service).generatePresignedUrl(anyString(), anyString());

        // Act
        MediaUploadResponse response = service.uploadPrivateDocument(file, "reports");

        // Assert
        assertEquals(expectedPresignedUrl, response.url());
        assertEquals(PRIVATE_BUCKET, response.bucket());
    }

    @Test
    void deleteFile_callsDeleteObject() {
        // Arrange
        String key = "uploads/vehicles/123e4567-e89b-12d3-a456-426614174000.jpg";
        String bucket = PUBLIC_BUCKET;
        ArgumentCaptor<DeleteObjectRequest> requestCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);

        // Act
        service.deleteFile(key, bucket);

        // Assert
        verify(s3Client).deleteObject(requestCaptor.capture());
        assertEquals(bucket, requestCaptor.getValue().bucket());
        assertEquals(key, requestCaptor.getValue().key());
    }

    @Test
    void uploadBytes_usesPrivateBucketWithPrefix() {
        // Arrange
        byte[] bytes = "file-content".getBytes();
        String key = "report.pdf";
        String prefix = "custom-prefix";
        String contentType = "application/pdf";
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);

        // Act
        service.uploadBytes(bytes, key, contentType, prefix);

        // Assert
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        PutObjectRequest captured = requestCaptor.getValue();
        assertEquals(PRIVATE_BUCKET, captured.bucket());
        assertEquals(prefix + "/" + key, captured.key());
        assertEquals(contentType, captured.contentType());
    }

    @Test
    void uploadPublicImage_withoutEndpoint_fallsBackToAWS() throws IOException {
        // Arrange
        ReflectionTestUtils.setField(service, "s3Endpoint", null);
        MockMultipartFile file = new MockMultipartFile(
            "file", "img.jpg", "image/jpeg", "data".getBytes()
        );

        // Act
        MediaUploadResponse response = service.uploadPublicImage(file, "ctx");

        // Assert
        String expectedPrefix = "https://" + PUBLIC_BUCKET + ".s3.amazonaws.com/";
        assertTrue(response.url().startsWith(expectedPrefix),
            "URL should use AWS fallback: " + response.url());
    }

    @Test
    void uploadPublicImage_blankEndpoint_fallsBackToAWS() throws IOException {
        // Arrange
        ReflectionTestUtils.setField(service, "s3Endpoint", "   ");
        MockMultipartFile file = new MockMultipartFile(
            "file", "img.jpg", "image/jpeg", "data".getBytes()
        );

        // Act
        MediaUploadResponse response = service.uploadPublicImage(file, "ctx");

        // Assert
        String expectedPrefix = "https://" + PUBLIC_BUCKET + ".s3.amazonaws.com/";
        assertTrue(response.url().startsWith(expectedPrefix),
            "Blank endpoint should fall back to AWS: " + response.url());
    }
}
