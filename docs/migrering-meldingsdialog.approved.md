# Meldingsdialog ved migrering

Tekniske ID-er, ident og meldingstidsstempler er erstattet med plassholdere.
Opplysningstype-UUID-er, faktaverdier og gyldighetsdatoer er beholdt.

| Runde | Retning | Melding | Opplysninger |
| --- | --- | --- | --- |
| 1 | Avsender → mottak | `migrering_startet` | 0 totalt (nye: 0) |
| 1 | Mottak → avsender | `migrering_startet` | 1 mangler |
| 2 | Avsender → mottak | `migrering_startet` | 1 totalt (nye: 1) |
| 2 | Mottak → avsender | `migrering_startet` | 10 mangler |
| 3 | Avsender → mottak | `migrering_startet` | 11 totalt (nye: 10) |
| 3 | Mottak → avsender | `migrering_startet` | 16 mangler |
| 4 | Avsender → mottak | `migrering_startet` | 27 totalt (nye: 16) |
| 4 | Mottak → avsender | `migrering_startet` | 2 mangler |
| 5 | Avsender → mottak | `migrering_startet` | 29 totalt (nye: 2) |

## Runde 1

### Avsender → mottak

Nye fakta i denne runden (JSON-meldingen inneholder også tidligere sendte fakta).

| UUID | Navn | Verdi | Fra og med | Til og med |
| --- | --- | --- | --- | --- |
| | Ingen nye fakta | | | |

<details>
<summary>Hele JSON-meldingen</summary>

```json
{
  "@event_name" : "migrering_startet",
  "sakId" : 13699879,
  "migreringId" : "<migreringId>",
  "ident" : "<ident>",
  "migreringTidspunkt" : "2026-01-05",
  "opplysninger" : [ ]
}
```

</details>

### Mottak → avsender

| Manglende UUID | Navn |
| --- | --- |
| 0194881f-940b-76ff-acf5-ba7bcb367233 | Fødselsdato |

<details>
<summary>Hele JSON-meldingen</summary>

```json
{
  "@event_name" : "migrering_startet",
  "sakId" : 13699879,
  "migreringId" : "<migreringId>",
  "ident" : "<ident>",
  "migreringTidspunkt" : "2026-01-05",
  "opplysninger" : [ ],
  "@id" : "<@id>",
  "@opprettet" : "<@opprettet>",
  "system_read_count" : 0,
  "system_participating_services" : [ {
    "id" : "<id>",
    "time" : "<time>"
  }, {
    "id" : "<id>",
    "time" : "<time>"
  } ],
  "manglendeOpplysninger" : [ {
    "uuid" : "0194881f-940b-76ff-acf5-ba7bcb367233",
    "navn" : "Fødselsdato"
  } ],
  "@forårsaket_av" : {
    "id" : "<id>",
    "opprettet" : "<opprettet>",
    "event_name" : "migrering_startet"
  }
}
```

</details>

## Runde 2

### Avsender → mottak

Nye fakta i denne runden (JSON-meldingen inneholder også tidligere sendte fakta).

| UUID | Navn | Verdi | Fra og med | Til og med |
| --- | --- | --- | --- | --- |
| 0194881f-940b-76ff-acf5-ba7bcb367233 | Fødselsdato | "1990-01-01" | 2026-01-05 | Ingen sluttdato |

<details>
<summary>Hele JSON-meldingen</summary>

```json
{
  "@event_name" : "migrering_startet",
  "sakId" : 13699879,
  "migreringId" : "<migreringId>",
  "ident" : "<ident>",
  "migreringTidspunkt" : "2026-01-05",
  "opplysninger" : [ {
    "uuid" : "0194881f-940b-76ff-acf5-ba7bcb367233",
    "navn" : "Fødselsdato",
    "verdi" : "1990-01-01",
    "gyldigFraOgMed" : "2026-01-05"
  } ]
}
```

</details>

### Mottak → avsender

| Manglende UUID | Navn |
| --- | --- |
| 0194881f-9413-77ce-92ec-d29700f04244 | Inntektsopplysninger |
| 0194881f-9442-707b-a6ee-e96c06877be0 | Registrert som arbeidssøker |
| 0194881f-9444-7a73-a458-0af81c034d85 | Har rett til ordinære dagpenger gjennom arbeidsforhold |
| 0194881f-9444-7a73-a458-0af81c034d86 | Skal permittering vurderes |
| 0194881f-9444-7a73-a458-0af81c034d87 | Forskutterte lønnsgarantimidler i form av dagpenger |
| 0194881f-9444-7a73-a458-0af81c034d88 | Skal permittert fra fiskeindustrien vurderes |
| 01948d3c-4bea-7802-9d18-5342a5e2be99 | Avtjent verneplikt |
| 0194ff86-a035-7eb0-9c60-21899f7cc0c1 | Skal reell arbeidssøker vurderes |
| 019f608d-f9c5-7ccc-a6f3-c53531d1f00c | Skal eksport vurderes |
| 01a0c316-a2e9-7088-acb1-da5596bd0055 | Skal etablering vurderes |

