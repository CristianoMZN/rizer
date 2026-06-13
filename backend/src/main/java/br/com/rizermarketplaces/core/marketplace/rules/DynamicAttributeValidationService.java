package br.com.rizermarketplaces.core.marketplace.rules;

import br.com.rizermarketplaces.core.marketplace.model.AttributeSchema;
import br.com.rizermarketplaces.core.marketplace.repository.AttributeSchemaRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Validador leve de JSON Schema (subset do draft-07). Cobre o que o
 * schema de veículos usa: type, required, enum, minimum, maximum,
 * minLength, maxLength, pattern, items, maxItems, additionalProperties.
 *
 * Não implementa $ref, oneOf, allOf, anyOf (não necessários para a Fase 3).
 */
@Service
public class DynamicAttributeValidationService {

    private final AttributeSchemaRepository repository;

    public DynamicAttributeValidationService(AttributeSchemaRepository repository) {
        this.repository = repository;
    }

    public Optional<AttributeSchema> findSchema(
        String countryCode, String realm, String categoryPath
    ) {
        return repository.findActiveFor(countryCode, "product", realm, categoryPath);
    }

    public List<String> validate(AttributeSchema schema, Map<String, Object> input) {
        List<String> errors = new ArrayList<>();
        Map<String, Object> def = schema.getSchemaDefinition();
        validateObject(def, input, "", errors);
        return errors;
    }

    @SuppressWarnings("unchecked")
    private void validateObject(Map<String, Object> schema, Map<String, Object> obj, String path, List<String> errors) {
        if (obj == null) {
            if (Boolean.TRUE.equals(schema.get("required")) && !path.isEmpty()) {
                errors.add("Campo obrigatório ausente: " + path);
            }
            return;
        }
        List<String> required = (List<String>) schema.get("required");
        if (required != null) {
            for (String key : required) {
                if (!obj.containsKey(key) || obj.get(key) == null) {
                    errors.add((path.isEmpty() ? key : path + "." + key) + " é obrigatório");
                }
            }
        }
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        if (properties != null) {
            for (var entry : properties.entrySet()) {
                String key = entry.getKey();
                Object value = obj.get(key);
                if (value == null) continue;
                String childPath = path.isEmpty() ? key : path + "." + key;
                Map<String, Object> propSchema = (Map<String, Object>) entry.getValue();
                validateValue(propSchema, value, childPath, errors);
            }
        }
        Boolean additional = (Boolean) schema.get("additionalProperties");
        if (Boolean.FALSE.equals(additional) && properties != null) {
            for (String key : obj.keySet()) {
                if (!properties.containsKey(key)) {
                    errors.add((path.isEmpty() ? key : path + "." + key) + " não é permitido");
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void validateValue(Map<String, Object> schema, Object value, String path, List<String> errors) {
        String type = (String) schema.get("type");
        if (type != null && !typeMatches(type, value)) {
            errors.add(path + " deve ser do tipo " + type);
            return;
        }
        if ("integer".equals(type) || "number".equals(type)) {
            double n = ((Number) value).doubleValue();
            if (schema.containsKey("minimum") && n < ((Number) schema.get("minimum")).doubleValue()) {
                errors.add(path + " deve ser >= " + schema.get("minimum"));
            }
            if (schema.containsKey("maximum") && n > ((Number) schema.get("maximum")).doubleValue()) {
                errors.add(path + " deve ser <= " + schema.get("maximum"));
            }
        }
        if ("string".equals(type) && value instanceof String s) {
            if (schema.containsKey("minLength") && s.length() < (int) schema.get("minLength")) {
                errors.add(path + " muito curto (mínimo " + schema.get("minLength") + ")");
            }
            if (schema.containsKey("maxLength") && s.length() > (int) schema.get("maxLength")) {
                errors.add(path + " muito longo (máximo " + schema.get("maxLength") + ")");
            }
            Object pat = schema.get("pattern");
            if (pat != null && !s.matches((String) pat)) {
                errors.add(path + " não bate com o formato esperado");
            }
            List<String> en = (List<String>) schema.get("enum");
            if (en != null && !en.contains(s)) {
                errors.add(path + " deve ser um de " + en);
            }
        }
        if ("array".equals(type) && value instanceof List<?> list) {
            Integer max = (Integer) schema.get("maxItems");
            if (max != null && list.size() > max) {
                errors.add(path + " excede o tamanho máximo (" + max + ")");
            }
            Map<String, Object> items = (Map<String, Object>) schema.get("items");
            if (items != null) {
                for (int i = 0; i < list.size(); i++) {
                    validateValue(items, list.get(i), path + "[" + i + "]", errors);
                }
            }
        }
    }

    private boolean typeMatches(String type, Object value) {
        return switch (type) {
            case "string" -> value instanceof String;
            case "integer" -> value instanceof Number n && n.doubleValue() == Math.floor(n.doubleValue());
            case "number" -> value instanceof Number;
            case "boolean" -> value instanceof Boolean;
            case "array" -> value instanceof List;
            case "object" -> value instanceof Map;
            default -> true;
        };
    }
}
