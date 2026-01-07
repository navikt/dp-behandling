# Beregning av Meldekort - Flyt og Regler

Denne dokumentasjonen beskriver hvordan BeregnMeldekortHendelse behandles, og hvilke regler som anvendes i BeregningsperiodeFabrikk og Beregningsperiode.

## Innholdsfortegnelse
- [Overordnet flyt](#overordnet-flyt)
- [Detaljert hendelsesflyt](#detaljert-hendelsesflyt)
- [BeregningsperiodeFabrikk - Regler](#beregningsperiodefabrikk---regler)
- [Beregningsperiode - Beregningslogikk](#beregningsperiode---beregningslogikk)
- [Avklaringer](#avklaringer)
- [Dataflyt og opplysninger](#dataflyt-og-opplysninger)
- [Nøkkelkonsepter](#nøkkelkonsepter)

---

## Overordnet flyt

```mermaid
graph TB
    A[BeregnMeldekortHendelse] --> B[Opprett Behandling]
    B --> C[Konverter Meldekort til Opplysninger]
    C --> D[Meldekortprosess.regelkjøring]
    D --> E[BeregningsperiodeFabrikk]
    E --> F[Beregningsperiode]
    F --> G[Beregningsresultat]
    G --> H[Lagre opplysninger]
    H --> I[Kvotetelling]
    
    style A fill:#e1f5ff
    style G fill:#c8e6c9
    style I fill:#fff9c4
```

---

## Detaljert hendelsesflyt

```mermaid
sequenceDiagram
    participant MK as Meldekort
    participant BMH as BeregnMeldekortHendelse
    participant BEH as Behandling
    participant MKP as Meldekortprosess
    participant BPF as BeregningsperiodeFabrikk
    participant BP as Beregningsperiode
    participant KT as Kvotetelling
    
    MK->>BMH: meldekort innsendt
    BMH->>BMH: Valider hendelse
    BMH->>BEH: Opprett behandling basert på forrige behandling
    BMH->>BEH: Legg til hendelsetype og meldeperiode opplysninger
    BMH->>BMH: Sjekk om korrigering
    
    alt Er korrigering
        BMH->>BEH: Legg til avklaring "KorrigertMeldekortBehandling"
        BMH->>BMH: Sjekk om periode allerede beregnet
        alt Periode allerede utbetalt
            BMH->>BEH: Legg til avklaring "KorrigeringUtbetaltPeriode"
        end
    else Ikke korrigering
        BMH->>BEH: Legg til avklaring "MeldekortBehandling"
    end
    
    BMH->>BMH: Sjekk aktiviteter
    alt Har utdanning
        BMH->>BEH: Legg til avklaring "MeldekortMedUtdanning"
    end
    
    BMH->>BEH: Konverter meldekort.tilOpplysninger()
    Note over BMH,BEH: Hver dag i meldekortet blir til opplysninger:<br/>arbeidsdag, arbeidstimer, meldt
    
    BEH->>MKP: Start regelkjøring
    MKP->>MKP: Bestem regelverksdato og prøvingsperiode
    MKP->>BPF: lagBeregningsperiode()
    
    BPF->>BPF: hentMeldekortDagerMedRett()
    BPF->>BPF: opprettPeriode(dager)
    BPF->>BPF: hentGjenståendeEgenandel()
    BPF->>BP: new Beregningsperiode(...)
    
    BP->>BP: beregnProsentfaktor()
    BP->>BP: beregnUtbetaling()
    BP-->>MKP: Beregningresultat
    
    MKP->>BEH: Legg til resultat som opplysninger
    Note over MKP,BEH: forbruktEgenandel, utbetalingForPeriode,<br/>gjenståendeEgenandel, oppfyllerKravTilTaptArbeidstid,<br/>sumFva, sumArbeidstimer, prosentfaktor
    
    MKP->>BEH: Legg til per-dag opplysninger (forbruk, utbetaling)
    MKP->>KT: regelkjøringFerdig()
    KT->>BEH: Beregn og legg til forbrukt/gjenstående dager
```

---

## BeregningsperiodeFabrikk - Regler

BeregningsperiodeFabrikk er ansvarlig for å bygge opp en beregningsperiode basert på meldekortets dager og eksisterende opplysninger.

```mermaid
flowchart TD
    START[lagBeregningsperiode] --> A[hentMeldekortDagerMedRett]
    
    A --> A1[Hent perioder med løpende rett]
    A1 --> A2[Sjekk om meldt i tide]
    
    A2 --> A3{Meldt i tide?}
    A3 -->|Ja| A4[Alle dager i meldeperioden med rett]
    A3 -->|Nei| A5[Kun dager som er eksplisitt meldt]
    
    A4 --> B[opprettPeriode]
    A5 --> B
    
    B --> B1{Kategoriser hver dag}
    
    B1 --> B2{Dagstype?}
    B2 -->|Hverdag| B3[opprettArbeidsdagEllerFraværsdag]
    B2 -->|Helg| B4[Opprett Helgedag]
    
    B3 --> B5{Er arbeidsdag?}
    B5 -->|Ja| B6[Opprett Arbeidsdag<br/>med sats, FVA, timer, terskel]
    B5 -->|Nei| B7[Opprett Fraværsdag]
    
    B6 --> C
    B7 --> C
    B4 --> C
    
    C[hentGjenståendeEgenandel] --> C1{Tidligere gjenstående<br/>egenandel?}
    C1 -->|Ja| C2[Bruk siste registrerte<br/>før meldeperioden]
    C1 -->|Nei| C3[Bruk innvilget egenandel]
    
    C2 --> D
    C3 --> D
    
    D[Beregn stønadsdager igjen] --> D1[antallStønadsdager - forbrukte dager]
    
    D1 --> E[Returner Beregningsperiode]
    
    style START fill:#e1f5ff
    style E fill:#c8e6c9
    style B5 fill:#fff9c4
    style A3 fill:#fff9c4
    style C1 fill:#fff9c4
```

### Viktige regler i BeregningsperiodeFabrikk

1. **Dagsklassifisering**
   - Mandag-Fredag = Hverdag (kan være arbeidsdag eller fraværsdag)
   - Lørdag-Søndag = Helgedag

2. **Arbeidsdag vs Fraværsdag**
   - Arbeidsdag: Krever opplysning om arbeidsdag=true, henter dagsats, FVA, arbeidstimer og terskel
   - Fraværsdag: Når arbeidsdag=false (typisk syk/fravær)

3. **Dager med rett**
   - Filtreres basert på perioder med løpende rett (harLøpendeRett)
   - Hvis ikke meldt i tide: kun dager eksplisitt meldt inkluderes
   - Terskel for "meldt i tide": maks 8 dager ikke meldt

4. **Gjenstående egenandel**
   - Hentes fra siste registrerte egenandel før denne perioden
   - Fallback: innvilget egenandel

---

## Beregningsperiode - Beregningslogikk

Beregningsperiode utfører selve beregningen av utbetaling basert på dagene fra fabrikken.

```mermaid
flowchart TD
    START[Beregningsperiode initialiseres] --> V1[Valider max 14 dager]
    V1 --> A[Beregn sumFva]
    A --> B[Beregn timerArbeidet]
    B --> C[Filtrer arbeidsdager<br/>basert på stønadsdager igjen]
    C --> D[Beregn prosentfaktor]
    
    D --> E[beregnUtbetaling]
    
    E --> F{Oppfyller krav til<br/>tapt arbeidstid?}
    
    F -->|Nei| G[Returner nullresultat<br/>Ingen utbetaling<br/>Ingen forbruk]
    
    F -->|Ja| H[Grupper arbeidsdager etter sats]
    
    H --> I[For hver satsgruppe:<br/>Beregn gradert beløp<br/>= sats × antall dager × prosentfaktor]
    
    I --> J[Beregn sum før egenandel]
    
    J --> K[Fordel egenandel på bøtter]
    K --> K1[Hver bøtte får egenandel<br/>proporsjonal med størrelse]
    
    K1 --> L[For hver bøtte:<br/>Trekk egenandel fra gradert beløp]
    
    L --> M[Fordel utbetaling per dag i bøtte]
    M --> M1[Dagsbeløp = utbetalt ÷ antall dager]
    M1 --> M2[Siste dag får dagsbeløp + rest]
    
    M2 --> N[Beregn forbrukt egenandel]
    N --> O[Beregn gjenstående egenandel]
    
    O --> P[Returner Beregningresultat]
    
    style START fill:#e1f5ff
    style P fill:#c8e6c9
    style G fill:#ffcdd2
    style F fill:#fff9c4
```

### Beregningsformler

#### 1. Prosentfaktor
```
prosentfaktor = (sumFva - timerArbeidet) / sumFva
```
Representerer hvor stor andel av arbeidstiden som er tapt.

#### 2. Krav til tapt arbeidstid
```
terskel = (100 - snittTerskel) / 100
oppfyller = (timerArbeidet / sumFva) <= terskel
```
Standard terskel er vanligvis 50% (kan arbeide inntil 50% av vanlig arbeidstid).

#### 3. Gradert satsbeløp per gruppe
```
gradertBeløp = sats × antallDager × prosentfaktor
```

#### 4. Egenandel per bøtte
```
bøtteStørrelseIProsent = bøtteSum / sumFørEgenandel
egenandelForBøtte = gjenståendeEgenandel × bøtteStørrelseIProsent
```

#### 5. Utbetaling per bøtte
```
utbetalt = gradertBeløp - egenandelForBøtte
```

#### 6. Dagsutbetaling
```
reminder = utbetalt % antallDager
dagsbeløp = (utbetalt - reminder) / antallDager
beløpSisteDag = dagsbeløp + reminder
```

### Bøttesystemet

Når arbeidsdager har forskjellige satser (f.eks. pga. barnetillegg eller satsjusteringer), grupperes de i "bøtter":

```mermaid
graph LR
    A[Arbeidsdag 1<br/>Sats: 500kr] --> B1[Bøtte 1]
    A2[Arbeidsdag 2<br/>Sats: 500kr] --> B1
    A3[Arbeidsdag 3<br/>Sats: 500kr] --> B1
    
    A4[Arbeidsdag 4<br/>Sats: 600kr] --> B2[Bøtte 2]
    A5[Arbeidsdag 5<br/>Sats: 600kr] --> B2
    
    B1 --> C1[Gradert: 1350kr<br/>Egenandel: 200kr<br/>Utbetalt: 1150kr]
    B2 --> C2[Gradert: 1080kr<br/>Egenandel: 120kr<br/>Utbetalt: 960kr]
    
    C1 --> D1[Dag 1: 383kr<br/>Dag 2: 383kr<br/>Dag 3: 384kr]
    C2 --> D2[Dag 4: 480kr<br/>Dag 5: 480kr]
    
    style B1 fill:#e1f5ff
    style B2 fill:#e1f5ff
    style C1 fill:#fff9c4
    style C2 fill:#fff9c4
```

---

## Avklaringer

BeregnMeldekortHendelse oppretter avklaringer som må behandles:

```mermaid
flowchart TD
    START{Sjekk meldekort} --> K{Er korrigering?}
    
    K -->|Ja| K1[Legg til:<br/>KorrigertMeldekortBehandling]
    K1 --> K2{Periode allerede<br/>beregnet og utbetalt?}
    K2 -->|Ja| K3[Legg til:<br/>KorrigeringUtbetaltPeriode<br/>⚠️ Kan ikke kvitteres]
    K2 -->|Nei| U
    
    K -->|Nei| M[Legg til:<br/>MeldekortBehandling]
    
    K3 --> U
    M --> U
    
    U{Har utdanning<br/>i aktiviteter?}
    U -->|Ja| U1[Legg til:<br/>MeldekortMedUtdanning<br/>✓ Kan kvitteres]
    U -->|Nei| END
    
    U1 --> END[Behandling klar]
    
    style K1 fill:#fff9c4
    style K3 fill:#ffcdd2
    style M fill:#c8e6c9
    style U1 fill:#fff9c4
```

### Avklaringskoder

| Kode | Tittel | Beskrivelse | Kan avbrytes | Kan kvitteres |
|------|--------|-------------|--------------|---------------|
| **MeldekortBehandling** | Beregning av meldekort | Behandlingen er opprettet av meldekort og kan ikke automatisk behandles | Nei | Nei |
| **KorrigertMeldekortBehandling** | Beregning av korrigert meldekort | Behandlingen er korrigering av et tidligere meldekort og kan ikke automatisk behandles | Nei | Nei |
| **KorrigeringUtbetaltPeriode** | Beregning av meldekort som korrigerer tidligere periode | Behandlingen er korrigering av et tidligere meldekort og kan ikke behandles | Nei | **Nei** |
| **MeldekortMedUtdanning** | Meldekort med utdanning | Bruker har krysset av for utdanning eller tiltak på meldekortet. Må vurderes manuelt. | Nei | **Ja** |

---

## Dataflyt og opplysninger

### Meldekort til opplysninger

Meldekortet konverteres til opplysninger via `Meldekort.tilOpplysninger()`:

```mermaid
flowchart LR
    MK[Meldekort] --> D1[Dag 1]
    MK --> D2[Dag 2]
    MK --> DN[Dag N]
    MK --> MD[meldedato]
    
    D1 --> A1[aktiviteter]
    A1 --> AV{Aktivitetstype}
    
    AV -->|Fravær/Syk| F1[arbeidsdag=false<br/>arbeidstimer=0]
    AV -->|Arbeid/Utdanning| F2[arbeidsdag=true<br/>arbeidstimer=sumTimer]
    AV -->|Ingen aktivitet| F3[arbeidsdag=true<br/>arbeidstimer=0]
    
    D1 --> M1[meldt opplysning]
    
    F1 --> OPP[Opplysninger]
    F2 --> OPP
    F3 --> OPP
    M1 --> OPP
    MD --> OPP
    
    OPP --> TI{Antall ikke-meldte<br/>dager < 8?}
    TI -->|Ja| TI1[meldtITide=true]
    TI -->|Nei| TI2[meldtITide=false]
    
    TI1 --> FINAL[Komplette opplysninger]
    TI2 --> FINAL
    
    style MK fill:#e1f5ff
    style FINAL fill:#c8e6c9
    style AV fill:#fff9c4
```

### Opplysninger lagt til av beregning

```mermaid
mindmap
  root((Beregningsopplysninger))
    Periode-opplysninger
      meldeperiode
      forbruktEgenandel
      utbetalingForPeriode
      gjenståendeEgenandel
      oppfyllerKravTilTaptArbeidstidIPerioden
      sumFva
      sumArbeidstimer
      prosentfaktor
    Per-dag-opplysninger
      forbruk
      utbetaling
      arbeidsdag
      arbeidstimer
      meldt
      terskel
    Kvotetelling-opplysninger
      forbrukt
      gjenståendePeriode
```

---

## Nøkkelkonsepter

### 1. Stønadsdager
- **Innvilget**: Totalt antall stønadsdager ved innvilgelse (typisk 52, 104 uker)
- **Forbrukt**: Akkumulert antall dager hvor det har vært forbruk
- **Gjenstående**: Innvilget - forbrukt
- **Forbruk skjer**: Kun på arbeidsdager hvor krav til tapt arbeidstid er oppfylt

### 2. Egenandel
- **Innvilget egenandel**: Beløp som må dekkes før full utbetaling (f.eks. 3000kr)
- **Gjenstående egenandel**: Oppdateres for hver meldeperiode
- **Forbrukt egenandel**: Hvor mye som trekkes i denne perioden
- **Fordeling**: Når flere satser, fordeles egenandel proporsjonalt

### 3. Fastsatt vanlig arbeidstid (FVA)
- Brukerens normale arbeidstid per uke (fordelt per dag: FVA/5)
- Brukes som baseline for å beregne tapt arbeidstid

### 4. Prosentfaktor
- Andel av arbeidstiden som er tapt: `(FVA - arbeidetTimer) / FVA`
- Brukes til å gradere dagpengesatsen
- Eksempel: FVA=37.5t, arbeidet=10t → prosentfaktor=0.73 (73% tapt)

### 5. Terskel for arbeidstidsreduksjon
- Standard 50%: Kan jobbe inntil 50% av vanlig arbeidstid og fortsatt få dagpenger
- Hvis terskelen overskrides: ingen utbetaling for perioden
- Snitt-terskel beregnes over alle arbeidsdager i perioden

### 6. Meldt i tide
- Meldekort skal meldes innen fristen
- Terskel: Max 8 dager ikke meldt i en periode
- Hvis ikke meldt i tide: kun eksplisitt meldte dager får rett

### 7. Dager med rett
- Må ha løpende rett i perioden (basert på tidligere vedtak)
- Må være meldt (enten i tide, eller eksplisitt per dag)
- Meldeperiode må overlappe med rettighetsperiode

---

## Eksempelberegning

### Scenario
- **Meldeperiode**: 14 dager (2 uker)
- **Arbeidsdager**: 10 dager (mandag-fredag)
- **FVA**: 37.5 timer/uke = 7.5 timer/dag
- **Dagsats**: 500 kr
- **Gjenstående egenandel**: 300 kr
- **Terskel**: 50%

### Periode med arbeid
- **Dag 1-5**: Arbeidet 3 timer/dag (total 15t)
- **Dag 6-10**: Arbeidet 0 timer/dag (total 0t)

### Beregning
```
sumFva = 10 dager × 7.5t = 75t
timerArbeidet = 15t
prosentfaktor = (75t - 15t) / 75t = 0.8 (80%)

Oppfyller terskel? 15t / 75t = 0.2 (20%) < 0.5 ✓

Gradert beløp = 500kr × 10 dager × 0.8 = 4000kr
Egenandel = 300kr
Utbetalt = 4000kr - 300kr = 3700kr

Reminder = 3700 % 10 = 0
Dagsbeløp = 3700 / 10 = 370kr/dag

Dag 1-9: 370kr
Dag 10: 370kr
```

### Resultat
- **Utbetaling totalt**: 3700 kr
- **Forbrukt egenandel**: 300 kr
- **Gjenstående egenandel**: 0 kr
- **Forbruksdager**: 10 dager
- **Oppfyller krav**: Ja

---

## Spesialtilfeller

### Korrigering av tidligere periode
```mermaid
flowchart TD
    K[Mottatt korrigert meldekort] --> K1{Periode allerede beregnet?}
    K1 -->|Nei| K2[Normal behandling med avklaring]
    K1 -->|Ja| K3{Periode utbetalt?}
    K3 -->|Nei| K4[Behandling med avklaring,<br/>kan beregnes på nytt]
    K3 -->|Ja| K5[⚠️ Blokkerende avklaring<br/>Omgjøring bak i tid<br/>Kan ikke kvitteres]
    
    style K5 fill:#ffcdd2
```

### Ikke oppfyller krav til tapt arbeidstid
Når terskelen overskrides:
- **Ingen utbetaling** for perioden
- **Ingen forbruk** av stønadsdager
- **Ingen trekk** i egenandel
- Opplysningene registreres likevel for dokumentasjon

### Flere satser i samme periode
Eksempel: Barnetillegg starter midt i perioden
- Dag 1-7: sats 500kr
- Dag 8-10: sats 600kr (med barnetillegg)

Systemet oppretter to bøtter og fordeler egenandel proporsjonalt.

---

## Referanser til kode

| Konsept | Kodefil |
|---------|---------|
| Hendelsesbehandling | `BeregnMeldekortHendelse.kt` |
| Meldekort-modell | `MeldekortInnsendtHendelse.kt` |
| Konvertering til opplysninger | `MeldekortDagerTilOpplysning.kt` |
| Prosesstyring | `Meldekortprosess.kt` |
| Periode-oppbygging | `BeregningsperiodeFabrikk.kt` |
| Beregningslogikk | `Beregningsperiode.kt` |
| Opplysningstyper | `Beregning.kt` |
| Dag-modeller | `Dag.kt` |

---

*Dokumentasjon generert basert på kodebase per januar 2026*
