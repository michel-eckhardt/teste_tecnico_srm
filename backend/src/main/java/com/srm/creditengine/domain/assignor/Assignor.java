package com.srm.creditengine.domain.assignor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import org.hibernate.annotations.UuidGenerator;

/** Company (cedente) that sells receivables to the fund, identified by its CNPJ. */
@Entity
@Table(name = "assignor")
public class Assignor {

    private static final Pattern CNPJ_DIGITS = Pattern.compile("\\d{14}");

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "document", nullable = false, length = 14, unique = true, updatable = false)
    private String document;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Assignor() {
        // for JPA
    }

    private Assignor(String name, String document, Instant createdAt) {
        this.name = name;
        this.document = document;
        this.createdAt = createdAt;
    }

    /**
     * @param document CNPJ with 14 digits and no punctuation (check digits validated by the caller)
     */
    public static Assignor register(String name, String document, Instant createdAt) {
        Objects.requireNonNull(createdAt, "createdAt");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (document == null || !CNPJ_DIGITS.matcher(document).matches()) {
            throw new IllegalArgumentException("document must be a CNPJ with 14 digits");
        }
        return new Assignor(name.strip(), document, createdAt);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDocument() {
        return document;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof Assignor that && id != null && id.equals(that.id));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
