package com.srm.creditengine.domain.treasury;

/** Direction of a cash movement from the point of view of the fund. */
public enum MovementDirection {
    /** Money leaving the fund (payment of a credit assignment). */
    DEBIT,
    /** Money entering the fund (e.g. capital contribution, collection of a receivable). */
    CREDIT
}
