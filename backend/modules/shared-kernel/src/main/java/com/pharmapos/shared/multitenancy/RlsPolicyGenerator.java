package com.pharmapos.shared.multitenancy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Generates the standard Row-Level Security policy SQL for a table,
 * following the exact pattern specified in TDD §4.4.
 *
 * <p>Every table with a {@code tenant_id} column must receive the
 * {@code tenant_isolation} policy. Branch-owned sensitive tables (clinical,
 * controlled register) additionally receive {@code branch_isolation}.
 *
 * <p>Use this utility in Flyway migration scripts so the SQL is consistent
 * across every module and the pattern cannot drift from the design.
 *
 * <pre>{@code
 * // In a Flyway Java migration:
 * RlsPolicyGenerator.generate("rx_prescription", true)
 *     .forEach(sql -> jdbcTemplate.execute(sql));
 * }</pre>
 */
public final class RlsPolicyGenerator {

    private RlsPolicyGenerator() { /* utility */ }

    /**
     * @param tableName    SQL table name (optionally schema-qualified, e.g. {@code sal_sale})
     * @param branchScoped {@code true} to also generate a {@code branch_isolation} policy
     *                     on {@code branch_id} against {@code app.branch_ids}
     * @return ordered list of SQL statements to execute in sequence
     */
    public static List<String> generate(String tableName, boolean branchScoped) {
        Objects.requireNonNull(tableName, "tableName must not be null");

        List<String> stmts = new ArrayList<>(4);

        stmts.add("ALTER TABLE " + tableName + " ENABLE ROW LEVEL SECURITY;");
        // FORCE ensures even the table owner (migrator role) is subject to RLS.
        stmts.add("ALTER TABLE " + tableName + " FORCE ROW LEVEL SECURITY;");

        // TDD §4.4 exact pattern:
        // nullif(current_setting('app.tenant_id', true), '')::uuid
        //   → a missing / empty setting → NULL → matches no rows → fail closed
        stmts.add("""
                CREATE POLICY tenant_isolation ON %s
                  USING      (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
                  WITH CHECK (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);
                """.formatted(tableName));

        if (branchScoped) {
            // TDD §4.4: "Branch-owned sensitive tables add a second policy on branch_id
            // against app.branch_ids."
            // An empty app.branch_ids value means tenant-wide access (owner/admin) → pass.
            stmts.add("""
                    CREATE POLICY branch_isolation ON %s
                      USING (
                        nullif(current_setting('app.branch_ids', true), '') IS NULL
                        OR branch_id::text = ANY(
                             string_to_array(current_setting('app.branch_ids', true), ','))
                      )
                      WITH CHECK (
                        nullif(current_setting('app.branch_ids', true), '') IS NULL
                        OR branch_id::text = ANY(
                             string_to_array(current_setting('app.branch_ids', true), ','))
                      );
                    """.formatted(tableName));
        }

        return stmts;
    }

    /** Convenience: returns all statements as a single SQL string ready for embedding in a migration. */
    public static String generateSql(String tableName, boolean branchScoped) {
        return String.join("\n", generate(tableName, branchScoped));
    }
}
