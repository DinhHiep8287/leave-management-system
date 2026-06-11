-- V5: Carry-over of remaining leave into the next year (REQUIREMENTS §4, v2.0.0).
ALTER TABLE leave_balances
    ADD COLUMN carried_over_days NUMERIC(5, 1) NOT NULL DEFAULT 0;
