package br.com.rizermarketplaces.core.marketplace.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "product_images")
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "s3_key", nullable = false, length = 512)
    private String s3Key;

    @Column(name = "s3_bucket", nullable = false, length = 120)
    private String s3Bucket;

    @Column(name = "public_url", nullable = false, length = 1024)
    private String publicUrl;

    @Column(name = "content_type", length = 60)
    private String contentType;

    private Integer width;
    private Integer height;

    @Column(name = "sort_order", nullable = false)
    private short sortOrder = 0;

    @Column(name = "is_cover", nullable = false)
    private boolean isCover = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public String getS3Key() { return s3Key; }
    public void setS3Key(String s3Key) { this.s3Key = s3Key; }
    public String getS3Bucket() { return s3Bucket; }
    public void setS3Bucket(String s3Bucket) { this.s3Bucket = s3Bucket; }
    public String getPublicUrl() { return publicUrl; }
    public void setPublicUrl(String publicUrl) { this.publicUrl = publicUrl; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }
    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }
    public short getSortOrder() { return sortOrder; }
    public void setSortOrder(short sortOrder) { this.sortOrder = sortOrder; }
    public boolean isCover() { return isCover; }
    public void setCover(boolean cover) { isCover = cover; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
