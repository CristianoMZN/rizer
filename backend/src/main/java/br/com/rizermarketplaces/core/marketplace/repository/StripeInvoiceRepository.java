package br.com.rizermarketplaces.core.marketplace.repository;

import br.com.rizermarketplaces.core.marketplace.model.StripeInvoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StripeInvoiceRepository extends JpaRepository<StripeInvoice, UUID> {

    Optional<StripeInvoice> findByStripeInvoiceId(String stripeInvoiceId);
}
