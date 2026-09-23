package com.srm.creditengine.domain.currency;

/** Where an exchange rate came from. */
public enum ExchangeRateSource {
    /** Registered by an operator through the API. */
    MANUAL,
    /** Synchronized from the Frankfurter API (European Central Bank reference rates). */
    FRANKFURTER,
    /** Bootstrap value created by the database migration. */
    SEED
}
