package br.com.rizermarketplaces.core.marketplace.tenant;

import br.com.rizermarketplaces.core.marketplace.dto.StoreView;
import br.com.rizermarketplaces.core.marketplace.dto.TenantView;
import br.com.rizermarketplaces.core.marketplace.model.PhysicalStore;
import br.com.rizermarketplaces.core.marketplace.model.Tenant;
import br.com.rizermarketplaces.core.marketplace.model.User;

import java.util.List;
import java.util.UUID;

public final class TenantMapper {

    private TenantMapper() {}

    public static StoreView toStoreView(PhysicalStore s) {
        Double lat = null, lng = null;
        if (s.getLocation() != null) {
            lat = s.getLocation().getY();
            lng = s.getLocation().getX();
        }
        return new StoreView(
            s.getId(), s.getTenantId(), s.getName(), s.getSlug(),
            s.getPhone(), s.getWhatsapp(), s.getEmail(),
            s.isMain(), s.isActive(), lat, lng,
            s.getCreatedAt(), s.getUpdatedAt()
        );
    }

    public static TenantView toView(Tenant t, User owner, long activeStores, long members, List<PhysicalStore> stores) {
        return new TenantView(
            t.getId(), t.getSlug(), t.getCountryCode(), t.getTradeName(),
            t.getLegalName(), t.getCnpj(), t.getDescription(),
            t.getLogoUrl(), t.getBannerUrl(),
            t.getPhone(), t.getWhatsapp(), t.getEmail(), t.getWebsite(),
            t.getStatus(), t.isPublic(), t.isPartnerPageEnabled(), t.isHadTrial(),
            t.getCustomDomain(), t.getCustomDomainStatus(),
            owner != null ? owner.getId().toString() : null,
            owner != null ? owner.getEmail() : null,
            owner != null ? owner.getName() : null,
            activeStores, members,
            stores == null ? List.of() : stores.stream()
                .map(s -> new TenantView.StoreView(s.getId(), s.getName(), s.getSlug(),
                    s.getPhone(), s.getWhatsapp(), s.isMain(), s.isActive()))
                .toList(),
            t.getCreatedAt(), t.getUpdatedAt()
        );
    }
}
