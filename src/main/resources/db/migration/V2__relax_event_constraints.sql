-- Migration to update events table schema
-- Note: This migration is for upgrading existing databases.
-- For fresh databases, V0 already creates the correct schema.

-- =============================================
-- 1. MIGRATE DATA (only if old columns exist)
-- =============================================

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'events' AND column_name = 'date') THEN
        UPDATE "events" SET start_date = date WHERE start_date IS NULL AND date IS NOT NULL;
        UPDATE "events" SET end_date = date + (COALESCE(number_days, 1) - 1) * INTERVAL '1 day'
            WHERE end_date IS NULL AND date IS NOT NULL;
    END IF;
END $$;

-- =============================================
-- 2. DROP OLD COLUMNS (safe with IF EXISTS)
-- =============================================

ALTER TABLE "events" DROP COLUMN IF EXISTS date;
ALTER TABLE "events" DROP COLUMN IF EXISTS number_days;

-- =============================================
-- 3. MODIFY COLUMN CONSTRAINTS
-- =============================================

-- Remove NOT NULL constraints (will succeed even if already nullable)
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
