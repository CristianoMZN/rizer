package br.com.rizermarketplaces.core.marketplace.model;

public enum PaymentStatus {
    pending,
    succeeded,
    failed,
    refunded,
    voided,
    chargeback
}
