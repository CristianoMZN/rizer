package br.com.rizermarketplaces.core.marketplace.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "plans")
public class Plan {

    @Id
    @Column(length = 40)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "max_physical_stores")
    private Integer maxPhysicalStores;

    @Column(name = "has_partner_page", nullable = false)
    private boolean hasPartnerPage = false;

    @Column(name = "has_custom_domain", nullable = false)
    private boolean hasCustomDomain = false;

    @Column(name = "has_instagram", nullable = false)
    private boolean hasInstagram = false;

    @Column(name = "has_meta_dpa", nullable = false)
    private boolean hasMetaDpa = false;

    @Column(name = "has_google_shopping", nullable = false)
    private boolean hasGoogleShopping = false;

    @Column(name = "price_cents", nullable = false)
    private long priceCents;

    @Column(nullable = false, length = 3)
    private String currency = "BRL";

    @Column(name = "trial_days", nullable = false)
    private int trialDays = 0;

    @Column(name = "stripe_price_id", length = 120)
    private String stripePriceId;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PreUpdate
    public void onUpdate() { this.updatedAt = OffsetDateTime.now(); }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getMaxPhysicalStores() { return maxPhysicalStores; }
    public void setMaxPhysicalStores(Integer maxPhysicalStores) { this.maxPhysicalStores = maxPhysicalStores; }
    public boolean isHasPartnerPage() { return hasPartnerPage; }
    public void setHasPartnerPage(boolean hasPartnerPage) { this.hasPartnerPage = hasPartnerPage; }
    public boolean isHasCustomDomain() { return hasCustomDomain; }
    public void setHasCustomDomain(boolean hasCustomDomain) { this.hasCustomDomain = hasCustomDomain; }
    public boolean isHasInstagram() { return hasInstagram; }
    public void setHasInstagram(boolean hasInstagram) { this.hasInstagram = hasInstagram; }
    public boolean isHasMetaDpa() { return hasMetaDpa; }
    public void setHasMetaDpa(boolean hasMetaDpa) { this.hasMetaDpa = hasMetaDpa; }
    public boolean isHasGoogleShopping() { return hasGoogleShopping; }
    public void setHasGoogleShopping(boolean hasGoogleShopping) { this.hasGoogleShopping = hasGoogleShopping; }
    public long getPriceCents() { return priceCents; }
    public void setPriceCents(long priceCents) { this.priceCents = priceCents; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public int getTrialDays() { return trialDays; }
    public void setTrialDays(int trialDays) { this.trialDays = trialDays; }
    public String getStripePriceId() { return stripePriceId; }
    public void setStripePriceId(String stripePriceId) { this.stripePriceId = stripePriceId; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
