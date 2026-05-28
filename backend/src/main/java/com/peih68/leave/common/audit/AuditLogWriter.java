package com.peih68.leave.common.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Writes rows into audit_log. JSONB columns are written via JdbcTemplate with an
 * explicit ::jsonb cast, since the JPA model does not map this table.
 */
@Component
@RequiredArgsConstructor
public class AuditLogWriter {

    private final JdbcTemplate jdbcTemplate;

    public void record(Long actorId, String action, String targetType, Long targetId,
                        String oldValueJson, String newValueJson) {
        jdbcTemplate.update("""
                INSERT INTO audit_log (actor_id, action, target_type, target_id, old_value, new_value, created_at)
                VALUES (?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), NOW())
                """,
                actorId, action, targetType, targetId, oldValueJson, newValueJson);
    }
}