<details>
<summary>Hele JSON-meldingen</summary>

```json
{
  "@event_name" : "migrering_startet",
  "sakId" : 13699879,
  "migreringId" : "<migreringId>",
  "ident" : "<ident>",
  "migreringTidspunkt" : "2026-01-05",
  "opplysninger" : [ {
    "uuid" : "0194881f-940b-76ff-acf5-ba7bcb367233",
    "navn" : "Fødselsdato",
    "verdi" : "1990-01-01",
    "gyldigFraOgMed" : "2026-01-05"
  } ],
  "@id" : "<@id>",
  "@opprettet" : "<@opprettet>",
  "system_read_count" : 0,
  "system_participating_services" : [ {
    "id" : "<id>",
    "time" : "<time>"
  }, {
    "id" : "<id>",
    "time" : "<time>"
  } ],
  "manglendeOpplysninger" : [ {
    "uuid" : "0194881f-9413-77ce-92ec-d29700f04244",
    "navn" : "Inntektsopplysninger"
  }, {
    "uuid" : "0194881f-9442-707b-a6ee-e96c06877be0",
    "navn" : "Registrert som arbeidssøker"
  }, {
    "uuid" : "0194881f-9444-7a73-a458-0af81c034d85",
    "navn" : "Har rett til ordinære dagpenger gjennom arbeidsforhold"
  }, {
    "uuid" : "0194881f-9444-7a73-a458-0af81c034d86",
    "navn" : "Skal permittering vurderes"
  }, {
    "uuid" : "0194881f-9444-7a73-a458-0af81c034d87",
    "navn" : "Forskutterte lønnsgarantimidler i form av dagpenger"
  }, {
    "uuid" : "0194881f-9444-7a73-a458-0af81c034d88",
    "navn" : "Skal permittert fra fiskeindustrien vurderes"
  }, {
    "uuid" : "01948d3c-4bea-7802-9d18-5342a5e2be99",
    "navn" : "Avtjent verneplikt"
  }, {
    "uuid" : "0194ff86-a035-7eb0-9c60-21899f7cc0c1",
    "navn" : "Skal reell arbeidssøker vurderes"
  }, {
    "uuid" : "019f608d-f9c5-7ccc-a6f3-c53531d1f00c",
    "navn" : "Skal eksport vurderes"
  }, {
    "uuid" : "01a0c316-a2e9-7088-acb1-da5596bd0055",
    "navn" : "Skal etablering vurderes"
  } ],
  "@forårsaket_av" : {
    "id" : "<id>",
    "opprettet" : "<opprettet>",
    "event_name" : "migrering_startet"
  }
}
```

</details>

## Runde 3

### Avsender → mottak

Nye fakta i denne runden (JSON-meldingen inneholder også tidligere sendte fakta).

| UUID | Navn | Verdi | Fra og med | Til og med |
| --- | --- | --- | --- | --- |
| 0194881f-9413-77ce-92ec-d29700f04244 | Inntektsopplysninger | Strukturert verdi (se JSON) | 2026-01-05 | Ingen sluttdato |
| 0194881f-9442-707b-a6ee-e96c06877be0 | Registrert som arbeidssøker | true | 2026-01-05 | Ingen sluttdato |
| 0194881f-9444-7a73-a458-0af81c034d85 | Har rett til ordinære dagpenger gjennom arbeidsforhold | true | 2026-01-05 | Ingen sluttdato |
| 0194881f-9444-7a73-a458-0af81c034d86 | Skal permittering vurderes | false | 2026-01-05 | Ingen sluttdato |
| 0194881f-9444-7a73-a458-0af81c034d87 | Forskutterte lønnsgarantimidler i form av dagpenger | false | 2026-01-05 | Ingen sluttdato |
| 0194881f-9444-7a73-a458-0af81c034d88 | Skal permittert fra fiskeindustrien vurderes | false | 2026-01-05 | Ingen sluttdato |
| 01948d3c-4bea-7802-9d18-5342a5e2be99 | Avtjent verneplikt | false | 2026-01-05 | Ingen sluttdato |
| 0194ff86-a035-7eb0-9c60-21899f7cc0c1 | Skal reell arbeidssøker vurderes | true | 2026-01-05 | Ingen sluttdato |
| 019f608d-f9c5-7ccc-a6f3-c53531d1f00c | Skal eksport vurderes | false | 2026-01-05 | Ingen sluttdato |
| 01a0c316-a2e9-7088-acb1-da5596bd0055 | Skal etablering vurderes | false | 2026-01-05 | Ingen sluttdato |

