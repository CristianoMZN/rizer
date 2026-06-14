package br.com.rizermarketplaces.core.marketplace.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class FavoriteView {

    private final UUID id;
    private final UUID productId;
    private final OffsetDateTime createdAt;
    private final PublicProductView product;

    public FavoriteView(UUID id, UUID productId, OffsetDateTime createdAt, PublicProductView product) {
        this.id = id;
        this.productId = productId;
        this.createdAt = createdAt;
        this.product = product;
    }

    public UUID getId() { return id; }
    public UUID getProductId() { return productId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public PublicProductView getProduct() { return product; }

    public static class IdList {
        private final List<UUID> ids;
        public IdList(List<UUID> ids) { this.ids = ids; }
        public List<UUID> getIds() { return ids; }
    }
}
