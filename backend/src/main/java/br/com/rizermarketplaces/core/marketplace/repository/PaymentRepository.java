package br.com.rizermarketplaces.core.marketplace.repository;

import br.com.rizermarketplaces.core.marketplace.model.Payment;
import br.com.rizermarketplaces.core.marketplace.model.PaymentMethod;
import br.com.rizermarketplaces.core.marketplace.model.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Page<Payment> findAllByTenantIdOrderByPaidAtDesc(UUID tenantId, Pageable pageable);

    List<Payment> findAllByTenantIdAndStatusOrderByPaidAtDesc(UUID tenantId, PaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.amountCents), 0) FROM Payment p WHERE p.tenantId = :tenantId AND p.status = 'succeeded' AND p.paidAt >= :from AND p.paidAt < :to")
    long sumSucceededByTenantBetween(UUID tenantId, OffsetDateTime from, OffsetDateTime to);

    @Query("SELECT COALESCE(SUM(p.amountCents), 0) FROM Payment p WHERE p.status = 'succeeded' AND p.method IN :methods AND p.paidAt >= :from AND p.paidAt < :to")
    long sumSucceededByMethodsBetween(List<PaymentMethod> methods, OffsetDateTime from, OffsetDateTime to);
}
