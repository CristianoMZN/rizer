package br.com.rizermarketplaces.core.marketplace.service;

import br.com.rizermarketplaces.core.marketplace.model.AttributeDataType;
import br.com.rizermarketplaces.core.marketplace.repository.AttributeDefinitionRuleProjection;
import br.com.rizermarketplaces.core.marketplace.repository.AttributeMetadataRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class DynamicAttributeValidationService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AttributeMetadataRepository attributeMetadataRepository;

    public DynamicAttributeValidationService(AttributeMetadataRepository attributeMetadataRepository) {
        this.attributeMetadataRepository = attributeMetadataRepository;
    }

    public List<ResolvedAttributeValue> validate(Long subsubcategoryId, Map<String, Object> attributes) {
        List<AttributeDefinitionRuleProjection> rules = attributeMetadataRepository.findRulesBySubsubcategoryId(subsubcategoryId);
        if (rules.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No attribute metadata configured for category");
        }

        Map<String, AttributeDefinitionRuleProjection> ruleByCode = new LinkedHashMap<>();
        for (AttributeDefinitionRuleProjection rule : rules) {
            ruleByCode.put(rule.getCode(), rule);
        }

        for (String attributeKey : attributes.keySet()) {
            if (!ruleByCode.containsKey(attributeKey)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attribute not allowed for category: " + attributeKey);
            }
        }

        List<ResolvedAttributeValue> resolved = new ArrayList<>();
        for (AttributeDefinitionRuleProjection rule : rules) {
            Object rawValue = attributes.get(rule.getCode());
            if (rawValue == null) {
                if (rule.getRequired()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Required attribute missing: " + rule.getCode());
                }
                continue;
            }

            JsonNode validationRules = parseValidationRules(rule.getValidationRules());
            resolved.add(convertAndValidate(rule, rawValue, validationRules));
        }

        return resolved;
    }

    private JsonNode parseValidationRules(String json) {
        try {
            return json == null || json.isBlank() ? OBJECT_MAPPER.createObjectNode() : OBJECT_MAPPER.readTree(json);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid validation rules metadata");
        }
    }

    private ResolvedAttributeValue convertAndValidate(
        AttributeDefinitionRuleProjection rule,
        Object rawValue,
        JsonNode validationRules
    ) {
        AttributeDataType dataType;
        try {
            dataType = AttributeDataType.valueOf(rule.getDataType());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unsupported attribute data type: " + rule.getDataType());
        }

        return switch (dataType) {
            case STRING -> {
                String value = String.valueOf(rawValue);
                int maxLength = validationRules.path("maxLength").asInt(Integer.MAX_VALUE);
                if (value.length() > maxLength) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attribute exceeds maxLength: " + rule.getCode());
                }
                yield ResolvedAttributeValue.text(rule.getAttributeDefinitionId(), rule.getCode(), value);
            }
            case NUMBER -> {
                BigDecimal value;
                try {
                    value = new BigDecimal(String.valueOf(rawValue));
                } catch (NumberFormatException ex) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attribute must be numeric: " + rule.getCode());
                }

                if (validationRules.has("min") && value.compareTo(validationRules.path("min").decimalValue()) < 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attribute below min: " + rule.getCode());
                }
                if (validationRules.has("max") && value.compareTo(validationRules.path("max").decimalValue()) > 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attribute above max: " + rule.getCode());
                }
                yield ResolvedAttributeValue.number(rule.getAttributeDefinitionId(), rule.getCode(), value);
            }
            case BOOLEAN -> {
                if (!(rawValue instanceof Boolean boolValue)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attribute must be boolean: " + rule.getCode());
                }
                yield ResolvedAttributeValue.bool(rule.getAttributeDefinitionId(), rule.getCode(), boolValue);
            }
            case DATE -> {
                try {
                    LocalDate date = LocalDate.parse(String.valueOf(rawValue));
                    yield ResolvedAttributeValue.date(rule.getAttributeDefinitionId(), rule.getCode(), date);
                } catch (DateTimeParseException ex) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attribute must be ISO date (yyyy-MM-dd): " + rule.getCode());
                }
            }
            case JSON -> {
                String value;
                try {
                    value = OBJECT_MAPPER.writeValueAsString(rawValue);
                } catch (Exception ex) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attribute JSON value is invalid: " + rule.getCode());
                }
                yield ResolvedAttributeValue.json(rule.getAttributeDefinitionId(), rule.getCode(), value);
            }
        };
    }

    public record ResolvedAttributeValue(
        Long attributeDefinitionId,
        String code,
        String textValue,
        BigDecimal numberValue,
        Boolean booleanValue,
        LocalDate dateValue,
        String jsonValue
    ) {
        public static ResolvedAttributeValue text(Long id, String code, String value) {
            return new ResolvedAttributeValue(id, code, value, null, null, null, null);
        }

        public static ResolvedAttributeValue number(Long id, String code, BigDecimal value) {
            return new ResolvedAttributeValue(id, code, null, value, null, null, null);
        }

        public static ResolvedAttributeValue bool(Long id, String code, Boolean value) {
            return new ResolvedAttributeValue(id, code, null, null, value, null, null);
        }

        public static ResolvedAttributeValue date(Long id, String code, LocalDate value) {
            return new ResolvedAttributeValue(id, code, null, null, null, value, null);
        }

        public static ResolvedAttributeValue json(Long id, String code, String value) {
            return new ResolvedAttributeValue(id, code, null, null, null, null, value);
        }
    }
}
