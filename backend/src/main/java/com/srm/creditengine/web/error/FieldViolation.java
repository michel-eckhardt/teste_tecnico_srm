package com.srm.creditengine.web.error;

/**
 * One invalid input item of a {@code VALIDATION_ERROR} problem.
 *
 * @param field path of the offending field, parameter or header (e.g. {@code receivables[0].faceValue})
 * @param message what is wrong with it (Portuguese)
 */
public record FieldViolation(String field, String message) {}
