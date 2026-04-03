package br.com.rizermarketplaces.core.marketplace.service;

import br.com.rizermarketplaces.core.marketplace.model.AttributeSchema;
import br.com.rizermarketplaces.core.marketplace.repository.AttributeSchemaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
public class DynamicAttributeValidationService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String ENTITY_TYPE_PRODUCT_ATTRIBUTES = "PRODUCT_ATTRIBUTES";
    private static final Pattern CATEGORY_PATH_PATTERN = Pattern.compile("^[a-z0-9_]+(\\.[a-z0-9_]+)*$");

    private final AttributeSchemaRepository attributeSchemaRepository;

    public DynamicAttributeValidationService(AttributeSchemaRepository attributeSchemaRepository) {
        this.attributeSchemaRepository = attributeSchemaRepository;
    }

    // Valida atributos dinâmicos usando JSON Schema persistido em JSONB por país e categoria.
    public void validate(String countryCode, String categoryPath, Map<String, Object> attributes) {
        if (!CATEGORY_PATH_PATTERN.matcher(categoryPath).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "categoryPath must use ltree format");
        }

        String normalizedCountry = countryCode.toUpperCase(Locale.ROOT);

        AttributeSchema schema = attributeSchemaRepository
            .findActiveByContext(ENTITY_TYPE_PRODUCT_ATTRIBUTES, normalizedCountry, categoryPath)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "No active attribute schema configured for country/category"
            ));

        JsonNode schemaNode = schema.getSchemaDefinition();
        if (schemaNode == null || !schemaNode.isObject()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid schema_definition metadata");
        }

        validateObject("attributes", attributes, schemaNode);
    }

    private void validateObject(String path, Map<String, Object> objectValue, JsonNode schemaNode) {
        JsonNode propertiesNode = schemaNode.path("properties");
        if (!propertiesNode.isObject()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Schema properties must be an object");
        }

        boolean additionalProperties = schemaNode.path("additionalProperties").asBoolean(true);

        JsonNode requiredNode = schemaNode.path("required");
        if (requiredNode.isArray()) {
            for (JsonNode requiredProp : requiredNode) {
                String propName = requiredProp.asText();
                if (!objectValue.containsKey(propName) || objectValue.get(propName) == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Required attribute missing: " + joinPath(path, propName));
                }
            }
        }

        for (Map.Entry<String, Object> entry : objectValue.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (!propertiesNode.has(key)) {
                if (!additionalProperties) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attribute not allowed in schema: " + joinPath(path, key));
                }
                continue;
            }

            validateValue(joinPath(path, key), value, propertiesNode.get(key));
        }
    }

    private void validateValue(String path, Object rawValue, JsonNode fieldSchema) {
        if (rawValue == null) {
            return;
        }

        String type = fieldSchema.path("type").asText();
        switch (type) {
            case "string" -> validateString(path, rawValue, fieldSchema);
            case "number" -> validateNumber(path, rawValue, fieldSchema, false);
            case "integer" -> validateNumber(path, rawValue, fieldSchema, true);
            case "boolean" -> {
                if (!(rawValue instanceof Boolean)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attribute must be boolean: " + path);
                }
            }
            case "object" -> {
                if (!(rawValue instanceof Map<?, ?> objectMap)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attribute must be object: " + path);
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> typedMap = (Map<String, Object>) objectMap;
                validateObject(path, typedMap, fieldSchema);
            }
            case "array" -> validateArray(path, rawValue, fieldSchema);
            default -> {
                if (!type.isBlank()) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unsupported schema type: " + type);
                }
            }
        }

        validateEnum(path, rawValue, fieldSchema.path("enum"));
    }

    private void validateString(String path, Object rawValue, JsonNode fieldSchema) {
        if (!(rawValue instanceof String value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attribute must be string: " + path);
        }

        int minLength = readIntRule(fieldSchema, "minLength", 0);
        int maxLength = readIntRule(fieldSchema, "maxLength", Integer.MAX_VALUE);
        if (value.length() < minLength) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attribute below minLength: " + path);
        }
        if (value.length() > maxLength) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attribute exceeds maxLength: " + path);
        }

        String pattern = fieldSchema.path("pattern").asText();
        if (!pattern.isBlank() && !Pattern.compile(pattern).matcher(value).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attribute does not match pattern: " + path);
        }
    }

    private void validateNumber(String path, Object rawValue, JsonNode fieldSchema, boolean integerOnly) {
        BigDecimal value;
        try {
            value = new BigDecimal(String.valueOf(rawValue));
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attribute must be numeric: " + path);
        }

        if (integerOnly && value.stripTrailingZeros().scale() > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attribute must be integer: " + path);
        }

        BigDecimal min = readNumericRule(fieldSchema, "minimum", "min");
        BigDecimal max = readNumericRule(fieldSchema, "maximum", "max");
        if (min != null && value.compareTo(min) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attribute below minimum: " + path);
        }
        if (max != null && value.compareTo(max) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attribute above maximum: " + path);
        }
    }

    private void validateArray(String path, Object rawValue, JsonNode fieldSchema) {
        if (!(rawValue instanceof List<?> values)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attribute must be array: " + path);
        }

        JsonNode itemSchema = fieldSchema.path("items");
        if (itemSchema.isMissingNode()) {
            return;
        }

        for (int i = 0; i < values.size(); i++) {
            validateValue(path + "[" + i + "]", values.get(i), itemSchema);
        }
    }

    private void validateEnum(String path, Object rawValue, JsonNode enumNode) {
        if (!enumNode.isArray()) {
            return;
        }

        JsonNode candidate = OBJECT_MAPPER.valueToTree(rawValue);
        Iterator<JsonNode> allowed = enumNode.elements();
        while (allowed.hasNext()) {
            if (Objects.equals(allowed.next(), candidate)) {
                return;
            }
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attribute value not allowed by enum: " + path);
    }

    private int readIntRule(JsonNode fieldSchema, String key, int defaultValue) {
        JsonNode node = fieldSchema.get(key);
        return node != null && node.isInt() ? node.intValue() : defaultValue;
    }

    private BigDecimal readNumericRule(JsonNode fieldSchema, String primaryKey, String fallbackKey) {
        JsonNode primary = fieldSchema.get(primaryKey);
        if (primary != null && primary.isNumber()) {
            return primary.decimalValue();
        }

        JsonNode fallback = fieldSchema.get(fallbackKey);
        if (fallback != null && fallback.isNumber()) {
            return fallback.decimalValue();
        }

        return null;
    }

    private String joinPath(String base, String child) {
        return base + "." + child;
    }
}