<details>
<summary>Hele JSON-meldingen</summary>

```json
{
  "@event_name" : "migrering_startet",
  "sakId" : 13699879,
  "migreringId" : "<migreringId>",
  "ident" : "<ident>",
  "migreringTidspunkt" : "2026-01-05",
  "opplysninger" : [ {
    "uuid" : "0194881f-940b-76ff-acf5-ba7bcb367233",
    "navn" : "Fødselsdato",
    "verdi" : "1990-01-01",
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9413-77ce-92ec-d29700f04244",
    "navn" : "Inntektsopplysninger",
    "verdi" : {
      "inntektsId" : "<inntektsId>",
      "sisteAvsluttendeKalenderMåned" : "2025-11",
      "manueltRedigert" : false,
      "inntektsListe" : [ {
        "årMåned" : "2025-11",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-10",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-09",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-08",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-07",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-06",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-05",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-04",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-03",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-02",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-01",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2024-12",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      } ]
    },
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9442-707b-a6ee-e96c06877be0",
    "navn" : "Registrert som arbeidssøker",
    "verdi" : true,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9444-7a73-a458-0af81c034d85",
    "navn" : "Har rett til ordinære dagpenger gjennom arbeidsforhold",
    "verdi" : true,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9444-7a73-a458-0af81c034d86",
    "navn" : "Skal permittering vurderes",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9444-7a73-a458-0af81c034d87",
    "navn" : "Forskutterte lønnsgarantimidler i form av dagpenger",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9444-7a73-a458-0af81c034d88",
    "navn" : "Skal permittert fra fiskeindustrien vurderes",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "01948d3c-4bea-7802-9d18-5342a5e2be99",
    "navn" : "Avtjent verneplikt",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194ff86-a035-7eb0-9c60-21899f7cc0c1",
    "navn" : "Skal reell arbeidssøker vurderes",
    "verdi" : true,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "019f608d-f9c5-7ccc-a6f3-c53531d1f00c",
    "navn" : "Skal eksport vurderes",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "01a0c316-a2e9-7088-acb1-da5596bd0055",
    "navn" : "Skal etablering vurderes",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  } ]
}
```

</details>

### Mottak → avsender

| Manglende UUID | Navn |
| --- | --- |
| 0194881f-9433-70e9-a85b-c246150c45cd | Sykepenger etter lovens kapittel 8 |
| 0194881f-9433-70e9-a85b-c246150c45ce | Pleiepenger etter lovens kapittel 9 |
| 0194881f-9433-70e9-a85b-c246150c45cf | Omsorgspenger etter lovens kapittel 9 |
| 0194881f-9433-70e9-a85b-c246150c45d0 | Opplæringspenger etter lovens kapittel 9 |
| 0194881f-9433-70e9-a85b-c246150c45d1 | Uføretrygd etter lovens kapittel 12 |
| 0194881f-9433-70e9-a85b-c246150c45d2 | Foreldrepenger etter lovens kapittel 14 |
| 0194881f-9433-70e9-a85b-c246150c45d3 | Svangerskapspenger etter lovens kapittel 14 |
| 0194881f-9435-72a8-b1ce-9575cbc2a75e | Har krav på lønn fra arbeidsgiver |
| 0194881f-9435-72a8-b1ce-9575cbc2a75f | Ønsket arbeidstid |
| 0194881f-9441-7d1b-a06a-6727543a141e | Kan jobbe heltid og deltid |
| 0194881f-9442-707b-a6ee-e96c06877bd9 | Kan jobbe i hele Norge |
| 0194881f-9442-707b-a6ee-e96c06877bdc | Kan ta alle typer arbeid |
| 0194881f-9442-707b-a6ee-e96c06877bde | Villig til å bytte yrke |
| 0194881f-9445-734c-a7ee-045edf29b522 | Brukeren er under utdanning eller opplæring |
| 0194881f-9447-7e36-a569-3e9f42bff9f6 | Bruker er utestengt fra dagpenger |
| 0196ab10-0cff-7301-99d6-65be50a50201 | Bostedsland er Norge |

<details>
<summary>Hele JSON-meldingen</summary>

