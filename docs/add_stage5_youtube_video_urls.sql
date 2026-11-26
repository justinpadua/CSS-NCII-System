ALTER TABLE learning_stage_contents
    ADD COLUMN IF NOT EXISTS youtube_video_urls LONGTEXT NULL;
