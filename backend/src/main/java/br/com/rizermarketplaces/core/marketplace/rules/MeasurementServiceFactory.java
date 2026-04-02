package br.com.rizermarketplaces.core.marketplace.rules;

import br.com.rizermarketplaces.core.marketplace.context.RegionContextHolder;
import org.springframework.stereotype.Component;

// Fábrica simples para escolher a implementação de MeasurementService baseada no countryCode
@Component
public class MeasurementServiceFactory {

    private final BRMeasurementService brMeasurementService;
    private final USMeasurementService usMeasurementService;

    public MeasurementServiceFactory(BRMeasurementService brMeasurementService, USMeasurementService usMeasurementService) {
        this.brMeasurementService = brMeasurementService;
        this.usMeasurementService = usMeasurementService;
    }

    // Pega o serviço com base no contexto de ThreadLocal (filtro RegionContextFilter)
    public MeasurementService getForCurrentContext() {
        return getForCountry(RegionContextHolder.getCountryCode());
    }

    // Seleciona a implementação com base no código do país (fallback para BR)
    public MeasurementService getForCountry(String countryCode) {
        if (countryCode == null) {
            return brMeasurementService;
        }

        return switch (countryCode.toUpperCase()) {
            case "US" -> usMeasurementService;
            case "BR" -> brMeasurementService;
            default -> brMeasurementService;
        };
    }
}
