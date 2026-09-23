package com.srm.creditengine.domain.currency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Reference data of a supported currency (read only). */
@Entity
@Table(name = "currency")
public class Currency {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "code", length = 3)
    private CurrencyCode code;

    @Column(name = "name", nullable = false, length = 60)
    private String name;

    @Column(name = "decimals", nullable = false)
    private short decimals;

    protected Currency() {
        // for JPA
    }

    public Currency(CurrencyCode code, String name, int decimals) {
        this.code = code;
        this.name = name;
        this.decimals = (short) decimals;
    }

    public CurrencyCode getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    /** Minor units used when rounding amounts in this currency. */
    public int getDecimals() {
        return decimals;
    }
}
