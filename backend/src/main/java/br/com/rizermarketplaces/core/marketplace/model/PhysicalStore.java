package br.com.rizermarketplaces.core.marketplace.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "physical_stores")
public class PhysicalStore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "address_id")
    private UUID addressId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 120)
    private String slug;

    @Column(length = 32)
    private String phone;

    @Column(length = 32)
    private String whatsapp;

    @Column(length = 255)
    private String email;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "opening_hours", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> openingHours = new HashMap<>();

    @Column(name = "is_main", nullable = false)
    private boolean isMain = false;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(columnDefinition = "geography(Point, 4326)")
    private Point location;

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
    public void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getAddressId() { return addressId; }
    public void setAddressId(UUID addressId) { this.addressId = addressId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getWhatsapp() { return whatsapp; }
    public void setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Map<String, Object> getOpeningHours() { return openingHours; }
    public void setOpeningHours(Map<String, Object> openingHours) { this.openingHours = openingHours; }
    public boolean isMain() { return isMain; }
    public void setMain(boolean main) { isMain = main; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public Point getLocation() { return location; }
    public void setLocation(Point location) { this.location = location; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(UUID createdByUserId) { this.createdByUserId = createdByUserId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(OffsetDateTime deletedAt) { this.deletedAt = deletedAt; }
}
