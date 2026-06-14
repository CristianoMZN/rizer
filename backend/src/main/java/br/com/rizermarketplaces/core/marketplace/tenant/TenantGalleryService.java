package br.com.rizermarketplaces.core.marketplace.tenant;

import br.com.rizermarketplaces.core.marketplace.dto.GalleryImageView;
import br.com.rizermarketplaces.core.marketplace.model.Tenant;
import br.com.rizermarketplaces.core.marketplace.model.TenantGalleryImage;
import br.com.rizermarketplaces.core.marketplace.repository.TenantGalleryImageRepository;
import br.com.rizermarketplaces.core.marketplace.repository.TenantRepository;
import br.com.rizermarketplaces.core.marketplace.service.S3StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class TenantGalleryService {

    private final TenantGalleryImageRepository repository;
    private final TenantRepository tenantRepository;
    private final S3StorageService s3;

    public TenantGalleryService(
        TenantGalleryImageRepository repository,
        TenantRepository tenantRepository,
        S3StorageService s3
    ) {
        this.repository = repository;
        this.tenantRepository = tenantRepository;
        this.s3 = s3;
    }

    @Transactional(readOnly = true)
    public List<GalleryImageView> list(UUID tenantId) {
        ensureTenant(tenantId);
        return repository.findAllByTenantIdOrderBySortOrderAscCreatedAtAsc(tenantId)
            .stream().map(this::toView).toList();
    }

    @Transactional
    public GalleryImageView upload(UUID tenantId, MultipartFile file, String caption) throws IOException {
        ensureTenant(tenantId);
        var up = s3.uploadPublicImage(file, "tenant-gallery/" + tenantId);
        TenantGalleryImage img = new TenantGalleryImage();
        img.setTenantId(tenantId);
        img.setS3Key(up.key());
        img.setS3Bucket(up.bucket());
        img.setPublicUrl(up.url());
        img.setCaption(caption);
        img.setSortOrder((short) repository.countByTenantId(tenantId));
        img.setCover(repository.countByTenantId(tenantId) == 0);
        return toView(repository.save(img));
    }

    @Transactional
    public GalleryImageView setCover(UUID tenantId, UUID imageId) {
        ensureTenant(tenantId);
        TenantGalleryImage img = repository.findByIdAndTenantId(imageId, tenantId)
            .orElseThrow(() -> TenantExceptions.notFound("Imagem"));
        repository.findByTenantIdAndIsCoverTrue(tenantId).ifPresent(current -> {
            if (!current.getId().equals(imageId)) {
                current.setCover(false);
                repository.save(current);
            }
        });
        img.setCover(true);
        return toView(repository.save(img));
    }

    @Transactional
    public void reorder(UUID tenantId, List<UUID> orderedIds) {
        ensureTenant(tenantId);
        short order = 0;
        for (UUID id : orderedIds) {
            TenantGalleryImage img = repository.findByIdAndTenantId(id, tenantId).orElse(null);
            if (img == null) continue;
            img.setSortOrder(order++);
            repository.save(img);
        }
    }

    @Transactional
    public void delete(UUID tenantId, UUID imageId) {
        ensureTenant(tenantId);
        TenantGalleryImage img = repository.findByIdAndTenantId(imageId, tenantId)
            .orElseThrow(() -> TenantExceptions.notFound("Imagem"));
        repository.delete(img);
        s3.deleteFile(img.getS3Key(), img.getS3Bucket());
    }

    private void ensureTenant(UUID tenantId) {
        Tenant t = tenantRepository.findById(tenantId).orElseThrow(() -> TenantExceptions.notFound("Tenant"));
        if (t.getDeletedAt() != null) throw TenantExceptions.notFound("Tenant");
    }

    private GalleryImageView toView(TenantGalleryImage i) {
        return new GalleryImageView(i.getId(), i.getPublicUrl(), i.getCaption(), i.getSortOrder(), i.isCover(), i.getCreatedAt());
    }
}
