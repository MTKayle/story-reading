-- Add view_count column to stories (PostgreSQL)
-- This migration handles existing rows that would have NULL values

-- Step 1: Add column as nullable first (to avoid constraint violation on existing rows)
ALTER TABLE stories
    ADD COLUMN IF NOT EXISTS view_count bigint;

-- Step 2: Set default value 0 for any existing rows that have NULL
UPDATE stories SET view_count = 0 WHERE view_count IS NULL;

-- Step 3: Now make the column NOT NULL and set default for future inserts
ALTER TABLE stories
    ALTER COLUMN view_count SET NOT NULL,
    ALTER COLUMN view_count SET DEFAULT 0;

-- Verify with:
-- SELECT id, title, view_count FROM stories ORDER BY id LIMIT 10;
