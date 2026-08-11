ALTER TABLE behandler_hendelse
    ADD COLUMN opplysninger_id uuid REFERENCES opplysninger (opplysninger_id);
