package br.com.rizermarketplaces.core.marketplace.product;

import br.com.rizermarketplaces.core.marketplace.dto.MediaUploadResponse;
import br.com.rizermarketplaces.core.marketplace.model.Product;
import br.com.rizermarketplaces.core.marketplace.model.ProductImage;
import br.com.rizermarketplaces.core.marketplace.repository.ProductImageRepository;
import br.com.rizermarketplaces.core.marketplace.repository.ProductRepository;
import br.com.rizermarketplaces.core.marketplace.service.S3StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductImageServiceTest {

    @Mock
    private ProductImageRepository imageRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private S3StorageService s3StorageService;

    private ProductImageService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID IMAGE_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ProductImageService real = new ProductImageService(imageRepository, productRepository, s3StorageService);
        service = spy(real);
        service.setSelf(service);
    }

    @Test
    void upload_callsS3WithCorrectContext() throws IOException {
        MediaUploadResponse uploadResult = new MediaUploadResponse(
            "https://example.com/img.jpg", "uploads/products/img.jpg", "bucket", "image/jpeg", 1024L
        );
        when(s3StorageService.uploadPublicImage(any(),
            eq("tenants/" + TENANT_ID + "/products/" + PRODUCT_ID)))
            .thenReturn(uploadResult);
        when(productRepository.findByIdAndTenantIdAndDeletedAtIsNull(PRODUCT_ID, TENANT_ID))
            .thenReturn(Optional.of(new Product()));
        when(imageRepository.countByProductId(PRODUCT_ID)).thenReturn(1L);
        when(imageRepository.save(any(ProductImage.class))).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = new MockMultipartFile(
            "file", "car.jpg", "image/jpeg", "data".getBytes()
        );

        service.upload(TENANT_ID, PRODUCT_ID, file, false);

        verify(s3StorageService).uploadPublicImage(file,
            "tenants/" + TENANT_ID + "/products/" + PRODUCT_ID);
    }

    @Test
    void upload_savesImageRecordWithS3Data() throws IOException {
        String s3Key = "uploads/products/img.jpg";
        String s3Bucket = "rizer-pic";
        String publicUrl = "https://rizer-pic.br-se1.magaluobjects.com/uploads/products/img.jpg";

        MediaUploadResponse uploadResult = new MediaUploadResponse(
            publicUrl, s3Key, s3Bucket, "image/jpeg", 2048L
        );
        when(s3StorageService.uploadPublicImage(any(), anyString())).thenReturn(uploadResult);
        when(productRepository.findByIdAndTenantIdAndDeletedAtIsNull(PRODUCT_ID, TENANT_ID))
            .thenReturn(Optional.of(new Product()));
        when(imageRepository.countByProductId(PRODUCT_ID)).thenReturn(0L);
        when(imageRepository.save(any(ProductImage.class))).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = new MockMultipartFile(
            "file", "car.jpg", "image/jpeg", "data".getBytes()
        );

        service.upload(TENANT_ID, PRODUCT_ID, file, true);

        ArgumentCaptor<ProductImage> captor = ArgumentCaptor.forClass(ProductImage.class);
        verify(imageRepository).save(captor.capture());
        ProductImage saved = captor.getValue();
        assertEquals(s3Key, saved.getS3Key());
        assertEquals(s3Bucket, saved.getS3Bucket());
        assertEquals(publicUrl, saved.getPublicUrl());
    }

    @Test
    void upload_firstImage_becomesCover() throws IOException {
        MediaUploadResponse uploadResult = new MediaUploadResponse(
            "https://example.com/img.jpg", "uploads/products/img.jpg", "bucket", "image/jpeg", 1024L
        );
        when(s3StorageService.uploadPublicImage(any(), anyString())).thenReturn(uploadResult);
        when(productRepository.findByIdAndTenantIdAndDeletedAtIsNull(PRODUCT_ID, TENANT_ID))
            .thenReturn(Optional.of(new Product()));
        when(imageRepository.countByProductId(PRODUCT_ID)).thenReturn(0L);
        when(imageRepository.findByProductIdAndIsCoverTrue(PRODUCT_ID)).thenReturn(Optional.empty());
        when(imageRepository.save(any(ProductImage.class))).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = new MockMultipartFile(
            "file", "car.jpg", "image/jpeg", "data".getBytes()
        );

        service.upload(TENANT_ID, PRODUCT_ID, file, true);

        ArgumentCaptor<ProductImage> captor = ArgumentCaptor.forClass(ProductImage.class);
        verify(imageRepository).save(captor.capture());
        assertTrue(captor.getValue().isCover());
    }

    @Test
    void upload_existingCover_doesNotOverride() throws IOException {
        MediaUploadResponse uploadResult = new MediaUploadResponse(
            "https://example.com/img.jpg", "uploads/products/img.jpg", "bucket", "image/jpeg", 1024L
        );
        when(s3StorageService.uploadPublicImage(any(), anyString())).thenReturn(uploadResult);
        when(productRepository.findByIdAndTenantIdAndDeletedAtIsNull(PRODUCT_ID, TENANT_ID))
            .thenReturn(Optional.of(new Product()));
        when(imageRepository.countByProductId(PRODUCT_ID)).thenReturn(1L);
        when(imageRepository.save(any(ProductImage.class))).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = new MockMultipartFile(
            "file", "car.jpg", "image/jpeg", "data".getBytes()
        );

        service.upload(TENANT_ID, PRODUCT_ID, file, false);

        verify(imageRepository, never()).findByProductIdAndIsCoverTrue(any());
        ArgumentCaptor<ProductImage> captor = ArgumentCaptor.forClass(ProductImage.class);
        verify(imageRepository, times(1)).save(captor.capture());
        assertFalse(captor.getValue().isCover());
    }

    @Test
    void upload_productNotFound_throwsNotFound() throws IOException {
        MediaUploadResponse uploadResult = new MediaUploadResponse(
            "https://example.com/img.jpg", "uploads/products/img.jpg", "bucket", "image/jpeg", 1024L
        );
        when(s3StorageService.uploadPublicImage(any(), anyString())).thenReturn(uploadResult);
        when(imageRepository.countByProductId(PRODUCT_ID)).thenReturn(0L);
        when(productRepository.findByIdAndTenantIdAndDeletedAtIsNull(PRODUCT_ID, TENANT_ID))
            .thenReturn(Optional.empty());

        MockMultipartFile file = new MockMultipartFile(
            "file", "car.jpg", "image/jpeg", "data".getBytes()
        );

        assertThrows(ResponseStatusException.class,
            () -> service.upload(TENANT_ID, PRODUCT_ID, file, false));
    }

    @Test
    void delete_callsS3Delete() {
        ProductImage img = new ProductImage();
        img.setProductId(PRODUCT_ID);
        img.setS3Key("uploads/products/img.jpg");
        img.setS3Bucket("rizer-pic");

        when(productRepository.findByIdAndTenantIdAndDeletedAtIsNull(PRODUCT_ID, TENANT_ID))
            .thenReturn(Optional.of(new Product()));
        when(imageRepository.findById(IMAGE_ID)).thenReturn(Optional.of(img));

        service.delete(TENANT_ID, PRODUCT_ID, IMAGE_ID);

        verify(s3StorageService).deleteFile("uploads/products/img.jpg", "rizer-pic");
    }

    @Test
    void delete_s3Fails_recordStillDeleted() {
        ProductImage img = new ProductImage();
        img.setProductId(PRODUCT_ID);
        img.setS3Key("uploads/products/img.jpg");
        img.setS3Bucket("rizer-pic");

        when(productRepository.findByIdAndTenantIdAndDeletedAtIsNull(PRODUCT_ID, TENANT_ID))
            .thenReturn(Optional.of(new Product()));
        when(imageRepository.findById(IMAGE_ID)).thenReturn(Optional.of(img));
        doThrow(new RuntimeException("S3 connection error"))
            .when(s3StorageService).deleteFile(anyString(), anyString());

        service.delete(TENANT_ID, PRODUCT_ID, IMAGE_ID);

        verify(s3StorageService).deleteFile("uploads/products/img.jpg", "rizer-pic");
        verify(imageRepository).delete(img);
    }
}
