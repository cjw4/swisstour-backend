-- Migration script: Event table schema changes
-- From: change-events-table (afaf47b)
-- To: main branch schema
-- Note: This migration is for upgrading existing databases. 
-- For fresh databases, V0 already creates the correct schema.

-- =============================================
-- 1. ADD NEW COLUMNS (safe with IF NOT EXISTS)
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
-- 2. MIGRATE DATA (only if old columns exist)
-- =============================================

-- Migrate date -> start_date (only if date column exists)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'events' AND column_name = 'date') THEN
        UPDATE "events" SET start_date = date WHERE start_date IS NULL AND date IS NOT NULL;
        UPDATE "events" SET end_date = date + (COALESCE(number_days, 1) - 1) * INTERVAL '1 day'
            WHERE end_date IS NULL AND date IS NOT NULL;
    END IF;
END $$;

-- =============================================
-- 3. DROP OLD COLUMNS (safe with IF EXISTS)
-- =============================================

ALTER TABLE "events" DROP COLUMN IF EXISTS date;
ALTER TABLE "events" DROP COLUMN IF EXISTS number_days;

-- =============================================
-- 4. MODIFY COLUMN CONSTRAINTS
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

-- =============================================
-- 5. SETUP SEQUENCE FOR AUTO-GENERATED ID
-- =============================================

-- Create sequence (Hibernate 6 default naming: {table}_seq)
CREATE SEQUENCE IF NOT EXISTS events_seq START WITH 1 INCREMENT BY 50;

-- Set sequence to continue from next allocation block after max existing id
SELECT setval('events_seq', COALESCE((SELECT ((MAX(id) / 50) + 1) * 50 FROM "events"), 1), false);
