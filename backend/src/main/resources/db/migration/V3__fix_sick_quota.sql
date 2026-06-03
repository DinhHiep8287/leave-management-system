-- =====================================================================
-- V3: Align SICK default quota with REQUIREMENTS.md §3 (30 days/year).
-- V2 seeded it as 3.0 by mistake. This only changes the default used when
-- initializing new balances; existing balance rows are left untouched.
-- =====================================================================

UPDATE leave_types
SET default_quota_days = 30.0,
    updated_at = NOW(),
    updated_by = 'system'
WHERE code = 'SICK';
