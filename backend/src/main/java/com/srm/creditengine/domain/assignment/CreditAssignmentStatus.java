package com.srm.creditengine.domain.assignment;

/**
 * Lifecycle of a credit assignment:
 *
 * <pre>
 *   PENDING --settle--> SETTLED     (terminal)
 *   PENDING --cancel--> CANCELLED   (terminal)
 * </pre>
 */
public enum CreditAssignmentStatus {
    PENDING,
    SETTLED,
    CANCELLED;

    public boolean canTransitionTo(CreditAssignmentStatus target) {
        return this == PENDING && (target == SETTLED || target == CANCELLED);
    }
}