```json
{
  "@event_name" : "migrering_startet",
  "sakId" : 13699879,
  "migreringId" : "<migreringId>",
  "ident" : "<ident>",
  "migreringTidspunkt" : "2026-01-05",
  "opplysninger" : [ {
    "uuid" : "0194881f-940b-76ff-acf5-ba7bcb367233",
    "navn" : "Fødselsdato",
    "verdi" : "1990-01-01",
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9413-77ce-92ec-d29700f04244",
    "navn" : "Inntektsopplysninger",
    "verdi" : {
      "inntektsId" : "<inntektsId>",
      "sisteAvsluttendeKalenderMåned" : "2025-11",
      "manueltRedigert" : false,
      "inntektsListe" : [ {
        "årMåned" : "2025-11",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-10",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-09",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-08",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-07",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-06",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-05",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-04",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-03",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-02",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-01",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2024-12",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      } ]
    },
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9442-707b-a6ee-e96c06877be0",
    "navn" : "Registrert som arbeidssøker",
    "verdi" : true,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9444-7a73-a458-0af81c034d85",
    "navn" : "Har rett til ordinære dagpenger gjennom arbeidsforhold",
    "verdi" : true,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9444-7a73-a458-0af81c034d86",
    "navn" : "Skal permittering vurderes",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9444-7a73-a458-0af81c034d87",
    "navn" : "Forskutterte lønnsgarantimidler i form av dagpenger",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9444-7a73-a458-0af81c034d88",
    "navn" : "Skal permittert fra fiskeindustrien vurderes",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "01948d3c-4bea-7802-9d18-5342a5e2be99",
    "navn" : "Avtjent verneplikt",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194ff86-a035-7eb0-9c60-21899f7cc0c1",
    "navn" : "Skal reell arbeidssøker vurderes",
    "verdi" : true,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "019f608d-f9c5-7ccc-a6f3-c53531d1f00c",
    "navn" : "Skal eksport vurderes",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "01a0c316-a2e9-7088-acb1-da5596bd0055",
    "navn" : "Skal etablering vurderes",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  } ],
  "@id" : "<@id>",
  "@opprettet" : "<@opprettet>",
  "system_read_count" : 0,
  "system_participating_services" : [ {
    "id" : "<id>",
    "time" : "<time>"
  }, {
    "id" : "<id>",
    "time" : "<time>"
  } ],
  "manglendeOpplysninger" : [ {
    "uuid" : "0194881f-9433-70e9-a85b-c246150c45cd",
    "navn" : "Sykepenger etter lovens kapittel 8"
  }, {
    "uuid" : "0194881f-9433-70e9-a85b-c246150c45ce",
    "navn" : "Pleiepenger etter lovens kapittel 9"
  }, {
    "uuid" : "0194881f-9433-70e9-a85b-c246150c45cf",
    "navn" : "Omsorgspenger etter lovens kapittel 9"
  }, {
    "uuid" : "0194881f-9433-70e9-a85b-c246150c45d0",
    "navn" : "Opplæringspenger etter lovens kapittel 9"
  }, {
    "uuid" : "0194881f-9433-70e9-a85b-c246150c45d1",
    "navn" : "Uføretrygd etter lovens kapittel 12"
  }, {
    "uuid" : "0194881f-9433-70e9-a85b-c246150c45d2",
    "navn" : "Foreldrepenger etter lovens kapittel 14"
  }, {
    "uuid" : "0194881f-9433-70e9-a85b-c246150c45d3",
    "navn" : "Svangerskapspenger etter lovens kapittel 14"
  }, {
    "uuid" : "0194881f-9435-72a8-b1ce-9575cbc2a75e",
    "navn" : "Har krav på lønn fra arbeidsgiver"
  }, {
    "uuid" : "0194881f-9435-72a8-b1ce-9575cbc2a75f",
    "navn" : "Ønsket arbeidstid"
  }, {
    "uuid" : "0194881f-9441-7d1b-a06a-6727543a141e",
    "navn" : "Kan jobbe heltid og deltid"
  }, {
    "uuid" : "0194881f-9442-707b-a6ee-e96c06877bd9",
    "navn" : "Kan jobbe i hele Norge"
  }, {
    "uuid" : "0194881f-9442-707b-a6ee-e96c06877bdc",
    "navn" : "Kan ta alle typer arbeid"
  }, {
    "uuid" : "0194881f-9442-707b-a6ee-e96c06877bde",
    "navn" : "Villig til å bytte yrke"
  }, {
    "uuid" : "0194881f-9445-734c-a7ee-045edf29b522",
    "navn" : "Brukeren er under utdanning eller opplæring"
  }, {
    "uuid" : "0194881f-9447-7e36-a569-3e9f42bff9f6",
    "navn" : "Bruker er utestengt fra dagpenger"
  }, {
    "uuid" : "0196ab10-0cff-7301-99d6-65be50a50201",
    "navn" : "Bostedsland er Norge"
  } ],
  "@forårsaket_av" : {
    "id" : "<id>",
    "opprettet" : "<opprettet>",
    "event_name" : "migrering_startet"
  }
}
```

