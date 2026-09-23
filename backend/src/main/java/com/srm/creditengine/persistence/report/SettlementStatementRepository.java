package com.srm.creditengine.persistence.report;

import com.srm.creditengine.domain.assignment.CreditAssignmentStatus;
import com.srm.creditengine.domain.currency.CurrencyCode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Settlement statement ("extrato de liquidação") over large volumes, in native SQL through
 * {@link JdbcClient}: no entity is loaded, only the projected columns travel.
 *
 * <p>Performance notes:
 *
 * <ul>
 *   <li>the period filter compares the raw {@code created_at} column with a precomputed half-open
 *       instant range ({@code >= from AND < to}), so the {@code (…, created_at DESC)} indexes are
 *       usable (no function applied to the indexed column);
 *   <li>the totals are denormalized on {@code credit_assignment}, so no aggregation over
 *       receivables is needed;
 *   <li>the count query skips the join with {@code assignor} (every filter is on the operation).
 * </ul>
 */
@Repository
@Transactional(readOnly = true)
public class SettlementStatementRepository {

    private static final String SELECT = """
            SELECT ca.id, ca.assignor_id, a.name AS assignor_name, a.document AS assignor_document,
                   ca.status, ca.payment_currency, ca.receivables_count,
                   ca.total_face_value, ca.total_discount, ca.total_net_amount,
                   ca.created_at, ca.settled_at
              FROM credit_assignment ca
              JOIN assignor a ON a.id = ca.assignor_id
            """;
    private static final String COUNT = "SELECT count(*) FROM credit_assignment ca ";

    private final JdbcClient jdbc;

    public SettlementStatementRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public SettlementStatementPage find(SettlementStatementCriteria criteria) {
        SqlConditions conditions = new SqlConditions()
                .whenPresent("ca.created_at >= :createdFrom", "createdFrom", timestamp(criteria.createdFrom()))
                .whenPresent("ca.created_at < :createdBefore", "createdBefore", timestamp(criteria.createdBefore()))
                .whenPresent("ca.assignor_id = :assignorId", "assignorId", criteria.assignorId())
                .whenPresent("ca.payment_currency = :currency", "currency", name(criteria.currency()))
                .whenPresent("ca.status = :status", "status", name(criteria.status()));
        Map<String, Object> parameters = conditions.parameters();

        long total = jdbc.sql(COUNT + conditions.where())
                .params(parameters)
                .query(Long.class)
                .single();
        if (total == 0 || criteria.offset() >= total) {
            return new SettlementStatementPage(List.of(), total);
        }
        List<SettlementStatementRow> rows = jdbc.sql(
                        SELECT + conditions.where() + " " + criteria.sort().toSql() + " LIMIT :limit OFFSET :offset")
                .params(parameters)
                .param("limit", criteria.size())
                .param("offset", criteria.offset())
                .query(SettlementStatementRepository::mapRow)
                .list();
        return new SettlementStatementPage(rows, total);
    }

    private static SettlementStatementRow mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new SettlementStatementRow(
                rs.getObject("id", UUID.class),
                rs.getObject("assignor_id", UUID.class),
                rs.getString("assignor_name"),
                rs.getString("assignor_document"),
                CreditAssignmentStatus.valueOf(rs.getString("status")),
                CurrencyCode.valueOf(rs.getString("payment_currency")),
                rs.getInt("receivables_count"),
                rs.getBigDecimal("total_face_value"),
                rs.getBigDecimal("total_discount"),
                rs.getBigDecimal("total_net_amount"),
                rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                instant(rs.getObject("settled_at", OffsetDateTime.class)));
    }

    private static @Nullable OffsetDateTime timestamp(@Nullable Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

    private static @Nullable Instant instant(@Nullable OffsetDateTime timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static @Nullable String name(@Nullable Enum<?> value) {
        return value == null ? null : value.name();
    }
}
