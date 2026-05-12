ALTER TABLE behandling_aktive_behov ADD COLUMN IF NOT EXISTS kilde TEXT NOT NULL DEFAULT 'api';
