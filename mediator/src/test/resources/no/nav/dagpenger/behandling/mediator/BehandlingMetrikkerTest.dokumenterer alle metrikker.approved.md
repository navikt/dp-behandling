# dp-behandling Metrikker

Automatisk generert dokumentasjon over alle Prometheus-metrikker i dp-behandling.
Generert fra `PrometheusRegistry` — legger du til en ny metrikk, oppdateres denne filen automatisk.

## Alle metrikker

| Metrikk | Type | Beskrivelse | Labels |
|---------|------|-------------|--------|
| `active_transactions` | gauge | Number of active transactions currently open | — |
| `behandling_hent_person_tid` | histogram | Tid brukt på å hente person med behandlinger | antall_behandlinger |
| `behandling_lagre_person_tid_sekunder` | histogram | Tid det tar å lagre en person, i sekunder | — |
| `behandling_publiser_aktivitetslogg_sekunder` | histogram | Tid brukt på å hente person med behandlinger | — |
| `commit_duration_seconds` | histogram | Time spent in actual DB commit | — |
| `dp_antall_behandlinger` | histogram | Antall behandlinger per person | — |
| `dp_behandling_avbrutt` | counter | Antall avbrutte behandlinger | hendelse_type, aarsak |
| `dp_behandling_avklaring_levetid_sekunder` | histogram | Tid fra avklaring opprettes til den lukkes, i sekunder | kode |
| `dp_behandling_avklaring_opprettet` | counter | Avklaringer opprettet i behandling | kode |
| `dp_behandling_besluttet` | counter | Antall besluttede behandlinger | — |
| `dp_behandling_ferdig` | counter | Antall ferdige behandlinger | utfall, hendelse_type, automatisk |
| `dp_behandling_forslag` | counter | Antall forslag til vedtak | hendelse_type |
| `dp_behandling_godkjent` | counter | Antall godkjente behandlinger | — |
| `dp_behandling_hendelse_haandtert` | counter | Antall hendelser håndtert | hendelse |
| `dp_behandling_hent_behandling_tid` | histogram | Tid brukt på å hente en behandling | — |
| `dp_behandling_meldekort_koe_storrelse` | gauge | Antall meldekort i behandlingskø | status |
| `dp_behandling_opplysning_svar` | counter | Antall opplysningssvar mottatt | — |
| `dp_behandling_opplysninger_antall` | histogram | Antall egne opplysninger per behandling ved ferdigstillelse | — |
| `dp_behandling_opprettet` | counter | Antall behandlinger opprettet | hendelse_type |
| `dp_behandling_sendt_tilbake` | counter | Antall behandlinger sendt tilbake | — |
| `dp_behandling_start_hendelse_mottatt` | counter | Antall StartHendelser mottatt (sammenlign med opprettet_total for å se avvisningsrate) | hendelse |
| `dp_behandling_tid_brukt_per_endring` | histogram | Tid det tar å utføre en endring i behandlingen, i sekunder | opplysningstype |
| `dp_behandling_tid_brukt_per_sletting` | histogram | Tid det tar å utføre en sletting i behandlingen, i sekunder | opplysningstype |
| `dp_behandling_tilstandsendring` | counter | Antall tilstandsendringer i behandlinger | fra_tilstand, til_tilstand |
| `dp_behandling_utbetaling_status` | counter | Antall utbetalingsstatusendringer mottatt | status |
| `dp_behandling_vilkaar` | counter | Vilkårvurderinger ved ferdigstillelse | vilkaar, status |
| `hendelse_behandling_tid_sekunder` | histogram | Tid det tar å behandle en hendelse, i sekunder | hendelse |
| `hendelse_behandling_total_tid_sekunder` | histogram | Total tid det tar å behandle en hendelse, i sekunder | hendelse |
| `transaction_duration_seconds` | histogram | Full transaction duration including queries and commit | — |
| `transactions_committed` | counter | Total number of committed transactions | — |
| `transactions_rolledback` | counter | Total number of rolled-back transactions | — |
