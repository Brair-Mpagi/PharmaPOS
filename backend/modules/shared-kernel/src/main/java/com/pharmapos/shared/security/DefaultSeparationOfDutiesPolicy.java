package com.pharmapos.shared.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Default implementation of {@link SeparationOfDutiesPolicy}.
 *
 * <p>Looks up the entity's creator by querying the table whose name matches
 * {@code entityType} (e.g. {@code prc_purchase_order}). All entity tables
 * follow the standard audit-column convention: {@code created_by uuid NOT NULL}.
 *
 * <p>The query is RLS-protected because it runs inside the caller's transaction
 * after {@link com.pharmapos.shared.multitenancy.TenantTransactionInterceptor}
 * has set {@code app.tenant_id}.
 */
@Component
public class DefaultSeparationOfDutiesPolicy implements SeparationOfDutiesPolicy {

    private final JdbcTemplate jdbcTemplate;

    public DefaultSeparationOfDutiesPolicy(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean canApprove(UUID entityId, UUID userId, String entityType) {
        // Whitelist the entity type to prevent SQL injection via entityType parameter.
        validateEntityType(entityType);

        // Build a parameterised query. Table name must be a literal (not a bind variable)
        // but we have validated it against a whitelist above.
        String sql = "SELECT created_by FROM " + entityType + " WHERE id = ?";

        var results = jdbcTemplate.query(sql, (rs, n) -> {
            String createdBy = rs.getString("created_by");
            return createdBy != null ? UUID.fromString(createdBy) : null;
        }, entityId);

        if (results.isEmpty()) {
            throw new EntityNotFoundException(
                "No " + entityType + " found with id " + entityId);
        }

        UUID createdBy = results.getFirst();
        // The user may approve if they are NOT the creator
        return createdBy == null || !createdBy.equals(userId);
    }

    /**
     * Validates that the entityType is an expected table name.
     * Table names may contain only lowercase letters, digits and underscores.
     *
     * @throws IllegalArgumentException if the name contains unexpected characters
     */
    private void validateEntityType(String entityType) {
        if (entityType == null || !entityType.matches("[a-z][a-z0-9_]{0,63}")) {
            throw new IllegalArgumentException(
                "entityType must be a valid table name (a-z, 0-9, _): " + entityType);
        }
        // Allowed prefixes — bounded contexts that can have SoD rules
        boolean allowed = entityType.startsWith("prc_")  // procurement
                       || entityType.startsWith("inv_")  // inventory (counts)
                       || entityType.startsWith("sal_")  // sales (refunds, overrides)
                       || entityType.startsWith("fin_")  // finance (journal approvals)
                       || entityType.startsWith("rx_");  // clinical
        if (!allowed) {
            throw new IllegalArgumentException(
                "entityType '" + entityType + "' is not in the SoD-approved table list");
        }
    }
}
