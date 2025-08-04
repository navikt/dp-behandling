-- Redundant med primary key
DROP INDEX CONCURRENTLY IF EXISTS idx_opplysning_kilde_system_kilde_id;
DROP INDEX CONCURRENTLY IF EXISTS idx_opplysning_kilde_saksbehandler_kilde_id;

-- Disse brukes kun av sletting, der kan vi tåle at det går saktere
-- Inserts går ganske seigt på grunn av disse
DROP INDEX CONCURRENTLY IF EXISTS idx_opplysning_utledet_av_opplysning_id;
DROP INDEX CONCURRENTLY IF EXISTS idx_opplysninger_utledet_av;
