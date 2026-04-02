package br.com.rizermarketplaces.core.marketplace.rules;

import br.com.rizermarketplaces.core.marketplace.model.UnitSystem;
import org.springframework.stereotype.Component;

import java.util.Map;

// Implementação de MeasurementService para o Brasil (BR)
@Component("BRMeasurementService")
public class BRMeasurementService implements MeasurementService {

    private static final Map<String, String> KEY_MAP = Map.of(
        "mileage", "quilometragem",
        "fuel", "combustivel"
    );

    @Override
    public UnitSystem unitSystem() {
        return UnitSystem.METRIC;
    }

    @Override
    public String normalizeAttributeKey(String attributeKey) {
        if (attributeKey == null) {
            return null;
        }
        // Mapeia chaves em inglês para termos usados localmente em português
        return KEY_MAP.getOrDefault(attributeKey, attributeKey);
    }
}
