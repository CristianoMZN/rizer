package br.com.rizermarketplaces.core.marketplace.model;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "seller_users", uniqueConstraints = {
    @UniqueConstraint(name = "uk_seller_users_seller_user", columnNames = {"seller_id", "user_id"})
})
public class SellerUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SellerUserRole role;

    @Column(name = "is_owner", nullable = false)
    private boolean isOwner;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
