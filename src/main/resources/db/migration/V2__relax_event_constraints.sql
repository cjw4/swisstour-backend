-- Migration to update events table schema

-- =============================================
-- 1. MIGRATE DATA (before dropping old columns)
-- =============================================

-- Migrate date -> start_date
UPDATE "events"
SET start_date = date
WHERE start_date IS NULL AND date IS NOT NULL;

-- Calculate end_date from date + number_days
UPDATE "events"
SET end_date = date + (COALESCE (number_days, 1) - 1) * INTERVAL '1 day'
WHERE end_date IS NULL AND date IS NOT NULL;

-- =============================================
-- 2. DROP OLD COLUMNS
-- =============================================

ALTER TABLE "events" DROP COLUMN IF EXISTS date;
ALTER TABLE "events" DROP COLUMN IF EXISTS number_days;

-- =============================================
-- 3. MODIFY COLUMN CONSTRAINTS
-- =============================================

-- Remove NOT NULL constraints
ALTER TABLE "events"
    ALTER COLUMN name DROP NOT NULL;
ALTER TABLE "events"
    ALTER COLUMN tier DROP NOT NULL;
ALTER TABLE "events"
    ALTER COLUMN city DROP NOT NULL;
ALTER TABLE "events"
    ALTER COLUMN country DROP NOT NULL;
ALTER TABLE "events"
    ALTER COLUMN number_players DROP NOT NULL;

-- Remove DEFAULT from year column
ALTER TABLE "events"
    ALTER COLUMN year DROP DEFAULT;

-- Update has_results column definition
ALTER TABLE "events"
    ALTER COLUMN has_results SET DEFAULT false;
ALTER TABLE "events"
    ALTER COLUMN has_results SET NOT NULL;