</details>

## Runde 4

### Avsender → mottak

Nye fakta i denne runden (JSON-meldingen inneholder også tidligere sendte fakta).

| UUID | Navn | Verdi | Fra og med | Til og med |
| --- | --- | --- | --- | --- |
| 0194881f-9433-70e9-a85b-c246150c45cd | Sykepenger etter lovens kapittel 8 | false | 2026-01-05 | Ingen sluttdato |
| 0194881f-9433-70e9-a85b-c246150c45ce | Pleiepenger etter lovens kapittel 9 | false | 2026-01-05 | Ingen sluttdato |
| 0194881f-9433-70e9-a85b-c246150c45cf | Omsorgspenger etter lovens kapittel 9 | false | 2026-01-05 | Ingen sluttdato |
| 0194881f-9433-70e9-a85b-c246150c45d0 | Opplæringspenger etter lovens kapittel 9 | false | 2026-01-05 | Ingen sluttdato |
| 0194881f-9433-70e9-a85b-c246150c45d1 | Uføretrygd etter lovens kapittel 12 | false | 2026-01-05 | Ingen sluttdato |
| 0194881f-9433-70e9-a85b-c246150c45d2 | Foreldrepenger etter lovens kapittel 14 | false | 2026-01-05 | Ingen sluttdato |
| 0194881f-9433-70e9-a85b-c246150c45d3 | Svangerskapspenger etter lovens kapittel 14 | false | 2026-01-05 | Ingen sluttdato |
| 0194881f-9435-72a8-b1ce-9575cbc2a75e | Har krav på lønn fra arbeidsgiver | false | 2026-01-05 | Ingen sluttdato |
| 0194881f-9435-72a8-b1ce-9575cbc2a75f | Ønsket arbeidstid | 37.5 | 2026-01-05 | Ingen sluttdato |
| 0194881f-9441-7d1b-a06a-6727543a141e | Kan jobbe heltid og deltid | true | 2026-01-05 | Ingen sluttdato |
| 0194881f-9442-707b-a6ee-e96c06877bd9 | Kan jobbe i hele Norge | true | 2026-01-05 | Ingen sluttdato |
| 0194881f-9442-707b-a6ee-e96c06877bdc | Kan ta alle typer arbeid | true | 2026-01-05 | Ingen sluttdato |
| 0194881f-9442-707b-a6ee-e96c06877bde | Villig til å bytte yrke | true | 2026-01-05 | Ingen sluttdato |
| 0194881f-9445-734c-a7ee-045edf29b522 | Brukeren er under utdanning eller opplæring | false | 2026-01-05 | Ingen sluttdato |
| 0194881f-9447-7e36-a569-3e9f42bff9f6 | Bruker er utestengt fra dagpenger | false | 2026-01-05 | Ingen sluttdato |
| 0196ab10-0cff-7301-99d6-65be50a50201 | Bostedsland er Norge | true | 2026-01-05 | Ingen sluttdato |

<details>
<summary>Hele JSON-meldingen</summary>

