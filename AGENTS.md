# AGENTS.md — dp-behandling

## Repository Overview

dp-behandling er en Kotlin-applikasjon som håndterer behandling av dagpengesøknader for NAV.
Den reagerer på hendelser, starter behandlinger, innhenter opplysninger og produserer vedtak/forslag til vedtak.

## Tech Stack

- **Språk**: Kotlin (JVM 21)
- **Rammeverk**: Ktor (server + client)
- **Bygg**: Gradle (Kotlin DSL) med version catalog (`dp-version-catalog`)
- **Database**: PostgreSQL 17 (Flyway-migrasjoner)
- **Meldingssystem**: Kafka (Rapids & Rivers)
- **Test**: JUnit 5, Kotest, Cucumber/Gherkin, MockK, Approval Tests
- **Linter**: ktlint (kjøres automatisk ved kompilering)
- **Plattform**: Nais (Kubernetes på GCP)
- **Observability**: OpenTelemetry, Prometheus, Loki, Elastic

## Project Structure

```
dp-behandling/
├── avklaring/       # Avklaringsdomene
├── dag/             # DAG-strukturer (rettet asyklisk graf)
├── dagpenger/       # Dagpenger-regler og beregning
├── dato/            # Dato-utilities
├── konfigurasjon/   # App-konfigurasjon
├── mediator/        # Hovedapplikasjon (Ktor, Rapids & Rivers, DB)
├── modell/          # Domenemodell (behandling, opplysninger)
├── openapi/         # OpenAPI-spesifikasjon
├── opplysninger/    # Opplysningsdomene (fakta, regler)
├── uuid-v7/         # UUID v7-generering
├── buildSrc/        # Felles Gradle-konfigurasjon
├── .nais/           # Nais-manifest og miljøvariabler
└── docs/            # Dokumentasjon
```

## Build & Test Commands

Prosjektet bruker Gradle og [ApprovalTests](https://approvaltests.com/) for testing.

```bash
# Bygg hele prosjektet
./gradlew build

# Kjør alle tester
./gradlew test

# Kjør tester i en spesifikk modul
./gradlew :dagpenger:test
./gradlew :mediator:test
./gradlew :opplysninger:test

# Formater kode (kjøres automatisk ved kompilering)
./gradlew ktlintFormat

# Sjekk formatering
./gradlew ktlintCheck
```

## ApprovalTests — viktig for AI-agenter

ApprovalTests sammenligner testoutput mot godkjente referansefiler (`.approved.txt`).
Når output endrer seg, genereres en `.received.txt`-fil og testen feiler.

### Når tester feiler etter kodeendringer

1. **Sjekk om det finnes nye `.received.txt`-filer** — dette betyr at testoutput har endret seg.
2. **Sammenlign `.received.txt` med tilhørende `.approved.txt`** for å forstå hva som har endret seg.
3. **Hvis endringene er forventet og korrekte**: Kopier innholdet fra `.received.txt` til `.approved.txt` (eller erstatt filen).
4. **Hvis endringene er uventede**: Undersøk kodeendringene som forårsaket avviket.

### Filkonvensjoner

- `.approved.txt` — Godkjent referanseoutput (committes til git).
- `.received.txt` — Faktisk testoutput ved avvik (skal **ikke** committes).

### Vanlige feil

- **Ikke anta at testen er ødelagt** bare fordi den feiler. Sjekk alltid `.received.txt` først.
- **Ikke slett `.approved.txt`-filer** — de er referansedata for testene.
- Etter kodeendringer som påvirker output, må `.approved.txt` oppdateres for at testene skal passere.

## Code Standards

- Kotlin coding conventions
- ktlint for formatering (kjøres automatisk før kompilering)
- JUnit 5 med `useJUnitPlatform()` for alle tester
- Kotest assertions (`shouldBe`, `shouldNotBe`, etc.)
- Cucumber/Gherkin for BDD-tester i `dagpenger`-modulen
- Approval Tests for snapshot-testing

## Key Concepts

- **Opplysninger**: Fakta og beregnede verdier som brukes i behandling
- **Avklaring**: Spørsmål som må besvares for å fullføre behandling
- **DAG**: Rettet asyklisk graf som beskriver avhengigheter mellom opplysninger
- **Rapids & Rivers**: Event-drevet arkitektur for Kafka-meldinger
- **Behandling**: Prosessen fra hendelse til vedtak

## Deployment

- Plattform: Nais (Kubernetes på GCP)
- Manifest: `.nais/nais.yaml`
- Miljøer: dev (`vars-dev.yaml`) og prod (`vars-prod.yaml`)
- Endepunkter: `/isalive`, `/isready`, `/metrics`
- Database: Cloud SQL PostgreSQL 17

## Boundaries

### ✅ Always

- Følg eksisterende kodemønstre og modulstruktur
- Bruk parameteriserte spørringer for databasetilgang
- Skriv tester for all ny funksjonalitet
- Kjør `./gradlew build` for å verifisere endringer

### ⚠️ Ask First

- Endringer i autentisering/autorisasjon (Azure AD)
- Endringer i Nais-manifest eller produksjonskonfigurasjon
- Nye avhengigheter eller moduler
- Endringer i Kafka-topics eller meldingsformat

### 🚫 Never

- Commit hemmeligheter eller credentials
- Hopp over input-validering
- Endre databasemigrasjoner som allerede er kjørt i produksjon
- Bypass sikkerhetskontroller
