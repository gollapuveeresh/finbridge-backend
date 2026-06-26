-- V14__add_recording_to_consultations.sql
ALTER TABLE consultations ADD COLUMN IF NOT EXISTS recording_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE consultations ADD COLUMN IF NOT EXISTS video_url VARCHAR(1024);
