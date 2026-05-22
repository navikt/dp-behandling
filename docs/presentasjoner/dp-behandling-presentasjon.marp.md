---
marp: true
theme: default
paginate: true
title: dp-behandling forklart for ikke-utviklere
description: Grunnprinsipper i opplysning, opplysninger, regelkjøring og behandling
---
# Hva er dp-behandling?

dp-behandling er motoren som:

- tar imot en hendelse (for eksempel søknad),
- samler inn opplysninger,
- kjører regler,
- vurderer avklaringer,
- og avslutter i vedtak eller forslag til vedtak.


---

# Seks lag i modellen

1. **Opplysning**: ett konkret faktum eller vurdert verdi.
2. **Opplysninger**: hele samlingen av fakta.
3. **Regelkjøring**: utleder nye opplysninger.
4. **Regelverk**: hvilke regler som gjelder.
5. **Behandling**: prosessen og tilstandene i saken.
6. **Hendelse**: det som starter/driver saken videre.

[tegninger](https://excalidraw.com/#json=3QLZSA8Wyiv_wLrssvrxA,Z-Ue5OLUZsbOCzhTic9w-g)

---

# Løkmodellen (innerst til ytterst)

```mermaid
flowchart LR
    A[Opplysning] --> B[Opplysninger]
    B --> C[Regelkjøring]
    C --> D[Regelverk]
    D --> E[Behandling]
    E --> F[Hendelse]
```

Lesning: Innerst er enkeltfakta, ytterst er hendelser som setter hele kjeden i bevegelse.

---

# Hva er en opplysning?

En opplysning i dp-behandling har blant annet:

- type (hva slags opplysning),
- verdi,
- gyldighetsperiode,
- kilde (ekstern, intern, utledet),
- sporbarhet (hva den kommer fra).

Systemet skiller også mellom **hypotese** (foreløpig) og **faktum** (bekreftet).

---

# Gyldighetsperiode: når en opplysning gjelder

Gyldighetsperiode betyr at en opplysning gjelder i et bestemt tidsrom.

Eksempel:
- **Registrert som arbeidssøker** kan være gyldig fra en dato.
- **Arbeidsinntekt siste 12 måneder** vurderes for en bestemt prøvingsdato.

Hvorfor viktig:
- Regelkjøring bruker opplysninger som er gyldige på riktig dato.
- Samme person kan ha ulike gyldige opplysninger i ulike perioder.

---

# Konkrete opplysninger i dagpenger-regelverket

Eksempler brukt i regelverket:

- **Søknadsdato**
- **Prøvingsdato**
- **Registrert som arbeidssøker**
- **Bostedsland er Norge**
- **Arbeidsinntekt siste 12 måneder**
- **Arbeidsinntekt siste 36 måneder**
- **Oppfyller kravet til minsteinntekt**
- **Dagpengegrunnlag**
- **Dagsats med barnetillegg etter samordning og 90 % regel**
- **Antall stønadsuker (stønadsperiode)**

---

# Input-opplysninger og utledede opplysninger

Input (kommer inn via behov/svar):

- Registrert som arbeidssøker
- Bostedsland er Norge
- Inntektsopplysninger
- Ønsket arbeidstid

Utledet (beregnes i regelkjøring):

- Oppfyller kravet til opphold i Norge eller unntak
- Oppfyller kravet til minsteinntekt
- Reell arbeidssøker
- Dagpengegrunnlag
- Antall stønadsuker (stønadsperiode)

---

# Regelkjøring i praksis

Regelkjøringen går i sløyfer:

1. Aktiverer regler for aktuell dato.
2. Lager plan for hvilke opplysninger som må produseres.
3. Utleder nye opplysninger med interne regler.
4. Stopper når en ekstern opplysning mangler.
5. Sender behov.
6. Fortsetter når svar kommer inn.

Resultatet er enten ferdig vurdering eller presist informasjonsbehov.

---

# Eksempel: fra inntekt til minsteinntekt

```mermaid
flowchart LR
    A[Inntektsopplysninger] --> B[Brutto arbeidsinntekt]
    B --> C[Arbeidsinntekt siste 12 måneder]
    B --> D[Arbeidsinntekt siste 36 måneder]
    E[Grunnbeløp] --> F[Inntektskrav for siste 12 måneder]
    E --> G[Inntektskrav for siste 36 måneder]
    C --> H[Arbeidsinntekt er over kravet for siste 12 måneder]
    F --> H
    D --> I[Arbeidsinntekt er over kravet for siste 36 måneder]
    G --> I
    H --> J[Oppfyller kravet til minsteinntekt]
    I --> J
```

---

# Eksempel: fra grunnlag til ytelse

```mermaid
flowchart LR
    A[Arbeidsinntekt siste 12/36 måneder] --> B[Uavrundet grunnlag]
    B --> C[Dagpengegrunnlag]
    C --> D[Dagsats uten barnetillegg før samordning]
    E[Antall barn som gir rett til barnetillegg] --> F[Sum av barnetillegg]
    D --> G[Dagsats med barnetillegg før samordning]
    F --> G
    G --> H[Dagsats med barnetillegg etter samordning og 90 % regel]
    H --> I[Ukessats med barnetillegg etter samordning]
```

---

# Behandling: prosessen rundt regelmotoren

Behandling holder styr på tilstand:

- Under opprettelse
- Under behandling
- Forslag til vedtak
- Til godkjenning / til beslutning
- Ferdig eller avbrutt

Når regelkjøring er ferdig vurderes avklaringer og om vedtak kan automatiseres.

---

# Helhetlig flyt: hendelse til vedtak

```mermaid
flowchart TD
    A[Hendelse: søknad eller meldekort] --> B[Behandling opprettes]
    B --> C[Opplysninger: arvede + nye]
    C --> D[Regelkjøring]
    D --> E{Mangler opplysninger?}
    E -->|Ja| F[Send behov]
    F --> G[Motta svar]
    G --> D
    E -->|Nei| H{Åpne avklaringer?}
    H -->|Ja| I[Forslag til vedtak]
    I --> J[Godkjenning / beslutning]
    H -->|Nei| K[Ferdig vedtak]
    J --> K
```

---

# Hvorfor denne modellen er nyttig

For fag og design:

- **Forklarbarhet**: vi kan vise hvorfor et resultat ble som det ble.
- **Sporbarhet**: vi kan følge vei fra regel til opplysning.
- **Forutsigbarhet**: samme grunnlag gir samme maskinelle vurdering.
- **Samspill menneske/maskin**: avklaringer håndterer det som ikke kan automatiseres fullt ut.

---

# Ordliste (enkelt språk)

| Begrep | Praktisk betydning |
|---|---|
| Opplysning | Ett konkret datapunkt i saken |
| Opplysninger | Hele datagrunnlaget saken bygger på |
| Regelkjøring | Motoren som vurderer og utleder |
| Avklaring | Punkt som krever manuell oppfølging/vurdering |
| Behandling | Hele saksløpet frem til vedtak |
| Vedtak | Endelig beslutning basert på behandlingen |
