-- =====================================================================
-- V2: Seed reference data (departments, leave types, VN 2026 holidays).
-- User accounts are seeded by DemoDataInitializer (dev profile only)
-- so password hashes never live in version-controlled SQL.
-- =====================================================================

INSERT INTO departments (code, name, is_active, created_at, updated_at, created_by, updated_by) VALUES
    ('ENG',   'Engineering',  TRUE, NOW(), NOW(), 'system', 'system'),
    ('SALES', 'Sales',        TRUE, NOW(), NOW(), 'system', 'system'),
    ('HR',    'Human Resources', TRUE, NOW(), NOW(), 'system', 'system');

INSERT INTO leave_types (code, name, description, default_quota_days, requires_balance, is_active, created_at, updated_at, created_by, updated_by) VALUES
    ('ANNUAL',   'Annual Leave',   'Phép năm theo luật lao động', 12.0, TRUE,  TRUE, NOW(), NOW(), 'system', 'system'),
    ('SICK',     'Sick Leave',     'Nghỉ ốm có giấy xác nhận',     3.0, TRUE,  TRUE, NOW(), NOW(), 'system', 'system'),
    ('PERSONAL', 'Personal Leave', 'Nghỉ việc riêng',              3.0, TRUE,  TRUE, NOW(), NOW(), 'system', 'system'),
    ('UNPAID',   'Unpaid Leave',   'Nghỉ không lương',             0.0, FALSE, TRUE, NOW(), NOW(), 'system', 'system');

-- VN public holidays 2026 (ngày dương lịch; Tết âm theo lịch dự kiến)
INSERT INTO holidays (holiday_date, name, created_at, updated_at, created_by, updated_by) VALUES
    ('2026-01-01', 'Tết Dương lịch',          NOW(), NOW(), 'system', 'system'),
    ('2026-02-16', 'Tết Nguyên đán (30 Tết)', NOW(), NOW(), 'system', 'system'),
    ('2026-02-17', 'Tết Nguyên đán (Mùng 1)', NOW(), NOW(), 'system', 'system'),
    ('2026-02-18', 'Tết Nguyên đán (Mùng 2)', NOW(), NOW(), 'system', 'system'),
    ('2026-02-19', 'Tết Nguyên đán (Mùng 3)', NOW(), NOW(), 'system', 'system'),
    ('2026-02-20', 'Tết Nguyên đán (Mùng 4)', NOW(), NOW(), 'system', 'system'),
    ('2026-04-26', 'Giỗ Tổ Hùng Vương',       NOW(), NOW(), 'system', 'system'),
    ('2026-04-30', 'Giải phóng miền Nam',     NOW(), NOW(), 'system', 'system'),
    ('2026-05-01', 'Quốc tế Lao động',        NOW(), NOW(), 'system', 'system'),
    ('2026-09-02', 'Quốc khánh',              NOW(), NOW(), 'system', 'system');
