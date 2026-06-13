package br.com.rizermarketplaces.core.marketplace.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "countries")
public class Country {

    @Id
    @Column(length = 2)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "local_name", nullable = false, length = 120)
    private String localName;

    @Column(name = "iso_alpha_3", nullable = false, length = 3)
    private String isoAlpha3;

    @Column(name = "numeric_code", nullable = false, length = 3)
    private String numericCode;

    @Column(name = "currency_code_iso", nullable = false, length = 3)
    private String currencyCodeIso;

    @Column(name = "currency_name", nullable = false, length = 80)
    private String currencyName;

    @Column(name = "currency_symbol", nullable = false, length = 8)
    private String currencySymbol;

    @Column(name = "currency_symbol_position", nullable = false, length = 8)
    private String currencySymbolPosition = "start";

    @Column(name = "currency_minor_unit", nullable = false)
    private short currencyMinorUnit = 2;

    @Column(name = "timezone_default", nullable = false, length = 64)
    private String timezoneDefault;

    @Column(name = "language_default", nullable = false, length = 8)
    private String languageDefault;

    @Column(name = "locale_default", nullable = false, length = 16)
    private String localeDefault;

    @Column(name = "default_phone_code", nullable = false, length = 8)
    private String defaultPhoneCode;

    @Column(name = "date_format_default", nullable = false, length = 32)
    private String dateFormatDefault = "dd/MM/yyyy";

    @Column(name = "postal_code_required", nullable = false)
    private boolean postalCodeRequired = true;

    @Column(name = "tax_identifier_label", nullable = false, length = 40)
    private String taxIdentifierLabel;

    @Column(name = "address_format", nullable = false, length = 32)
    private String addressFormat = "national";

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLocalName() { return localName; }
    public void setLocalName(String localName) { this.localName = localName; }

    public String getIsoAlpha3() { return isoAlpha3; }
    public void setIsoAlpha3(String isoAlpha3) { this.isoAlpha3 = isoAlpha3; }

    public String getNumericCode() { return numericCode; }
    public void setNumericCode(String numericCode) { this.numericCode = numericCode; }

    public String getCurrencyCodeIso() { return currencyCodeIso; }
    public void setCurrencyCodeIso(String currencyCodeIso) { this.currencyCodeIso = currencyCodeIso; }

    public String getCurrencyName() { return currencyName; }
    public void setCurrencyName(String currencyName) { this.currencyName = currencyName; }

    public String getCurrencySymbol() { return currencySymbol; }
    public void setCurrencySymbol(String currencySymbol) { this.currencySymbol = currencySymbol; }

    public String getCurrencySymbolPosition() { return currencySymbolPosition; }
    public void setCurrencySymbolPosition(String currencySymbolPosition) { this.currencySymbolPosition = currencySymbolPosition; }

    public short getCurrencyMinorUnit() { return currencyMinorUnit; }
    public void setCurrencyMinorUnit(short currencyMinorUnit) { this.currencyMinorUnit = currencyMinorUnit; }

    public String getTimezoneDefault() { return timezoneDefault; }
    public void setTimezoneDefault(String timezoneDefault) { this.timezoneDefault = timezoneDefault; }

    public String getLanguageDefault() { return languageDefault; }
    public void setLanguageDefault(String languageDefault) { this.languageDefault = languageDefault; }

    public String getLocaleDefault() { return localeDefault; }
    public void setLocaleDefault(String localeDefault) { this.localeDefault = localeDefault; }

    public String getDefaultPhoneCode() { return defaultPhoneCode; }
    public void setDefaultPhoneCode(String defaultPhoneCode) { this.defaultPhoneCode = defaultPhoneCode; }

    public String getDateFormatDefault() { return dateFormatDefault; }
    public void setDateFormatDefault(String dateFormatDefault) { this.dateFormatDefault = dateFormatDefault; }

    public boolean isPostalCodeRequired() { return postalCodeRequired; }
    public void setPostalCodeRequired(boolean postalCodeRequired) { this.postalCodeRequired = postalCodeRequired; }

    public String getTaxIdentifierLabel() { return taxIdentifierLabel; }
    public void setTaxIdentifierLabel(String taxIdentifierLabel) { this.taxIdentifierLabel = taxIdentifierLabel; }

    public String getAddressFormat() { return addressFormat; }
    public void setAddressFormat(String addressFormat) { this.addressFormat = addressFormat; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
