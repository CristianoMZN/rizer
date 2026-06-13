package br.com.rizermarketplaces.core.marketplace.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

// Schema dinâmico de atributos por contexto: entidade + país + caminho de categoria.
@Entity
@Table(name = "attribute_schemas")
public class AttributeSchema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "category_path", nullable = false, length = 255)
    private String categoryPath;

    @Column(name = "version", nullable = false, length = 50)
    private String version;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "schema_definition", nullable = false, columnDefinition = "jsonb")
    private JsonNode schemaDefinition;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getCategoryPath() {
        return categoryPath;
    }

    public String getVersion() {
        return version;
    }

    public boolean isActive() {
        return active;
    }

    public JsonNode getSchemaDefinition() {
        return schemaDefinition;
    }
}