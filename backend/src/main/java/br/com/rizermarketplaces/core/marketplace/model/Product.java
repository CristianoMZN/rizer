package br.com.rizermarketplaces.core.marketplace.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "physical_store_id", nullable = false)
    private UUID physicalStoreId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "brand_id")
    private Integer brandId;

    @Column(name = "model_id")
    private Integer modelId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VehicleRealm realm;

    @Column(name = "year_model")
    private Short yearModel;

    @Column(name = "year_build")
    private Short yearBuild;

    @Column(name = "mileage_km")
    private Integer mileageKm;

    @Column(length = 40)
    private String fuel;

    @Column(length = 40)
    private String transmission;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> attributes = new HashMap<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status = ProductStatus.DRAFT;

    @Column(name = "posted_to_instagram_at")
    private OffsetDateTime postedToInstagramAt;

    @Column(name = "instagram_media_id", length = 80)
    private String instagramMediaId;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(columnDefinition = "geography(Point, 4326)")
    private Point location;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_source", nullable = false, length = 20)
    private ProductLocationSource locationSource = ProductLocationSource.STORE;

    @Column(name = "seller_user_id")
    private UUID sellerUserId;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @PrePersist
    public void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() { this.updatedAt = OffsetDateTime.now(); }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getPhysicalStoreId() { return physicalStoreId; }
    public void setPhysicalStoreId(UUID physicalStoreId) { this.physicalStoreId = physicalStoreId; }
    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }
    public Integer getBrandId() { return brandId; }
    public void setBrandId(Integer brandId) { this.brandId = brandId; }
    public Integer getModelId() { return modelId; }
    public void setModelId(Integer modelId) { this.modelId = modelId; }
    public VehicleRealm getRealm() { return realm; }
    public void setRealm(VehicleRealm realm) { this.realm = realm; }
    public Short getYearModel() { return yearModel; }
    public void setYearModel(Short yearModel) { this.yearModel = yearModel; }
    public Short getYearBuild() { return yearBuild; }
    public void setYearBuild(Short yearBuild) { this.yearBuild = yearBuild; }
    public Integer getMileageKm() { return mileageKm; }
    public void setMileageKm(Integer mileageKm) { this.mileageKm = mileageKm; }
    public String getFuel() { return fuel; }
    public void setFuel(String fuel) { this.fuel = fuel; }
    public String getTransmission() { return transmission; }
    public void setTransmission(String transmission) { this.transmission = transmission; }
    public Map<String, Object> getAttributes() { return attributes; }
    public void setAttributes(Map<String, Object> attributes) { this.attributes = attributes; }
    public ProductStatus getStatus() { return status; }
    public void setStatus(ProductStatus status) { this.status = status; }
    public OffsetDateTime getPostedToInstagramAt() { return postedToInstagramAt; }
    public void setPostedToInstagramAt(OffsetDateTime postedToInstagramAt) { this.postedToInstagramAt = postedToInstagramAt; }
    public String getInstagramMediaId() { return instagramMediaId; }
    public void setInstagramMediaId(String instagramMediaId) { this.instagramMediaId = instagramMediaId; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(UUID createdByUserId) { this.createdByUserId = createdByUserId; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Point getLocation() { return location; }
    public void setLocation(Point location) { this.location = location; }

    public ProductLocationSource getLocationSource() { return locationSource; }
    public void setLocationSource(ProductLocationSource locationSource) { this.locationSource = locationSource; }

    public UUID getSellerUserId() { return sellerUserId; }
    public void setSellerUserId(UUID sellerUserId) { this.sellerUserId = sellerUserId; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(OffsetDateTime deletedAt) { this.deletedAt = deletedAt; }
}