```json
{
  "@event_name" : "migrering_startet",
  "sakId" : 13699879,
  "migreringId" : "<migreringId>",
  "ident" : "<ident>",
  "migreringTidspunkt" : "2026-01-05",
  "opplysninger" : [ {
    "uuid" : "0194881f-940b-76ff-acf5-ba7bcb367233",
    "navn" : "Fødselsdato",
    "verdi" : "1990-01-01",
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9413-77ce-92ec-d29700f04244",
    "navn" : "Inntektsopplysninger",
    "verdi" : {
      "inntektsId" : "<inntektsId>",
      "sisteAvsluttendeKalenderMåned" : "2025-11",
      "manueltRedigert" : false,
      "inntektsListe" : [ {
        "årMåned" : "2025-11",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-10",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-09",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-08",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-07",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-06",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-05",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-04",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-03",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-02",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-01",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2024-12",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      } ]
    },
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9442-707b-a6ee-e96c06877be0",
    "navn" : "Registrert som arbeidssøker",
    "verdi" : true,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9444-7a73-a458-0af81c034d85",
    "navn" : "Har rett til ordinære dagpenger gjennom arbeidsforhold",
    "verdi" : true,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9444-7a73-a458-0af81c034d86",
    "navn" : "Skal permittering vurderes",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9444-7a73-a458-0af81c034d87",
    "navn" : "Forskutterte lønnsgarantimidler i form av dagpenger",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9444-7a73-a458-0af81c034d88",
    "navn" : "Skal permittert fra fiskeindustrien vurderes",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "01948d3c-4bea-7802-9d18-5342a5e2be99",
    "navn" : "Avtjent verneplikt",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194ff86-a035-7eb0-9c60-21899f7cc0c1",
    "navn" : "Skal reell arbeidssøker vurderes",
    "verdi" : true,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "019f608d-f9c5-7ccc-a6f3-c53531d1f00c",
    "navn" : "Skal eksport vurderes",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "01a0c316-a2e9-7088-acb1-da5596bd0055",
    "navn" : "Skal etablering vurderes",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9433-70e9-a85b-c246150c45cd",
    "navn" : "Sykepenger etter lovens kapittel 8",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9433-70e9-a85b-c246150c45ce",
    "navn" : "Pleiepenger etter lovens kapittel 9",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9433-70e9-a85b-c246150c45cf",
    "navn" : "Omsorgspenger etter lovens kapittel 9",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9433-70e9-a85b-c246150c45d0",
    "navn" : "Opplæringspenger etter lovens kapittel 9",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9433-70e9-a85b-c246150c45d1",
    "navn" : "Uføretrygd etter lovens kapittel 12",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9433-70e9-a85b-c246150c45d2",
    "navn" : "Foreldrepenger etter lovens kapittel 14",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9433-70e9-a85b-c246150c45d3",
    "navn" : "Svangerskapspenger etter lovens kapittel 14",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9435-72a8-b1ce-9575cbc2a75e",
    "navn" : "Har krav på lønn fra arbeidsgiver",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9435-72a8-b1ce-9575cbc2a75f",
    "navn" : "Ønsket arbeidstid",
    "verdi" : 37.5,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9441-7d1b-a06a-6727543a141e",
    "navn" : "Kan jobbe heltid og deltid",
    "verdi" : true,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9442-707b-a6ee-e96c06877bd9",
    "navn" : "Kan jobbe i hele Norge",
    "verdi" : true,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9442-707b-a6ee-e96c06877bdc",
    "navn" : "Kan ta alle typer arbeid",
    "verdi" : true,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9442-707b-a6ee-e96c06877bde",
    "navn" : "Villig til å bytte yrke",
    "verdi" : true,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9445-734c-a7ee-045edf29b522",
    "navn" : "Brukeren er under utdanning eller opplæring",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9447-7e36-a569-3e9f42bff9f6",
    "navn" : "Bruker er utestengt fra dagpenger",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0196ab10-0cff-7301-99d6-65be50a50201",
    "navn" : "Bostedsland er Norge",
    "verdi" : true,
    "gyldigFraOgMed" : "2026-01-05"
  } ]
}
```

</details>

### Mottak → avsender

| Manglende UUID | Navn |
| --- | --- |
| 0194881f-9428-74d5-b160-f63a4c61a23b | Barn fra register/søknad/saksbehandler |
| 0194881f-942e-7cb0-aa59-05ea449d88e0 | Oppgitt andre ytelser utenfor NAV i søknaden |

<details>
<summary>Hele JSON-meldingen</summary>

