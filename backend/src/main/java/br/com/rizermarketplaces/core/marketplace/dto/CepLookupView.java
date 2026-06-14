package br.com.rizermarketplaces.core.marketplace.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CepLookupView(
    String cep,
    String street,
    String complement,
    String neighborhood,
    String city,
    String state,
    String ibge,
    String ddd,
    Double latitude,
    Double longitude
) {}
