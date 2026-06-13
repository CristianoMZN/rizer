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

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "attribute_schemas")
public class AttributeSchema {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(length = 40)
    private String realm;

    @Column(name = "category_path", nullable = false, columnDefinition = "ltree")
    private String categoryPath;

    @Column(name = "entity_type", nullable = false, length = 40)
    private String entityType = "product";

    @Column(nullable = false)
    private int version = 1;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "schema_definition", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> schemaDefinition = new HashMap<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() { this.updatedAt = OffsetDateTime.now(); }

    public UUID getId() { return id; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    public String getRealm() { return realm; }
    public void setRealm(String realm) { this.realm = realm; }
    public String getCategoryPath() { return categoryPath; }
    public void setCategoryPath(String categoryPath) { this.categoryPath = categoryPath; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public Map<String, Object> getSchemaDefinition() { return schemaDefinition; }
    public void setSchemaDefinition(Map<String, Object> schemaDefinition) { this.schemaDefinition = schemaDefinition; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