```json
{
  "@event_name" : "migrering_startet",
  "sakId" : 13699879,
  "migreringId" : "<migreringId>",
  "ident" : "<ident>",
  "migreringTidspunkt" : "2026-01-05",
  "opplysninger" : [ {
    "uuid" : "0194881f-940b-76ff-acf5-ba7bcb367233",
    "navn" : "Fødselsdato",
    "verdi" : "1990-01-01",
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9413-77ce-92ec-d29700f04244",
    "navn" : "Inntektsopplysninger",
    "verdi" : {
      "inntektsId" : "<inntektsId>",
      "sisteAvsluttendeKalenderMåned" : "2025-11",
      "manueltRedigert" : false,
      "inntektsListe" : [ {
        "årMåned" : "2025-11",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-10",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-09",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-08",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-07",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-06",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-05",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-04",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-03",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-02",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-01",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2024-12",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      } ]
    },
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9442-707b-a6ee-e96c06877be0",
    "navn" : "Registrert som arbeidssøker",
    "verdi" : true,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9444-7a73-a458-0af81c034d85",
    "navn" : "Har rett til ordinære dagpenger gjennom arbeidsforhold",
    "verdi" : true,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9444-7a73-a458-0af81c034d86",
    "navn" : "Skal permittering vurderes",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9444-7a73-a458-0af81c034d87",
    "navn" : "Forskutterte lønnsgarantimidler i form av dagpenger",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9444-7a73-a458-0af81c034d88",
    "navn" : "Skal permittert fra fiskeindustrien vurderes",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "01948d3c-4bea-7802-9d18-5342a5e2be99",
    "navn" : "Avtjent verneplikt",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194ff86-a035-7eb0-9c60-21899f7cc0c1",
    "navn" : "Skal reell arbeidssøker vurderes",
    "verdi" : true,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "019f608d-f9c5-7ccc-a6f3-c53531d1f00c",
    "navn" : "Skal eksport vurderes",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "01a0c316-a2e9-7088-acb1-da5596bd0055",
    "navn" : "Skal etablering vurderes",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9433-70e9-a85b-c246150c45cd",
    "navn" : "Sykepenger etter lovens kapittel 8",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9433-70e9-a85b-c246150c45ce",
    "navn" : "Pleiepenger etter lovens kapittel 9",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9433-70e9-a85b-c246150c45cf",
    "navn" : "Omsorgspenger etter lovens kapittel 9",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9433-70e9-a85b-c246150c45d0",
    "navn" : "Opplæringspenger etter lovens kapittel 9",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9433-70e9-a85b-c246150c45d1",
    "navn" : "Uføretrygd etter lovens kapittel 12",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9433-70e9-a85b-c246150c45d2",
    "navn" : "Foreldrepenger etter lovens kapittel 14",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9433-70e9-a85b-c246150c45d3",
    "navn" : "Svangerskapspenger etter lovens kapittel 14",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9435-72a8-b1ce-9575cbc2a75e",
    "navn" : "Har krav på lønn fra arbeidsgiver",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9435-72a8-b1ce-9575cbc2a75f",
    "navn" : "Ønsket arbeidstid",
    "verdi" : 37.5,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9441-7d1b-a06a-6727543a141e",
    "navn" : "Kan jobbe heltid og deltid",
    "verdi" : true,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9442-707b-a6ee-e96c06877bd9",
    "navn" : "Kan jobbe i hele Norge",
    "verdi" : true,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9442-707b-a6ee-e96c06877bdc",
    "navn" : "Kan ta alle typer arbeid",
    "verdi" : true,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9442-707b-a6ee-e96c06877bde",
    "navn" : "Villig til å bytte yrke",
    "verdi" : true,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9445-734c-a7ee-045edf29b522",
    "navn" : "Brukeren er under utdanning eller opplæring",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9447-7e36-a569-3e9f42bff9f6",
    "navn" : "Bruker er utestengt fra dagpenger",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0196ab10-0cff-7301-99d6-65be50a50201",
    "navn" : "Bostedsland er Norge",
    "verdi" : true,
    "gyldigFraOgMed" : "2026-01-05"
  } ],
  "@id" : "<@id>",
  "@opprettet" : "<@opprettet>",
  "system_read_count" : 0,
  "system_participating_services" : [ {
    "id" : "<id>",
    "time" : "<time>"
  }, {
    "id" : "<id>",
    "time" : "<time>"
  } ],
  "manglendeOpplysninger" : [ {
    "uuid" : "0194881f-9428-74d5-b160-f63a4c61a23b",
    "navn" : "Barn fra register/søknad/saksbehandler"
  }, {
    "uuid" : "0194881f-942e-7cb0-aa59-05ea449d88e0",
    "navn" : "Oppgitt andre ytelser utenfor NAV i søknaden"
  } ],
  "@forårsaket_av" : {
    "id" : "<id>",
    "opprettet" : "<opprettet>",
    "event_name" : "migrering_startet"
  }
}
```

</details>

## Runde 5

### Avsender → mottak

Nye fakta i denne runden (JSON-meldingen inneholder også tidligere sendte fakta).

| UUID | Navn | Verdi | Fra og med | Til og med |
| --- | --- | --- | --- | --- |
| 0194881f-9428-74d5-b160-f63a4c61a23b | Barn fra register/søknad/saksbehandler | Strukturert verdi (se JSON) | 2026-01-05 | Ingen sluttdato |
| 0194881f-942e-7cb0-aa59-05ea449d88e0 | Oppgitt andre ytelser utenfor NAV i søknaden | false | 2026-01-05 | Ingen sluttdato |

<details>
<summary>Hele JSON-meldingen</summary>

