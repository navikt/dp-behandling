# BeregnMeldekortHendelse: flyt, regler og beregning

Denne dokumentasjonen beskriver hva som faktisk skjer i kode når et meldekort beregnes: fra `BeregnMeldekortHendelse` til `BeregningsperiodeFabrikk` og `Beregningsperiode`.

## Innholdsfortegnelse
- [Overordnet flyt](#overordnet-flyt)
- [Detaljert hendelsesflyt](#detaljert-hendelsesflyt)
- [BeregningsperiodeFabrikk](#beregningsperiodefabrikk)
- [Beregningsperiode](#beregningsperiode)
- [Avklaringer og valideringer](#avklaringer-og-valideringer)
- [Dataflyt og opplysninger](#dataflyt-og-opplysninger)
- [Nøkkelkonsepter](#nøkkelkonsepter)
- [Referanser til kode](#referanser-til-kode)

---

## Overordnet flyt

```mermaid
flowchart TD
    A[BeregnMeldekortHendelse] --> B[Opprett ny Behandling basert på forrige]
    B --> C[Legg til meldeperiode + hendelsestype]
    C --> D[Legg til avklaringer]
    D --> E[Konverter meldekortdager til opplysninger]
    E --> F{harBeregnetPeriodenEtterDenne}
    F -->|Nei| G[Meldekortprosess]
    F -->|Ja| H[Omgjøringsprosess]
    G --> I[MeldekortBeregningPlugin]
    H --> J[OmgjøringBeregningPlugin]
    I --> K[BeregningsperiodeFabrikk]
    J --> K
    K --> L[Beregningsperiode]
    L --> M[Legg til periode- og dagsopplysninger]
    M --> N[Kvotetelling]
    G --> O[TaptArbeidstidStans]

    style A fill:#e1f5ff
    style M fill:#c8e6c9
    style N fill:#c8e6c9
    style F fill:#fff9c4
    style O fill:#fff9c4
```

## Detaljert hendelsesflyt

```mermaid
sequenceDiagram
    participant H as BeregnMeldekortHendelse
    participant B as Behandling
    participant P as Meldekortprosess/Omgjøringsprosess
    participant MB as MeldekortBeregningPlugin
    participant F as BeregningsperiodeFabrikk
    participant BP as Beregningsperiode

    H->>B: Opprett behandling (krever forrige behandling)
    H->>B: Legg til hendelsetype + meldeperiode
    H->>B: Legg til avklaringer
    H->>B: Legg til meldekort.tilOpplysninger(...)
    H->>P: Start regelkjøring

    P->>MB: beregnForPeriode(...)
    MB->>MB: meldtITide = antallIkkeMeldtDager < terskel(8)
    MB->>F: lagBeregningsperiode()
    F->>F: hentMeldekortDagerMedRett()
    F->>F: hentGjenståendeEgenandel()
    F->>BP: new Beregningsperiode(...)
    BP-->>MB: Beregningresultat
    MB->>B: Legg til forbruktEgenandel/utbetalingForPeriode/...
    MB->>B: Legg til forbruk + utbetaling per dag
```

## BeregningsperiodeFabrikk

```mermaid
flowchart TD
    A[lagBeregningsperiode] --> B[hentMeldekortDagerMedRett]
    B --> C{meldtITide?}
    C -->|Ja| D[Bruk alle dager med løpende rett]
    C -->|Nei| E[Bruk bare dager meldt=true]
    D --> F[opprettPeriode]
    E --> F
    F --> G{Dagstype}
    G -->|Hverdag| H{arbeidsdag=true?}
    G -->|Helg| I[Helgedag med timerArbeidet]
    H -->|Ja| J[Arbeidsdag: sats + fva + timer + terskel]
    H -->|Nei| K[Fraværsdag]
    J --> L[hentGjenståendeEgenandel]
    K --> L
    I --> L
    L --> M[stønadsdagerIgjen = antallStønadsdager - forbrukte dager]
    M --> N[Returner Beregningsperiode]

    style A fill:#e1f5ff
    style N fill:#c8e6c9
    style C fill:#fff9c4
    style G fill:#fff9c4
    style H fill:#fff9c4
```

Viktige regler:
- Dager med rett hentes fra `KravPåDagpenger.harLøpendeRett`.
- Ved for sen melding tas bare dager med `Beregning.meldt=true`.
- `gjenståendeEgenandel` hentes fra siste tidligere verdi, ellers innvilget egenandel.
- Hverdag blir `Arbeidsdag` eller `Fraværsdag`; helg blir `Helgedag`.

## Beregningsperiode

```mermaid
flowchart TD
    A[Init: valider maks 14 dager] --> B[sumFva + sumArbeidstimer]
    B --> C[Filtrer arbeidsdager opp til stønadsdagerIgjen]
    C --> D["prosentfaktor = (sumFva - timerArbeidet) / sumFva"]
    D --> E{arbeidsdager tom?}
    E -->|Ja| F[ingenArbeidsdager]
    E -->|Nei| G{oppfyller krav til tapt arbeidstid?}
    G -->|Nei| H[ingenUtbetaling]
    G -->|Ja| I[Grupper arbeidsdager per sats]
    I --> J[Brutto per gruppe = sats * dager * prosentfaktor]
    J --> K[Fordel egenandel proporsjonalt per gruppe]
    K --> L[Fordel netto på dager, rest på siste dag]
    L --> M[Beregningresultat]

    style A fill:#e1f5ff
    style M fill:#c8e6c9
    style E fill:#fff9c4
    style G fill:#fff9c4
    style H fill:#ffcdd2
```

Formler:
- `oppfyllerKravTilTaptArbeidstid = (timerArbeidet / sumFva) <= (100 - snittTerskel) / 100`
- `bruttoBeløp = sats * antallDager * prosentfaktor`
- `egenandelForGruppe = min(gruppeBrutto, avrundet(gjenståendeEgenandel * (gruppeBrutto / totalBrutto)))`
- `nettoPerDag`: jevn fordeling, øre-rest legges på siste dag

## Avklaringer og valideringer

Fra `BeregnMeldekortHendelse`:
- `requireNotNull(forrigeBehandling)`
- `MeldekortBehandling` når meldekort ikke er korrigering
- `KorrigertMeldekortBehandling` når `korrigeringAv != null`
- `KorrigeringUtbetaltPeriode` når siste beregnede periode slutter etter `meldekort.tom`
- `MeldekortMedUtdanning` når minst én aktivitet er `Utdanning` (`kanKvitteres = true`)

## Dataflyt og opplysninger

```mermaid
flowchart LR
    A[Meldekort.dager] --> B[Meldekort.tilOpplysninger]
    B --> C[arbeidsdag/arbeidstimer/meldt per dato]
    B --> D[meldedato + oppfyllerMeldeplikt=true]
    C --> E[MeldekortBeregningPlugin]
    D --> E
    E --> F[meldtITide for perioden]
    E --> G[Beregningsperiode-resultat]
    G --> H[forbruktEgenandel/utbetalingForPeriode/gjenståendeEgenandel]
    G --> I[oppfyllerKravTilTaptArbeidstidIPerioden/sumFva/sumArbeidstimer/prosentfaktor]
    G --> J[forbruk + utbetaling per dag]

    style A fill:#e1f5ff
    style H fill:#c8e6c9
    style I fill:#c8e6c9
    style J fill:#c8e6c9
```

## Nøkkelkonsepter

- **Meldekortprosess**: beregner én meldeperiode, kjører kvotetelling og stans-vurdering.
- **Omgjøringsprosess**: re-beregner alle meldeperioder i kronologisk rekkefølge.
- **Meldt i tide**: beregnes som `antallIkkeMeldtDager < 8`.
- **Forbruk**: settes per dag basert på `forbruksdager` fra `Beregningresultat`.
- **Stans ved manglende tapt arbeidstid**: hvis antall siste påfølgende perioder uten tapt arbeidstid er større eller lik terskel (`maksAntallPerioderMedIkkeTaptArbeidstid`), legges `kravTilTaptArbeidstid=false` fra første periode i rekken og prosessen ber om rekjøring.

## Avrunding og øreslipping

### Avrundingsstrategi

Systemet bruker **HALF_UP**-avrunding (standard bankrunding) til hele øre (0 desimaler). Dette betyr:
- 12,4 kr → 12 kr (avrund ned)
- 12,5 kr → 13 kr (avrund opp)
- 12,6 kr → 13 kr (avrund opp)

### Hvor avrunding skjer

1. **Egenandel per satsgruppe** (Beregningsperiode.kt, linje 98)
   ```kotlin
   val beregnetEgenandel = Beløp(gjenståendeEgenandel.verdien * andel).avrundetBeløp
   ```
   Når gjenstående egenandel fordeles proporsjonalt på flere satsgrupper.

2. **Netto per satsgruppe** (Beregningsperiode.kt, linje 73)
   ```kotlin
   val netto = (gruppe.bruttoBeløp - egenandelForGruppe).avrundetBeløp
   ```
   Brutto minus egenandel avrundet før fordeling på dager.

### Øreslipping (rest-fordeling)

Når ett beløp fordeles på flere dager, blir resten av avrundingen lagt på **siste dag i gruppen**:

```kotlin
fun fordelPåDager(beløp: Beløp): List<Beregningresultat.Forbruksdag> {
    val antall = arbeidsdager.size.toBigDecimal()
    val rest = Beløp(beløp.verdien % antall)          // Beregn rest (øre)
    val dagsbeløp = (beløp - rest) / Beløp(antall)   // Del ut likt, minus rest
    return arbeidsdager.mapIndexed { index, dag ->
        val erSisteDag = index == arbeidsdager.lastIndex
        // Siste dag får sitt dagsbeløp PLUSS resten
        Beløp(if (erSisteDag) dagsbeløp + rest else dagsbeløp)
    }
}
```

### Eksempel

Gitt en satsgruppe med 12 kr total og 5 arbeidsdager:
- Rest: 12 % 5 = 2 kr
- Dagsbeløp per dag: (12 - 2) / 5 = 2 kr
- Dag 1-4: 2 kr hver = 8 kr
- Dag 5 (siste): 2 kr + 2 kr (rest) = 4 kr
- **Totalt**: 8 + 4 = 12 kr ✓

### Presisjon før avrunding

Alle melomberegninger foretas med ubegrenset presisjon (`MathContext.UNLIMITED` i Beløp-klassen) for å sikre nøyaktighet. Avrunding gjøres kun når beløpene skal legges til behandlinger og registreres.

### Scenario 1: Satsendring midt i periode

**Situasjon:** Sats skifter fra 1516 kr til 1517 kr den 01.01.2026 (barnetillegg justeres ved nyttår).

| Parameter | Verdi |
|-----------|-------|
| Periode | 22.12.2025 – 04.01.2026 |
| Terskel | 50% |
| FVA | 37,5 t/uke = 7,5 t/dag |
| Arbeidsdager | 10 (8 dager med sats 1516, 2 dager med sats 1517) |
| Arbeid | 22 timer totalt (8.5t + 7.5t + 6.0t på helgedag) |
| Egenandel | 0 kr |

**Beregning:**
1. **Terskelsjekk**: 22,0 / 75,0 = 29,3% ≤ 50% ✓
2. **Prosentfaktor**: (75 − 22) / 75 = 70,67%
3. **Bøtte 1** (sats 1516, 8 dager): 1516 × 8 × 0,7067 = 8570 kr
4. **Bøtte 2** (sats 1517, 2 dager): 1517 × 2 × 0,7067 = 2144 kr
5. **Fordeling Bøtte 1**: 8570 % 8 = 2 kr rest → 7 dager × 1071 kr + 1 dag × 1073 kr
6. **Fordeling Bøtte 2**: 2144 % 2 = 0 kr rest → 2 dager × 1072 kr

**Resultat:**

| Dato | Sats | Utbetaling |
|------|------|-----------|
| 22.12–30.12 (dager 1–7) | 1516 | 1 071 kr |
| 31.12 (siste dag, bøtte 1) | 1516 | 1 073 kr ← +2 kr rest |
| 01.01–02.01 (bøtte 2) | 1517 | 1 072 kr |
| **Totalt** | — | **10 714 kr** |

**Nøkkelpoeng:** Resten (2 kr) fra bøtte 1 havner på **31.12** (siste dag i bøtten), ikke på 01.01 eller senere.

### Scenario 2: Desimaltall og reminder-fordeling

**Situasjon:** 903 kr × 10 dager × prosentfaktor gir desimaltall som må rettes.

| Parameter | Verdi |
|-----------|-------|
| Periode | 06.01.2020 – 17.01.2020 |
| Terskel | 50% |
| FVA | 37,5 t/uke = 7,5 t/dag |
| Arbeidsdager | 10 |
| Arbeid | 7 timer totalt (3t + 2t + 2t) |
| Egenandel | 0 kr |

**Beregning:**
1. **Terskelsjekk**: 7,0 / 75,0 = 9,3% ≤ 50% ✓
2. **Prosentfaktor**: (75 − 7) / 75 = 90,67%
3. **Brutto**: 903 × 10 × 0,9067 = 8187,20 kr → avrund → 8187 kr
4. **Fordeling**: 8187 % 10 = 7 kr rest → 9 dager × 818 kr + 1 dag × 825 kr

**Resultat:**

| Dag | Dato | Utbetaling |
|-----|------|-----------|
| 1–9 | 06.01–16.01 | 818 kr |
| 10 | 17.01 | 825 kr ← +7 kr rest |
| **Totalt** | — | **8 187 kr** |

**Nøkkelpoeng:** 903 × 10 × 68/75 = 8187,20 → 8187 kr, og de 7 kr av resten legges på siste dag.

---

## Referanser til kode

| Konsept | Kodefil |
|---------|---------|
| Start av meldekortberegning | `dagpenger/src/main/kotlin/no/nav/dagpenger/regel/hendelse/BeregnMeldekortHendelse.kt` |
| Meldekort → opplysninger | `dagpenger/src/main/kotlin/no/nav/dagpenger/regel/hendelse/MeldekortDagerTilOpplysning.kt` |
| Standard prosess | `dagpenger/src/main/kotlin/no/nav/dagpenger/regel/prosess/Meldekortprosess.kt` |
| Omgjøringsprosess | `dagpenger/src/main/kotlin/no/nav/dagpenger/regel/prosess/Omgjøringsprosess.kt` |
| Meldekortberegning-plugin | `dagpenger/src/main/kotlin/no/nav/dagpenger/regel/prosess/MeldekortBeregningPlugin.kt` |
| Stans-plugin | `dagpenger/src/main/kotlin/no/nav/dagpenger/regel/prosess/TaptArbeidstidStans.kt` |
| Periodebygging | `dagpenger/src/main/kotlin/no/nav/dagpenger/regel/regelsett/beregning/BeregningsperiodeFabrikk.kt` |
| Selve beregningen | `dagpenger/src/main/kotlin/no/nav/dagpenger/regel/regelsett/beregning/Beregningsperiode.kt` |
| Opplysningstyper for beregning | `dagpenger/src/main/kotlin/no/nav/dagpenger/regel/regelsett/beregning/Beregning.kt` |
| Dag-modell brukt i beregning | `dagpenger/src/main/kotlin/no/nav/dagpenger/regel/regelsett/beregning/Dag.kt` |
