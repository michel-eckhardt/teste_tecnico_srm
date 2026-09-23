package com.srm.creditengine.domain.pricing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Catalog row of a receivable type: display description and whether new operations may use it. */
@Entity
@Table(name = "receivable_type")
public class ReceivableTypeDefinition {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "code", length = 40)
    private ReceivableType code;

    @Column(name = "description", nullable = false, length = 120)
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected ReceivableTypeDefinition() {
        // for JPA
    }

    public ReceivableTypeDefinition(ReceivableType code, String description, boolean active) {
        this.code = code;
        this.description = description;
        this.active = active;
    }

    public ReceivableType getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return active;
    }
}
