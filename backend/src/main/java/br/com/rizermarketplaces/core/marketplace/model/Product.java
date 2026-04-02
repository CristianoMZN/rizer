package br.com.rizermarketplaces.core.marketplace.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

// Entidade Product que representa anúncios/catalogo de itens.
// Observações:
// - Usa JSONB para armazenar atributos dinâmicos (flexível por realm)
// - Usa ltree no campo category_path para hierarquias de categoria
@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_products_uuid", columnList = "uuid", unique = true),
    @Index(name = "idx_products_realm", columnList = "realm"),
    @Index(name = "idx_products_merchant_id", columnList = "merchant_id")
})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // UUID público usado para exposição externa
    @Column(nullable = false, unique = true)
    private UUID uuid = UUID.randomUUID();

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "seller_id")
    private Long sellerId;

    @Column(name = "subsubcategory_id")
    private Long subsubcategoryId;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ProductStatus status = ProductStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ProductRealm realm;

    @Column(name = "category_path", nullable = false, columnDefinition = "ltree")
    private String categoryPath;

    // Atributos dinâmicos armazenados como JSONB no Postgres. @JdbcTypeCode informa o tipo JDBC usado.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode attributes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(UUID merchantId) {
        this.merchantId = merchantId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public Long getSubsubcategoryId() {
        return subsubcategoryId;
    }

    public void setSubsubcategoryId(Long subsubcategoryId) {
        this.subsubcategoryId = subsubcategoryId;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(UUID createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
    }

    public ProductRealm getRealm() {
        return realm;
    }

    public void setRealm(ProductRealm realm) {
        this.realm = realm;
    }

    public String getCategoryPath() {
        return categoryPath;
    }

    public void setCategoryPath(String categoryPath) {
        this.categoryPath = categoryPath;
    }

    public JsonNode getAttributes() {
        return attributes;
    }

    public void setAttributes(JsonNode attributes) {
        this.attributes = attributes;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
