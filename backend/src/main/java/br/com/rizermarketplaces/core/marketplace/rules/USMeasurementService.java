package br.com.rizermarketplaces.core.marketplace.rules;

import br.com.rizermarketplaces.core.marketplace.model.UnitSystem;
import org.springframework.stereotype.Component;

import java.util.Map;

// Implementação de MeasurementService para os EUA (US)
@Component("USMeasurementService")
public class USMeasurementService implements MeasurementService {

    private static final Map<String, String> KEY_MAP = Map.of(
        "quilometragem", "mileage",
        "combustivel", "fuel"
    );

    @Override
    public UnitSystem unitSystem() {
        return UnitSystem.IMPERIAL;
    }

    @Override
    public String normalizeAttributeKey(String attributeKey) {
        if (attributeKey == null) {
            return null;
        }
        // Mapeia chaves portuguesas para o equivalente em inglês/usual nos EUA
        return KEY_MAP.getOrDefault(attributeKey, attributeKey);
    }
}