```json
{
  "@event_name" : "migrering_startet",
  "sakId" : 13699879,
  "migreringId" : "<migreringId>",
  "ident" : "<ident>",
  "migreringTidspunkt" : "2026-01-05",
  "opplysninger" : [ {
    "uuid" : "0194881f-940b-76ff-acf5-ba7bcb367233",
    "navn" : "Fødselsdato",
    "verdi" : "1990-01-01",
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9413-77ce-92ec-d29700f04244",
    "navn" : "Inntektsopplysninger",
    "verdi" : {
      "inntektsId" : "<inntektsId>",
      "sisteAvsluttendeKalenderMåned" : "2025-11",
      "manueltRedigert" : false,
      "inntektsListe" : [ {
        "årMåned" : "2025-11",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-10",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-09",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-08",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-07",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-06",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-05",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-04",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-03",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-02",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2025-01",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      }, {
        "årMåned" : "2024-12",
        "harAvvik" : false,
        "klassifiserteInntekter" : [ {
          "beløp" : 50000,
          "inntektKlasse" : "ARBEIDSINNTEKT"
        } ]
      } ]
    },
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9442-707b-a6ee-e96c06877be0",
    "navn" : "Registrert som arbeidssøker",
    "verdi" : true,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9444-7a73-a458-0af81c034d85",
    "navn" : "Har rett til ordinære dagpenger gjennom arbeidsforhold",
    "verdi" : true,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9444-7a73-a458-0af81c034d86",
    "navn" : "Skal permittering vurderes",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9444-7a73-a458-0af81c034d87",
    "navn" : "Forskutterte lønnsgarantimidler i form av dagpenger",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9444-7a73-a458-0af81c034d88",
    "navn" : "Skal permittert fra fiskeindustrien vurderes",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "01948d3c-4bea-7802-9d18-5342a5e2be99",
    "navn" : "Avtjent verneplikt",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194ff86-a035-7eb0-9c60-21899f7cc0c1",
    "navn" : "Skal reell arbeidssøker vurderes",
    "verdi" : true,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "019f608d-f9c5-7ccc-a6f3-c53531d1f00c",
    "navn" : "Skal eksport vurderes",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "01a0c316-a2e9-7088-acb1-da5596bd0055",
    "navn" : "Skal etablering vurderes",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9433-70e9-a85b-c246150c45cd",
    "navn" : "Sykepenger etter lovens kapittel 8",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9433-70e9-a85b-c246150c45ce",
    "navn" : "Pleiepenger etter lovens kapittel 9",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9433-70e9-a85b-c246150c45cf",
    "navn" : "Omsorgspenger etter lovens kapittel 9",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9433-70e9-a85b-c246150c45d0",
    "navn" : "Opplæringspenger etter lovens kapittel 9",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9433-70e9-a85b-c246150c45d1",
    "navn" : "Uføretrygd etter lovens kapittel 12",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9433-70e9-a85b-c246150c45d2",
    "navn" : "Foreldrepenger etter lovens kapittel 14",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9433-70e9-a85b-c246150c45d3",
    "navn" : "Svangerskapspenger etter lovens kapittel 14",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9435-72a8-b1ce-9575cbc2a75e",
    "navn" : "Har krav på lønn fra arbeidsgiver",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9435-72a8-b1ce-9575cbc2a75f",
    "navn" : "Ønsket arbeidstid",
    "verdi" : 37.5,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9441-7d1b-a06a-6727543a141e",
    "navn" : "Kan jobbe heltid og deltid",
    "verdi" : true,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9442-707b-a6ee-e96c06877bd9",
    "navn" : "Kan jobbe i hele Norge",
    "verdi" : true,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9442-707b-a6ee-e96c06877bdc",
    "navn" : "Kan ta alle typer arbeid",
    "verdi" : true,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9442-707b-a6ee-e96c06877bde",
    "navn" : "Villig til å bytte yrke",
    "verdi" : true,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9445-734c-a7ee-045edf29b522",
    "navn" : "Brukeren er under utdanning eller opplæring",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9447-7e36-a569-3e9f42bff9f6",
    "navn" : "Bruker er utestengt fra dagpenger",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0196ab10-0cff-7301-99d6-65be50a50201",
    "navn" : "Bostedsland er Norge",
    "verdi" : true,
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-9428-74d5-b160-f63a4c61a23b",
    "navn" : "Barn fra register/søknad/saksbehandler",
    "verdi" : {
      "barn" : [ ]
    },
    "gyldigFraOgMed" : "2026-01-05"
  }, {
    "uuid" : "0194881f-942e-7cb0-aa59-05ea449d88e0",
    "navn" : "Oppgitt andre ytelser utenfor NAV i søknaden",
    "verdi" : false,
    "gyldigFraOgMed" : "2026-01-05"
  } ]
}
```

</details>

## Resultat i testen

Mottaket videresendte hendelsen én gang, og testen opprettet én domenebehandling.
Etter regelkjøring og rettighetsplugin er beregnet avgjørelse `Innvilgelse`.
Dette er testobservasjoner, ikke en publisert svarmelding eller et fattet vedtak.
