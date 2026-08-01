-- V15__drop_donor_email.sql
-- Donor email is no longer collected, stored, or returned anywhere — drop the column.

ALTER TABLE donations
    DROP COLUMN IF EXISTS donor_email;
