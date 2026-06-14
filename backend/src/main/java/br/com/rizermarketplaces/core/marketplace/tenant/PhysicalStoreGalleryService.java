package br.com.rizermarketplaces.core.marketplace.tenant;

import br.com.rizermarketplaces.core.marketplace.dto.GalleryImageView;
import br.com.rizermarketplaces.core.marketplace.model.PhysicalStore;
import br.com.rizermarketplaces.core.marketplace.model.PhysicalStoreGalleryImage;
import br.com.rizermarketplaces.core.marketplace.model.Tenant;
import br.com.rizermarketplaces.core.marketplace.repository.PhysicalStoreGalleryImageRepository;
import br.com.rizermarketplaces.core.marketplace.repository.PhysicalStoreRepository;
import br.com.rizermarketplaces.core.marketplace.repository.TenantRepository;
import br.com.rizermarketplaces.core.marketplace.service.S3StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class PhysicalStoreGalleryService {

    private final PhysicalStoreGalleryImageRepository repository;
    private final PhysicalStoreRepository storeRepository;
    private final TenantRepository tenantRepository;
    private final S3StorageService s3;

    public PhysicalStoreGalleryService(
        PhysicalStoreGalleryImageRepository repository,
        PhysicalStoreRepository storeRepository,
        TenantRepository tenantRepository,
        S3StorageService s3
    ) {
        this.repository = repository;
        this.storeRepository = storeRepository;
        this.tenantRepository = tenantRepository;
        this.s3 = s3;
    }

    @Transactional(readOnly = true)
    public List<GalleryImageView> list(UUID tenantId, UUID storeId) {
        ensureStore(tenantId, storeId);
        return repository.findAllByPhysicalStoreIdOrderBySortOrderAscCreatedAtAsc(storeId)
            .stream().map(this::toView).toList();
    }

    @Transactional
    public GalleryImageView upload(UUID tenantId, UUID storeId, MultipartFile file, String caption) throws IOException {
        ensureStore(tenantId, storeId);
        var up = s3.uploadPublicImage(file, "store-gallery/" + storeId);
        PhysicalStoreGalleryImage img = new PhysicalStoreGalleryImage();
        img.setPhysicalStoreId(storeId);
        img.setS3Key(up.key());
        img.setS3Bucket(up.bucket());
        img.setPublicUrl(up.url());
        img.setCaption(caption);
        img.setSortOrder((short) repository.countByPhysicalStoreId(storeId));
        img.setCover(repository.countByPhysicalStoreId(storeId) == 0);
        return toView(repository.save(img));
    }

    @Transactional
    public GalleryImageView setCover(UUID tenantId, UUID storeId, UUID imageId) {
        ensureStore(tenantId, storeId);
        PhysicalStoreGalleryImage img = repository.findByIdAndPhysicalStoreId(imageId, storeId)
            .orElseThrow(() -> TenantExceptions.notFound("Imagem"));
        repository.findByPhysicalStoreIdAndIsCoverTrue(storeId).ifPresent(current -> {
            if (!current.getId().equals(imageId)) {
                current.setCover(false);
                repository.save(current);
            }
        });
        img.setCover(true);
        return toView(repository.save(img));
    }

    @Transactional
    public void reorder(UUID tenantId, UUID storeId, List<UUID> orderedIds) {
        ensureStore(tenantId, storeId);
        short order = 0;
        for (UUID id : orderedIds) {
            PhysicalStoreGalleryImage img = repository.findByIdAndPhysicalStoreId(id, storeId).orElse(null);
            if (img == null) continue;
            img.setSortOrder(order++);
            repository.save(img);
        }
    }

    @Transactional
    public void delete(UUID tenantId, UUID storeId, UUID imageId) {
        ensureStore(tenantId, storeId);
        PhysicalStoreGalleryImage img = repository.findByIdAndPhysicalStoreId(imageId, storeId)
            .orElseThrow(() -> TenantExceptions.notFound("Imagem"));
        repository.delete(img);
        s3.deleteFile(img.getS3Key(), img.getS3Bucket());
    }

    private void ensureStore(UUID tenantId, UUID storeId) {
        PhysicalStore s = storeRepository.findByIdAndTenantIdAndDeletedAtIsNull(storeId, tenantId)
            .orElseThrow(() -> TenantExceptions.notFound("Loja"));
        Tenant t = tenantRepository.findById(tenantId).orElseThrow(() -> TenantExceptions.notFound("Tenant"));
        if (t.getDeletedAt() != null) throw TenantExceptions.notFound("Tenant");
    }

    private GalleryImageView toView(PhysicalStoreGalleryImage i) {
        return new GalleryImageView(i.getId(), i.getPublicUrl(), i.getCaption(), i.getSortOrder(), i.isCover(), i.getCreatedAt());
    }
}
