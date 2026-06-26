-- V16__create_consultation_recordings.sql
CREATE TABLE IF NOT EXISTS consultation_recordings (
    consultation_id UUID PRIMARY KEY,
    recording_enabled BOOLEAN DEFAULT FALSE,
    video_url VARCHAR(1024)
);
