package br.com.rizermarketplaces.core.marketplace.model;

public enum PaymentMethod {
    stripe_card,
    stripe_pix,
    stripe_boleto,
    manual_cash,
    manual_bank_transfer,
    manual_pix_external,
    manual_bonus,
    manual_courtesy,
    manual_other
}
