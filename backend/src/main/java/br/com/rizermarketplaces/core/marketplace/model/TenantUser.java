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

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenant_users")
public class TenantUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TenantUserRole role = TenantUserRole.SELLER;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "physical_store_ids", columnDefinition = "uuid[]")
    private UUID[] physicalStoreIds = new UUID[0];

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "invited_by_user_id")
    private UUID invitedByUserId;

    @Column(name = "invited_at")
    private OffsetDateTime invitedAt;

    @Column(name = "accepted_at")
    private OffsetDateTime acceptedAt;

    @Column(name = "expire_at")
    private OffsetDateTime expireAt;

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
    public void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public TenantUserRole getRole() { return role; }
    public void setRole(TenantUserRole role) { this.role = role; }
    public UUID[] getPhysicalStoreIds() { return physicalStoreIds; }
    public void setPhysicalStoreIds(UUID[] physicalStoreIds) { this.physicalStoreIds = physicalStoreIds; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public UUID getInvitedByUserId() { return invitedByUserId; }
    public void setInvitedByUserId(UUID invitedByUserId) { this.invitedByUserId = invitedByUserId; }
    public OffsetDateTime getInvitedAt() { return invitedAt; }
    public void setInvitedAt(OffsetDateTime invitedAt) { this.invitedAt = invitedAt; }
    public OffsetDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(OffsetDateTime acceptedAt) { this.acceptedAt = acceptedAt; }
    public OffsetDateTime getExpireAt() { return expireAt; }
    public void setExpireAt(OffsetDateTime expireAt) { this.expireAt = expireAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
