package br.com.rizermarketplaces.core.marketplace.rules;

import br.com.rizermarketplaces.core.marketplace.model.UnitSystem;

// Interface para regras de medidas por país (ex: sistema métrico no BR, imperial nos EUA)
public interface MeasurementService {

    // Retorna o sistema de unidade padrão para o país
    UnitSystem unitSystem();

    // Normaliza chaves de atributo (ex: 'km' -> 'quilometragem') para manter consistência no catálogo
    String normalizeAttributeKey(String attributeKey);
}
