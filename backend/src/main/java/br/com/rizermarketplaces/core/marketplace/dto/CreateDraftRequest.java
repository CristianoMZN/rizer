package br.com.rizermarketplaces.core.marketplace.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Payload mínimo para criar um rascunho de anúncio ao clicar "Novo anúncio".
 * Não exige título, categoria, preço, etc.
 */
public record CreateDraftRequest(
    @NotNull UUID physicalStoreId
) {}
