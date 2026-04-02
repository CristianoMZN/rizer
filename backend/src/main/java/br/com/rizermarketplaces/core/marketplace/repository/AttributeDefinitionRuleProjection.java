package br.com.rizermarketplaces.core.marketplace.repository;

public interface AttributeDefinitionRuleProjection {

    Long getAttributeDefinitionId();

    String getCode();

    String getDataType();

    boolean getRequired();

    String getValidationRules();
}
