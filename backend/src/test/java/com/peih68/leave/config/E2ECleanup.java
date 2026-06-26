package com.peih68.leave.config;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Shared cleanup for {@code @SpringBootTest(RANDOM_PORT)} E2E suites, which commit (no
 * rollback). Every suite that wipes {@code users} must first delete the tables that
 * reference it, or a later suite's {@code DELETE FROM users} hits an FK violation when a
 * previous suite left committed children. Gradle does not guarantee a stable test-class
 * order across machines, so this must run in every E2E {@code @BeforeEach} to stay
 * order-independent. See CLAUDE.md gotchas.
 */
public final class E2ECleanup {

    private E2ECleanup() {}

    /** Delete user-scoped data in FK-safe order (children first), then users. */
    public static void wipeUsersFkSafe(JdbcTemplate jdbc) {
        jdbc.update("DELETE FROM attachments");
        jdbc.update("DELETE FROM notifications");
        jdbc.update("DELETE FROM approval_actions");
        jdbc.update("DELETE FROM leave_requests");
        jdbc.update("DELETE FROM leave_balances");
        jdbc.update("DELETE FROM audit_log");
        jdbc.update("DELETE FROM refresh_tokens");
        jdbc.update("DELETE FROM users");
    }
}
