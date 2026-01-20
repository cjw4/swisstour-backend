-- Migration script: Event table schema changes
-- From: change-events-table (afaf47b)
-- To: main branch schema

-- =============================================
-- 1. ADD NEW COLUMNS
-- =============================================

-- Add event_id column (unique external identifier)
ALTER TABLE "events" ADD COLUMN IF NOT EXISTS event_id BIGINT UNIQUE;

-- Add link and registration columns
ALTER TABLE "events" ADD COLUMN IF NOT EXISTS info_link VARCHAR(500);
ALTER TABLE "events" ADD COLUMN IF NOT EXISTS registration_link VARCHAR(500);
ALTER TABLE "events" ADD COLUMN IF NOT EXISTS registration_start DATE;
ALTER TABLE "events" ADD COLUMN IF NOT EXISTS swisstour_type VARCHAR(50);

-- Add start_date and end_date columns
ALTER TABLE "events" ADD COLUMN IF NOT EXISTS start_date DATE;
ALTER TABLE "events" ADD COLUMN IF NOT EXISTS end_date DATE;

-- =============================================
-- 2. MIGRATE DATA (before dropping old columns)
-- =============================================

-- Migrate date -> start_date
UPDATE "events" SET start_date = date WHERE start_date IS NULL AND date IS NOT NULL;

-- Calculate end_date from date + number_days
UPDATE "events" SET end_date = date + (COALESCE(number_days, 1) - 1) * INTERVAL '1 day'
WHERE end_date IS NULL AND date IS NOT NULL;

-- =============================================
-- 3. DROP OLD COLUMNS
-- =============================================

ALTER TABLE "events" DROP COLUMN IF EXISTS date;
ALTER TABLE "events" DROP COLUMN IF EXISTS number_days;

-- =============================================
-- 4. MODIFY COLUMN CONSTRAINTS
-- =============================================

-- Remove NOT NULL constraints
ALTER TABLE "events" ALTER COLUMN name DROP NOT NULL;
ALTER TABLE "events" ALTER COLUMN tier DROP NOT NULL;
ALTER TABLE "events" ALTER COLUMN city DROP NOT NULL;
ALTER TABLE "events" ALTER COLUMN country DROP NOT NULL;
ALTER TABLE "events" ALTER COLUMN number_players DROP NOT NULL;

-- Remove DEFAULT from year column
ALTER TABLE "events" ALTER COLUMN year DROP DEFAULT;

-- Update has_results column definition
ALTER TABLE "events" ALTER COLUMN has_results SET DEFAULT false;
ALTER TABLE "events" ALTER COLUMN has_results SET NOT NULL;

-- =============================================
-- 5. SETUP SEQUENCE FOR AUTO-GENERATED ID
-- =============================================

-- Create sequence (Hibernate default: starts at 1, increments by 50)
CREATE SEQUENCE IF NOT EXISTS event_seq START WITH 1 INCREMENT BY 50;

-- Set sequence to continue from next allocation block after max existing id
SELECT setval('event_seq', COALESCE((SELECT ((MAX(id) / 50) + 1) * 50 FROM "events"), 1), false);
