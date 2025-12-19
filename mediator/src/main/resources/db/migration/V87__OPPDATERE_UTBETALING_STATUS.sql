-- Behandling med id 019aca22-e56e-7535-8f64-2ae44edfa81c har vi fått OK status fra Helved.
-- Oppdatert manuel i dp-mellom-barken-og-veden https://github.com/navikt/dp-mellom-barken-og-veden/commit/3ba4f313780e7d83116c0f1fc97a958794fb07c2
UPDATE utbetaling_status set status = 'UTFØRT'
WHERE behandling_id = '019aca22-e56e-7535-8f64-2ae44edfa81c';

-- Behandling med id 019b1eee-018d-7739-8fba-04a482f74097 har vi fått OK status fra Helved.
-- Oppdatert manuel i dp-mellom-barken-og-veden https://github.com/navikt/dp-mellom-barken-og-veden/commit/c0c21163186e549ce068328016acdb5114f74343
UPDATE utbetaling_status set status = 'UTFØRT'
WHERE behandling_id = '019b1eee-018d-7739-8fba-04a482f74097';

