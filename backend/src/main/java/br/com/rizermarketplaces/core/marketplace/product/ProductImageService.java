package br.com.rizermarketplaces.core.marketplace.product;

import br.com.rizermarketplaces.core.marketplace.dto.AttachImageRequest;
import br.com.rizermarketplaces.core.marketplace.dto.ProductView;
import br.com.rizermarketplaces.core.marketplace.dto.UploadResponse;
import br.com.rizermarketplaces.core.marketplace.model.ProductImage;
import br.com.rizermarketplaces.core.marketplace.repository.ProductImageRepository;
import br.com.rizermarketplaces.core.marketplace.repository.ProductRepository;
import br.com.rizermarketplaces.core.marketplace.service.S3StorageService;
import br.com.rizermarketplaces.core.marketplace.tenant.TenantExceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class ProductImageService {

    private static final Logger log = LoggerFactory.getLogger(ProductImageService.class);

    private final ProductImageRepository imageRepository;
    private final ProductRepository productRepository;
    private final S3StorageService s3StorageService;
    private ProductImageService self;

    public ProductImageService(
        ProductImageRepository imageRepository,
        ProductRepository productRepository,
        S3StorageService s3StorageService
    ) {
        this.imageRepository = imageRepository;
        this.productRepository = productRepository;
        this.s3StorageService = s3StorageService;
    }

    @Autowired
    @Lazy
    public void setSelf(ProductImageService self) {
        this.self = self;
    }

    public UploadResponse upload(
        UUID tenantId, UUID productId, MultipartFile file, Boolean isCover
    ) throws IOException {
        var uploaded = s3StorageService.uploadPublicImage(file, "tenants/" + tenantId + "/products/" + productId);
        AttachImageRequest req = new AttachImageRequest(
            productId, uploaded.key(), uploaded.bucket(), uploaded.url(),
            uploaded.contentType(), null, null,
            (int) imageRepository.countByProductId(productId),
            isCover
        );
        ProductView.ProductImageView view = self.attach(tenantId, req);
        return new UploadResponse(view, uploaded.url(), uploaded.key());
    }

    @Transactional
    public ProductView.ProductImageView attach(UUID tenantId, AttachImageRequest req) {
        productRepository.findByIdAndTenantIdAndDeletedAtIsNull(req.productId(), tenantId)
            .orElseThrow(() -> TenantExceptions.notFound("Produto"));
        ProductImage img = new ProductImage();
        img.setProductId(req.productId());
        img.setS3Key(req.s3Key());
        img.setS3Bucket(req.s3Bucket());
        img.setPublicUrl(req.publicUrl());
        img.setContentType(req.contentType());
        img.setWidth(req.width());
        img.setHeight(req.height());
        img.setSortOrder(req.sortOrder() != null ? req.sortOrder().shortValue() : (short) 0);
        if (Boolean.TRUE.equals(req.isCover())) {
            imageRepository.findByProductIdAndIsCoverTrue(req.productId())
                .ifPresent(existing -> {
                    existing.setCover(false);
                    imageRepository.save(existing);
                });
            img.setCover(true);
        }
        img = imageRepository.save(img);
        return new ProductView.ProductImageView(
            img.getId(), img.getPublicUrl(), img.getContentType(),
            img.getSortOrder(), img.isCover()
        );
    }

    public void delete(UUID tenantId, UUID productId, UUID imageId) {
        ProductImage img = loadImageS3Ref(tenantId, productId, imageId);
        String s3Key = img.getS3Key();
        String s3Bucket = img.getS3Bucket();
        try {
            s3StorageService.deleteFile(s3Key, s3Bucket);
        } catch (Exception e) {
            log.warn("[s3] falha ao remover {} do bucket {} (registro DB será mantido): {}", s3Key, s3Bucket, e.getMessage());
        }
        self.deleteImageRecord(imageId, productId, tenantId);
    }

    @Transactional(readOnly = true)
    public ProductImage loadImageS3Ref(UUID tenantId, UUID productId, UUID imageId) {
        productRepository.findByIdAndTenantIdAndDeletedAtIsNull(productId, tenantId)
            .orElseThrow(() -> TenantExceptions.notFound("Produto"));
        ProductImage img = imageRepository.findById(imageId)
            .orElseThrow(() -> TenantExceptions.notFound("Imagem"));
        if (!img.getProductId().equals(productId)) {
            throw TenantExceptions.notFound("Imagem");
        }
        return img;
    }

    @Transactional
    public void deleteImageRecord(UUID imageId, UUID productId, UUID tenantId) {
        productRepository.findByIdAndTenantIdAndDeletedAtIsNull(productId, tenantId)
            .orElseThrow(() -> TenantExceptions.notFound("Produto"));
        ProductImage img = imageRepository.findById(imageId)
            .orElseThrow(() -> TenantExceptions.notFound("Imagem"));
        if (!img.getProductId().equals(productId)) {
            throw TenantExceptions.notFound("Imagem");
        }
        imageRepository.delete(img);
    }

    @Transactional(readOnly = true)
    public List<ProductView.ProductImageView> list(UUID tenantId, UUID productId) {
        productRepository.findByIdAndTenantIdAndDeletedAtIsNull(productId, tenantId)
            .orElseThrow(() -> TenantExceptions.notFound("Produto"));
        return imageRepository.findAllByProductIdOrderBySortOrderAscCreatedAtAsc(productId)
            .stream().map(i -> new ProductView.ProductImageView(
                i.getId(), i.getPublicUrl(), i.getContentType(), i.getSortOrder(), i.isCover()
            )).toList();
    }
}
