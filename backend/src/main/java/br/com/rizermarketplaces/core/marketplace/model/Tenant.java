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
@Table(name = "tenants")
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(nullable = false, length = 80)
    private String slug;

    @Column(length = 20)
    private String cnpj;

    @Column(name = "legal_name", length = 255)
    private String legalName;

    @Column(name = "trade_name", nullable = false, length = 255)
    private String tradeName;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "logo_url", length = 512)
    private String logoUrl;

    @Column(name = "banner_url", length = 512)
    private String bannerUrl;

    @Column(length = 32)
    private String phone;

    @Column(length = 32)
    private String whatsapp;

    @Column(length = 255)
    private String email;

    @Column(length = 255)
    private String website;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> theme = new HashMap<>();

    @Column(name = "custom_domain", length = 255)
    private String customDomain;

    @Enumerated(EnumType.STRING)
    @Column(name = "custom_domain_status", nullable = false, length = 20)
    private CustomDomainStatus customDomainStatus = CustomDomainStatus.NONE;

    @Column(name = "custom_domain_last_check_at")
    private OffsetDateTime customDomainLastCheckAt;

    @Column(name = "custom_domain_error", columnDefinition = "text")
    private String customDomainError;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TenantStatus status = TenantStatus.pending;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic = false;

    @Column(name = "is_partner_page_enabled", nullable = false)
    private boolean isPartnerPageEnabled = false;

    @Column(name = "had_trial", nullable = false)
    private boolean hadTrial = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> attributes = new HashMap<>();

    @Column(name = "partner_owner_name", length = 255)
    private String partnerOwnerName;

    @Column(name = "partner_owner_cpf", length = 14)
    private String partnerOwnerCpf;

    @Column(name = "admin_phone", length = 32)
    private String adminPhone;

    @Column(name = "address_zip_code", length = 16)
    private String addressZipCode;

    @Column(name = "address_street", length = 255)
    private String addressStreet;

    @Column(name = "address_number", length = 32)
    private String addressNumber;

    @Column(name = "address_complement", length = 120)
    private String addressComplement;

    @Column(name = "address_neighborhood", length = 120)
    private String addressNeighborhood;

    @Column(name = "address_city", length = 120)
    private String addressCity;

    @Column(name = "address_state", length = 80)
    private String addressState;

    @Column(name = "address_location", columnDefinition = "geography(Point, 4326)")
    private Point addressLocation;

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

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }

    public String getLegalName() { return legalName; }
    public void setLegalName(String legalName) { this.legalName = legalName; }

    public String getTradeName() { return tradeName; }
    public void setTradeName(String tradeName) { this.tradeName = tradeName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public String getBannerUrl() { return bannerUrl; }
    public void setBannerUrl(String bannerUrl) { this.bannerUrl = bannerUrl; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getWhatsapp() { return whatsapp; }
    public void setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public Map<String, Object> getTheme() { return theme; }
    public void setTheme(Map<String, Object> theme) { this.theme = theme; }

    public String getCustomDomain() { return customDomain; }
    public void setCustomDomain(String customDomain) { this.customDomain = customDomain; }

    public CustomDomainStatus getCustomDomainStatus() { return customDomainStatus; }
    public void setCustomDomainStatus(CustomDomainStatus customDomainStatus) { this.customDomainStatus = customDomainStatus; }

    public OffsetDateTime getCustomDomainLastCheckAt() { return customDomainLastCheckAt; }
    public void setCustomDomainLastCheckAt(OffsetDateTime customDomainLastCheckAt) { this.customDomainLastCheckAt = customDomainLastCheckAt; }

    public String getCustomDomainError() { return customDomainError; }
    public void setCustomDomainError(String customDomainError) { this.customDomainError = customDomainError; }

    public TenantStatus getStatus() { return status; }
    public void setStatus(TenantStatus status) { this.status = status; }

    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean aPublic) { isPublic = aPublic; }

    public boolean isPartnerPageEnabled() { return isPartnerPageEnabled; }
    public void setPartnerPageEnabled(boolean partnerPageEnabled) { isPartnerPageEnabled = partnerPageEnabled; }

    public boolean isHadTrial() { return hadTrial; }
    public void setHadTrial(boolean hadTrial) { this.hadTrial = hadTrial; }

    public Map<String, Object> getAttributes() { return attributes; }
    public void setAttributes(Map<String, Object> attributes) { this.attributes = attributes; }

    public String getPartnerOwnerName() { return partnerOwnerName; }
    public void setPartnerOwnerName(String partnerOwnerName) { this.partnerOwnerName = partnerOwnerName; }

    public String getPartnerOwnerCpf() { return partnerOwnerCpf; }
    public void setPartnerOwnerCpf(String partnerOwnerCpf) { this.partnerOwnerCpf = partnerOwnerCpf; }

    public String getAdminPhone() { return adminPhone; }
    public void setAdminPhone(String adminPhone) { this.adminPhone = adminPhone; }

    public String getAddressZipCode() { return addressZipCode; }
    public void setAddressZipCode(String addressZipCode) { this.addressZipCode = addressZipCode; }

    public String getAddressStreet() { return addressStreet; }
    public void setAddressStreet(String addressStreet) { this.addressStreet = addressStreet; }

    public String getAddressNumber() { return addressNumber; }
    public void setAddressNumber(String addressNumber) { this.addressNumber = addressNumber; }

    public String getAddressComplement() { return addressComplement; }
    public void setAddressComplement(String addressComplement) { this.addressComplement = addressComplement; }

    public String getAddressNeighborhood() { return addressNeighborhood; }
    public void setAddressNeighborhood(String addressNeighborhood) { this.addressNeighborhood = addressNeighborhood; }

    public String getAddressCity() { return addressCity; }
    public void setAddressCity(String addressCity) { this.addressCity = addressCity; }

    public String getAddressState() { return addressState; }
    public void setAddressState(String addressState) { this.addressState = addressState; }

    public Point getAddressLocation() { return addressLocation; }
    public void setAddressLocation(Point addressLocation) { this.addressLocation = addressLocation; }

    public UUID getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(UUID createdByUserId) { this.createdByUserId = createdByUserId; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(OffsetDateTime deletedAt) { this.deletedAt = deletedAt; }
}
