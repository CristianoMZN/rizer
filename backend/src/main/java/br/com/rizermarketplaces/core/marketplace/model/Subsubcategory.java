package br.com.rizermarketplaces.core.marketplace.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "subsubcategories")
public class Subsubcategory {

    @Id
    private Long id;

    @Column(nullable = false, length = 120)
    private String slug;

    public Long getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }
}
