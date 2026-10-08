-- Utestengning lagres nå som en del av den generiske rettighetstatus-tabellen (nøklet på regelverk),
-- på samme måte som Dagpenger og Ferietillegg. Utestengning er ikke tatt i bruk i prod ennå, så det er
-- ingen data å migrere.
DROP TABLE utestengning;
