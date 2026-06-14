package br.com.rizermarketplaces.core.marketplace.dto;

import br.com.rizermarketplaces.core.marketplace.model.Address;

import java.time.OffsetDateTime;
import java.util.UUID;

public class AddressView {

    private final UUID id;
    private final String label;
    private final String zipCode;
    private final String street;
    private final String number;
    private final String complement;
    private final String neighborhood;
    private final String city;
    private final String state;
    private final String countryCode;
    private final String country;
    private final boolean isPrimary;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    public AddressView(
        UUID id, String label, String zipCode, String street, String number, String complement,
        String neighborhood, String city, String state, String countryCode, String country,
        boolean isPrimary, OffsetDateTime createdAt, OffsetDateTime updatedAt
    ) {
        this.id = id;
        this.label = label;
        this.zipCode = zipCode;
        this.street = street;
        this.number = number;
        this.complement = complement;
        this.neighborhood = neighborhood;
        this.city = city;
        this.state = state;
        this.countryCode = countryCode;
        this.country = country;
        this.isPrimary = isPrimary;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static AddressView from(Address a) {
        return new AddressView(
            a.getId(), a.getLabel(), a.getZipCode(), a.getStreet(), a.getNumber(), a.getComplement(),
            a.getNeighborhood(), a.getCity(), a.getState(), a.getCountryCode(), a.getCountry(),
            a.isPrimary(), a.getCreatedAt(), a.getUpdatedAt()
        );
    }

    public UUID getId() { return id; }
    public String getLabel() { return label; }
    public String getZipCode() { return zipCode; }
    public String getStreet() { return street; }
    public String getNumber() { return number; }
    public String getComplement() { return complement; }
    public String getNeighborhood() { return neighborhood; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getCountryCode() { return countryCode; }
    public String getCountry() { return country; }
    public boolean isPrimary() { return isPrimary; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
