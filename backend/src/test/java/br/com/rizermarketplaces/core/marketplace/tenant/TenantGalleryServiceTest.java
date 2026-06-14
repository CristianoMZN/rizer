package br.com.rizermarketplaces.core.marketplace.tenant;

import br.com.rizermarketplaces.core.marketplace.dto.GalleryImageView;
import br.com.rizermarketplaces.core.marketplace.dto.MediaUploadResponse;
import br.com.rizermarketplaces.core.marketplace.model.Tenant;
import br.com.rizermarketplaces.core.marketplace.model.TenantGalleryImage;
import br.com.rizermarketplaces.core.marketplace.repository.TenantGalleryImageRepository;
import br.com.rizermarketplaces.core.marketplace.repository.TenantRepository;
import br.com.rizermarketplaces.core.marketplace.service.S3StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantGalleryServiceTest {

    @Mock
    private TenantGalleryImageRepository repository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private S3StorageService s3;

    @Mock
    private MultipartFile file;

    @InjectMocks
    private TenantGalleryService service;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
    }

    private void stubValidTenant() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(new Tenant()));
    }

    private TenantGalleryImage buildImage(UUID id, boolean cover) {
        TenantGalleryImage img = new TenantGalleryImage();
        if (id != null) {
            ReflectionTestUtils.setField(img, "id", id);
        }
        img.setCover(cover);
        return img;
    }

    @Test
    void upload_firstImage_becomesCover() throws IOException {
        stubValidTenant();
        when(repository.countByTenantId(tenantId)).thenReturn(0L);

        when(s3.uploadPublicImage(eq(file), eq("tenant-gallery/" + tenantId)))
            .thenReturn(new MediaUploadResponse("https://s3.example.com/img.webp", "uploads/img.webp", "bucket", "image/webp", 1024L));
        when(repository.save(org.mockito.ArgumentMatchers.any(TenantGalleryImage.class)))
            .thenAnswer(inv -> {
                TenantGalleryImage img = inv.getArgument(0);
                ReflectionTestUtils.setField(img, "id", UUID.randomUUID());
                return img;
            });

        service.upload(tenantId, file, "Caption");

        ArgumentCaptor<TenantGalleryImage> captor = ArgumentCaptor.forClass(TenantGalleryImage.class);
        verify(repository).save(captor.capture());
        TenantGalleryImage saved = captor.getValue();
        assertTrue(saved.isCover());
        assertEquals(0, saved.getSortOrder());
    }

    @Test
    void upload_callsS3WithContext() throws IOException {
        stubValidTenant();
        when(repository.countByTenantId(tenantId)).thenReturn(1L);

        when(s3.uploadPublicImage(eq(file), eq("tenant-gallery/" + tenantId)))
            .thenReturn(new MediaUploadResponse("https://s3.example.com/img.webp", "uploads/img.webp", "bucket", "image/webp", 1024L));
        when(repository.save(org.mockito.ArgumentMatchers.any(TenantGalleryImage.class)))
            .thenAnswer(inv -> {
                TenantGalleryImage img = inv.getArgument(0);
                ReflectionTestUtils.setField(img, "id", UUID.randomUUID());
                return img;
            });

        service.upload(tenantId, file, null);

        verify(s3).uploadPublicImage(eq(file), eq("tenant-gallery/" + tenantId));
    }

    @Test
    void delete_removesRecordAndS3() {
        stubValidTenant();

        TenantGalleryImage img = new TenantGalleryImage();
        img.setS3Key("uploads/gallery.webp");
        img.setS3Bucket("rizer-pic");

        when(repository.findByIdAndTenantId(org.mockito.ArgumentMatchers.any(UUID.class), eq(tenantId)))
            .thenReturn(Optional.of(img));

        service.delete(tenantId, UUID.randomUUID());

        verify(repository).delete(img);
        verify(s3).deleteFile("uploads/gallery.webp", "rizer-pic");
    }

    @Test
    void setCover_unsetsPreviousCover() {
        stubValidTenant();

        UUID newCoverId = UUID.randomUUID();
        UUID previousCoverId = UUID.randomUUID();

        TenantGalleryImage previousCover = buildImage(previousCoverId, true);
        TenantGalleryImage newCover = buildImage(newCoverId, false);

        when(repository.findByIdAndTenantId(eq(newCoverId), eq(tenantId)))
            .thenReturn(Optional.of(newCover));
        when(repository.findByTenantIdAndIsCoverTrue(tenantId))
            .thenReturn(Optional.of(previousCover));
        when(repository.save(org.mockito.ArgumentMatchers.any(TenantGalleryImage.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        GalleryImageView result = service.setCover(tenantId, newCoverId);

        assertTrue(result.isCover());
        ArgumentCaptor<TenantGalleryImage> captor = ArgumentCaptor.forClass(TenantGalleryImage.class);
        verify(repository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertEquals(false, captor.getAllValues().get(0).isCover());
        assertEquals(true, captor.getAllValues().get(1).isCover());
    }
}
