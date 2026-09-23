package com.srm.creditengine.domain.assignment;

import com.srm.creditengine.domain.currency.CurrencyCode;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Command to open a credit assignment.
 *
 * @param receivables receivables in the order they were submitted
 */
public record NewCreditAssignment(UUID assignorId, CurrencyCode paymentCurrency, List<Item> receivables) {

    /** One receivable of the command; the type code is validated against the catalog. */
    public record Item(String receivableType, BigDecimal faceValue, CurrencyCode faceCurrency, LocalDate dueDate) {}

    public NewCreditAssignment {
        Objects.requireNonNull(assignorId, "assignorId");
        Objects.requireNonNull(paymentCurrency, "paymentCurrency");
        receivables = List.copyOf(receivables);
    }

    /**
     * SHA-256 of a canonical representation of the command. Numerically equal amounts produce the
     * same fingerprint ({@code "10000.00"} and {@code "10000"}), so a client retrying with a
     * re-serialized payload is still recognized as the same request.
     */
    public String fingerprint() {
        StringBuilder canonical = new StringBuilder()
                .append("assignor=")
                .append(assignorId)
                .append(";payment=")
                .append(paymentCurrency);
        for (Item item : receivables) {
            canonical
                    .append(";receivable=")
                    .append(item.receivableType())
                    .append('|')
                    .append(item.faceValue().stripTrailingZeros().toPlainString())
                    .append('|')
                    .append(item.faceCurrency())
                    .append('|')
                    .append(item.dueDate());
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is mandatory in every JVM", impossible);
        }
    }
}
