package com.srm.creditengine.domain.assignment;

/**
 * Outcome of a creation request.
 *
 * @param created {@code false} when an idempotent replay returned an operation created before
 */
public record CreationResult(CreditAssignment assignment, boolean created) {}
