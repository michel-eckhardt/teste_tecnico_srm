package com.srm.creditengine.domain.currency;

/**
 * ISO-4217 currencies supported by the fund. Each constant must exist in the {@code currency}
 * table (foreign keys guarantee it), which also holds the display name and the minor units.
 */
public enum CurrencyCode {
    BRL,
    USD
}
