-- Default autovacuum-terskel (20 % + 50 rader) gjør at bloat på opplysning
-- kan bygge seg opp mot ~100k døde rader (ca. 20 %) før autovacuum trigges.
-- Tabellen har mye skriving (fjernet-flagg + vaktmesterens sletting), så vi
-- senker terskelen for hyppigere opprydding.
ALTER TABLE opplysning SET (
    autovacuum_vacuum_scale_factor = 0.05,
    autovacuum_vacuum_threshold = 1000
);
