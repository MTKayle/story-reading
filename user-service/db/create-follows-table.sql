-- SQL script to create the follows table in user_db
-- Run this against your PostgreSQL database for the user-service schema

CREATE TABLE IF NOT EXISTS follows (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    story_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_follows_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_follows_user_story UNIQUE (user_id, story_id)
);

-- Create index for faster queries
CREATE INDEX IF NOT EXISTS idx_follows_user_id ON follows(user_id);
CREATE INDEX IF NOT EXISTS idx_follows_story_id ON follows(story_id);
CREATE INDEX IF NOT EXISTS idx_follows_created_at ON follows(created_at DESC);

