package br.com.rizermarketplaces.core.marketplace.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "vehicle_models")
public class VehicleModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "brand_id", nullable = false)
    private Integer brandId;

    @Column(name = "fipe_id", nullable = false)
    private Integer fipeId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public Integer getId() { return id; }
    public Integer getBrandId() { return brandId; }
    public void setBrandId(Integer brandId) { this.brandId = brandId; }
    public Integer getFipeId() { return fipeId; }
    public void setFipeId(Integer fipeId) { this.fipeId = fipeId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
