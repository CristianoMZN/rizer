package br.com.rizermarketplaces.core.marketplace.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "stripe_invoices")
public class StripeInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "stripe_invoice_id", nullable = false, length = 120, unique = true)
    private String stripeInvoiceId;

    @Column(name = "stripe_charge_id", length = 120)
    private String stripeChargeId;

    @Column(name = "hosted_invoice_url", columnDefinition = "text")
    private String hostedInvoiceUrl;

    @Column(name = "invoice_pdf", columnDefinition = "text")
    private String invoicePdf;

    @Column(name = "amount_due_cents")
    private Long amountDueCents;

    @Column(name = "amount_paid_cents")
    private Long amountPaidCents;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", columnDefinition = "jsonb")
    private Map<String, Object> rawPayload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }
    public String getStripeInvoiceId() { return stripeInvoiceId; }
    public void setStripeInvoiceId(String stripeInvoiceId) { this.stripeInvoiceId = stripeInvoiceId; }
    public String getStripeChargeId() { return stripeChargeId; }
    public void setStripeChargeId(String stripeChargeId) { this.stripeChargeId = stripeChargeId; }
    public String getHostedInvoiceUrl() { return hostedInvoiceUrl; }
    public void setHostedInvoiceUrl(String hostedInvoiceUrl) { this.hostedInvoiceUrl = hostedInvoiceUrl; }
    public String getInvoicePdf() { return invoicePdf; }
    public void setInvoicePdf(String invoicePdf) { this.invoicePdf = invoicePdf; }
    public Long getAmountDueCents() { return amountDueCents; }
    public void setAmountDueCents(Long amountDueCents) { this.amountDueCents = amountDueCents; }
    public Long getAmountPaidCents() { return amountPaidCents; }
    public void setAmountPaidCents(Long amountPaidCents) { this.amountPaidCents = amountPaidCents; }
    public Map<String, Object> getRawPayload() { return rawPayload; }
    public void setRawPayload(Map<String, Object> rawPayload) { this.rawPayload = rawPayload; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
