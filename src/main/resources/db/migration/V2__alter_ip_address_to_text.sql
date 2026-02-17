-- Change ip_address column from INET to TEXT to avoid JDBC type mismatch.
-- INET provides network-address validation but is incompatible with standard
-- JDBC VARCHAR parameter binding. TEXT is sufficient for audit logging purposes.
ALTER TABLE audit_logs ALTER COLUMN ip_address TYPE TEXT;
