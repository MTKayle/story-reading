-- SQL: add unique constraint to prevent two chapters with same chapter_number for a story
-- Run this against your Postgres database for the story-service schema

-- 1) Find duplicates (rows with same story_id and chapter_number occurring more than once)
-- Replace 'public' schema if different
SELECT story_id, chapter_number, count(*) as cnt
FROM chapters
GROUP BY story_id, chapter_number
HAVING count(*) > 1;

-- If duplicates exist, you must resolve them before adding the constraint.
-- Example strategy: keep the earliest created row and delete others (review before running):
-- DELETE FROM chapters
-- WHERE id IN (
--   SELECT id FROM (
--     SELECT id, ROW_NUMBER() OVER (PARTITION BY story_id, chapter_number ORDER BY created_at ASC) rn
--     FROM chapters
--   ) t WHERE t.rn > 1
-- );

-- 2) Add the unique constraint
ALTER TABLE chapters
ADD CONSTRAINT uk_story_chapter_number UNIQUE (story_id, chapter_number);

-- 3) Verify
-- This should now fail if any duplicate insertion happens

-- Notes:
-- - Take a DB backup before running these commands.
-- - If you use a migration tool (Flyway/Liquibase), create a migration under your migrations folder instead of running ad-hoc.

