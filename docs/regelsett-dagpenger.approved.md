# Regelsett - Dagpenger

Viser hvordan regelsettene avhenger av hverandre. Pilen går fra regelsettet som påvirker til regelsettet som påvirkes,
så grunnlag (f.eks. Søknadstidspunkt) ligger til venstre og resultater til høyre.

- `data` (grå): regelsettet bruker opplysninger som et annet regelsett produserer
- `skalVurderes` (rosa): regelsettets `skalVurderes` leser opplysninger fra et annet regelsett
- `påvirkerResultat` (oransje): regelsettets `påvirkerResultat` leser opplysninger fra et annet regelsett
- `kravPåDagpenger`: lesingen skjer via `kravPåDagpenger()`, som igjen leser betingelsene til alle relevante vilkår
- Gul tykk kant: portvakt, et regelsett som minst 5 andre regelsett styres av via `skalVurderes` eller `påvirkerResultat`

Leste opplysninger i `skalVurderes`/`påvirkerResultat` er funnet ved å kjøre lambdaene mot et
opptakende opplysningssett som utforsker alle utfall av hver lesing.

## Graf

```mermaid
graph LR
  classDef Vilkår fill:#1a5fb4,stroke:#0b3d7a,color:#fff
  classDef Fastsettelse fill:#2e7d32,stroke:#1b5e20,color:#fff
  classDef Prosess fill:#b26a00,stroke:#7a4900,color:#fff
  KRAV{{"kravPåDagpenger: alle relevante vilkår"}}
  R0["Alder (Vilkår)"]:::Vilkår
  R1["Meldekortberegning (Fastsettelse)"]:::Fastsettelse
  R2["Dagpengegrunnlag (Fastsettelse)"]:::Fastsettelse
  R3["Sats og barnetillegg (Fastsettelse)"]:::Fastsettelse
  R4["Periode (Fastsettelse)"]:::Fastsettelse
  R5["Egenandel (Fastsettelse)"]:::Fastsettelse
  R6["Eksport til EØS-land (Vilkår)"]:::Vilkår
  R7["Etablering (Vilkår)"]:::Vilkår
  R8["Fulle ytelser (Vilkår)"]:::Vilkår
  R9["Gjenopptak (Vilkår)"]:::Vilkår
  R10["Periode (Vilkår)"]:::Vilkår
  R11["Krav på dagpenger (Vilkår)"]:::Vilkår
  R12["Medlemmets opplysningsplikt (Vilkår)"]:::Vilkår
  R13["Meldeplikt (Vilkår)"]:::Vilkår
  R14["Minsteinntekt (Vilkår)"]:::Vilkår
  R15["Omgjøring uten klage (Prosess)"]:::Prosess
  R16["Skal omgjøring uten klage vurderes? (Prosess)"]:::Prosess
  R17["Opphold (Vilkår)"]:::Vilkår
  R18["Opptjeningsperiode (Fastsettelse)"]:::Fastsettelse
  R19["Permittering (Vilkår)"]:::Vilkår
  R20["Permittering (Fastsettelse)"]:::Fastsettelse
  R21["Permittering fiskeindustri (Vilkår)"]:::Vilkår
  R22["Permittering fiskeindustri (Fastsettelse)"]:::Fastsettelse
  R23["Reell arbeidssøker (Vilkår)"]:::Vilkår
  R24["Registrert som arbeidssøker (Vilkår)"]:::Vilkår
  R25["Rettighetstype (Fastsettelse)"]:::Fastsettelse
  R26["Samordning utenfor folketrygden (Fastsettelse)"]:::Fastsettelse
  R27["Samordning reduserte ytelser (Vilkår)"]:::Vilkår
  R28["Sanksjonsperiode (Vilkår)"]:::Vilkår
  R29["Streik og lock-out (Vilkår)"]:::Vilkår
  R30["Søknad (Vilkår)"]:::Vilkår
  R31["Søknadstidspunkt (Fastsettelse)"]:::Fastsettelse
  R32["Tap av arbeidsinntekt og arbeidstid (Vilkår)"]:::Vilkår
  R33["Tidsbegrenset bortfall (Vilkår)"]:::Vilkår
  R34["Tre meldeperioder uten tilstrekkelig tap (Vilkår)"]:::Vilkår
  R35["Uriktige opplysninger og tilbakeholdelse (Vilkår)"]:::Vilkår
  R36["Utdanning (Vilkår)"]:::Vilkår
  R37["Utestengning (Vilkår)"]:::Vilkår
  R38["Fastsettelse av arbeidstid (Fastsettelse)"]:::Fastsettelse
  R39["Verneplikt (Vilkår)"]:::Vilkår
  R40["Dagpenger ved verneplikt (Fastsettelse)"]:::Fastsettelse
  style R0 stroke:#ffd43b,stroke-width:5px
  style R14 stroke:#ffd43b,stroke-width:5px
  style R25 stroke:#ffd43b,stroke-width:5px
  style R39 stroke:#ffd43b,stroke-width:5px
  R31 -->|data| R0
  R4 -->|data| R1
  R31 -->|data| R2
  R18 -->|data| R2
  R25 -->|data| R2
  R40 -->|data| R2
  KRAV -->|skalVurderes| R2
  R0 -->|påvirkerResultat| R2
  R31 -->|data| R3
  R2 -->|data| R3
  R26 -->|data| R3
  KRAV -->|skalVurderes| R3
  KRAV -->|påvirkerResultat| R3
  R31 -->|data| R4
  R14 -->|data| R4
  R2 -->|data| R4
  KRAV -->|skalVurderes| R4
  R14 -->|påvirkerResultat| R4
  R39 -->|påvirkerResultat| R4
  R31 -->|data| R5
  R3 -->|data| R5
  R25 -->|data| R5
  KRAV -->|skalVurderes| R5
  KRAV -->|påvirkerResultat| R5
  R25 -->|data| R6
  R25 -->|skalVurderes| R6
  R25 -->|påvirkerResultat| R6
  R25 -->|data| R7
  R25 -->|skalVurderes| R7
  R25 -->|påvirkerResultat| R7
  R14 -->|skalVurderes| R8
  R39 -->|skalVurderes| R8
  R14 -->|påvirkerResultat| R8
  R39 -->|påvirkerResultat| R8
  R1 -->|data| R9
  R31 -->|data| R9
  R25 -->|data| R9
  R25 -->|skalVurderes| R9
  R4 -->|skalVurderes| R10
  R31 -->|data| R13
  R11 -->|skalVurderes| R13
  R18 -->|data| R14
  R31 -->|data| R14
  R0 -->|skalVurderes| R14
  R0 -->|påvirkerResultat| R14
  R2 -->|påvirkerResultat| R14
  R16 -->|skalVurderes| R15
  R31 -->|data| R17
  R14 -->|skalVurderes| R17
  R39 -->|skalVurderes| R17
  R14 -->|påvirkerResultat| R17
  R39 -->|påvirkerResultat| R17
  R31 -->|data| R18
  R0 -->|skalVurderes| R18
  R25 -->|data| R19
  R20 -->|data| R19
  R0 -->|skalVurderes| R19
  R25 -->|skalVurderes| R19
  R25 -->|påvirkerResultat| R19
  R25 -->|skalVurderes| R20
  R19 -->|påvirkerResultat| R20
  R31 -->|data| R21
  R25 -->|data| R21
  R14 -->|skalVurderes| R21
  R39 -->|skalVurderes| R21
  R25 -->|skalVurderes| R21
  R14 -->|påvirkerResultat| R21
  R39 -->|påvirkerResultat| R21
  R25 -->|påvirkerResultat| R21
  R31 -->|data| R22
  R21 -->|skalVurderes| R22
  R21 -->|påvirkerResultat| R22
  R31 -->|data| R23
  R0 -->|skalVurderes| R23
  R25 -->|skalVurderes| R23
  R25 -->|påvirkerResultat| R23
  R14 -->|påvirkerResultat| R23
  R39 -->|påvirkerResultat| R23
  R0 -->|påvirkerResultat| R23
  R31 -->|data| R24
  R0 -->|skalVurderes| R24
  R0 -->|påvirkerResultat| R24
  R25 -->|påvirkerResultat| R24
  R31 -->|data| R25
  R39 -->|data| R25
  R0 -->|skalVurderes| R25
  R31 -->|data| R26
  R2 -->|data| R26
  R27 -->|data| R26
  R3 -->|data| R26
  KRAV -->|skalVurderes| R26
  R14 -->|påvirkerResultat| R26
  R39 -->|påvirkerResultat| R26
  R31 -->|data| R27
  R32 -->|data| R27
  R3 -->|data| R27
  R14 -->|skalVurderes| R27
  R39 -->|skalVurderes| R27
  R31 -->|data| R28
  R4 -->|data| R28
  KRAV -->|skalVurderes| R28
  R14 -->|skalVurderes| R29
  R39 -->|skalVurderes| R29
  R14 -->|påvirkerResultat| R29
  R39 -->|påvirkerResultat| R29
  R31 -->|data| R30
  R31 -->|data| R32
  R25 -->|data| R32
  R20 -->|data| R32
  R21 -->|data| R32
  R2 -->|data| R32
  R40 -->|data| R32
  R38 -->|data| R32
  R14 -->|skalVurderes| R32
  R39 -->|skalVurderes| R32
  R4 -->|data| R33
  KRAV -->|skalVurderes| R33
  R31 -->|data| R34
  R1 -->|skalVurderes| R34
  R9 -->|skalVurderes| R34
  R31 -->|data| R36
  R14 -->|skalVurderes| R36
  R39 -->|skalVurderes| R36
  R14 -->|påvirkerResultat| R36
  R39 -->|påvirkerResultat| R36
  R31 -->|data| R37
  R14 -->|skalVurderes| R37
  R39 -->|skalVurderes| R37
  R14 -->|påvirkerResultat| R37
  R39 -->|påvirkerResultat| R37
  R32 -->|data| R38
  R27 -->|data| R38
  R23 -->|data| R38
  KRAV -->|skalVurderes| R38
  R14 -->|påvirkerResultat| R38
  R39 -->|påvirkerResultat| R38
  R31 -->|data| R39
  R0 -->|skalVurderes| R39
  R25 -->|påvirkerResultat| R39
  R2 -->|påvirkerResultat| R39
  R14 -->|påvirkerResultat| R39
  R31 -->|data| R40
  R2 -->|data| R40
  R25 -->|skalVurderes| R40
  R39 -->|påvirkerResultat| R40
  R2 -->|påvirkerResultat| R40
  linkStyle 0,1,2,3,4,5,8,9,10,13,14,15,19,20,21,24,27,34,35,36,39,41,42,47,52,54,55,61,62,69,72,79,83,84,86,87,88,89,93,94,95,98,99,105,106,107,108,109,110,111,112,115,117,120,125,130,131,132,136,141,142 stroke:#888888
  linkStyle 6,11,16,22,25,28,30,31,37,38,40,43,46,48,49,53,56,57,59,63,64,65,70,73,74,80,85,90,96,97,100,101,102,113,114,116,118,119,121,122,126,127,133,137,143 stroke:#d6336c
  linkStyle 7,12,17,18,23,26,29,32,33,44,45,50,51,58,60,66,67,68,71,75,76,77,78,81,82,91,92,103,104,123,124,128,129,134,135,138,139,140,144,145 stroke:#f08c00
```

## Tabell

| Regelsett | Data fra | skalVurderes avhenger av | påvirkerResultat avhenger av |
| --- | --- | --- | --- |
| Alder (Vilkår) | Søknadstidspunkt (Fastsettelse) | - | - |
| Meldekortberegning (Fastsettelse) | Periode (Fastsettelse) | - | - |
| Dagpengegrunnlag (Fastsettelse) | Dagpenger ved verneplikt (Fastsettelse), Opptjeningsperiode (Fastsettelse), Rettighetstype (Fastsettelse), Søknadstidspunkt (Fastsettelse) | kravPåDagpenger | Alder (Vilkår) |
| Sats og barnetillegg (Fastsettelse) | Dagpengegrunnlag (Fastsettelse), Samordning utenfor folketrygden (Fastsettelse), Søknadstidspunkt (Fastsettelse) | kravPåDagpenger | kravPåDagpenger |
| Periode (Fastsettelse) | Dagpengegrunnlag (Fastsettelse), Minsteinntekt (Vilkår), Søknadstidspunkt (Fastsettelse) | kravPåDagpenger | Minsteinntekt (Vilkår), Verneplikt (Vilkår) |
| Egenandel (Fastsettelse) | Rettighetstype (Fastsettelse), Sats og barnetillegg (Fastsettelse), Søknadstidspunkt (Fastsettelse) | kravPåDagpenger | kravPåDagpenger |
| Eksport til EØS-land (Vilkår) | Rettighetstype (Fastsettelse) | Rettighetstype (Fastsettelse) | Rettighetstype (Fastsettelse) |
| Etablering (Vilkår) | Rettighetstype (Fastsettelse) | Rettighetstype (Fastsettelse) | Rettighetstype (Fastsettelse) |
| Fulle ytelser (Vilkår) | - | Minsteinntekt (Vilkår), Verneplikt (Vilkår) | Minsteinntekt (Vilkår), Verneplikt (Vilkår) |
| Gjenopptak (Vilkår) | Meldekortberegning (Fastsettelse), Rettighetstype (Fastsettelse), Søknadstidspunkt (Fastsettelse) | Rettighetstype (Fastsettelse) | - |
| Periode (Vilkår) | - | Periode (Fastsettelse) | - |
| Krav på dagpenger (Vilkår) | - | - | - |
| Medlemmets opplysningsplikt (Vilkår) | - | - | - |
| Meldeplikt (Vilkår) | Søknadstidspunkt (Fastsettelse) | Krav på dagpenger (Vilkår) | - |
| Minsteinntekt (Vilkår) | Opptjeningsperiode (Fastsettelse), Søknadstidspunkt (Fastsettelse) | Alder (Vilkår) | Alder (Vilkår), Dagpengegrunnlag (Fastsettelse) |
| Omgjøring uten klage (Prosess) | - | Skal omgjøring uten klage vurderes? (Prosess) | - |
| Skal omgjøring uten klage vurderes? (Prosess) | - | - | - |
| Opphold (Vilkår) | Søknadstidspunkt (Fastsettelse) | Minsteinntekt (Vilkår), Verneplikt (Vilkår) | Minsteinntekt (Vilkår), Verneplikt (Vilkår) |
| Opptjeningsperiode (Fastsettelse) | Søknadstidspunkt (Fastsettelse) | Alder (Vilkår) | - |
| Permittering (Vilkår) | Permittering (Fastsettelse), Rettighetstype (Fastsettelse) | Alder (Vilkår), Rettighetstype (Fastsettelse) | Rettighetstype (Fastsettelse) |
| Permittering (Fastsettelse) | - | Rettighetstype (Fastsettelse) | Permittering (Vilkår) |
| Permittering fiskeindustri (Vilkår) | Rettighetstype (Fastsettelse), Søknadstidspunkt (Fastsettelse) | Minsteinntekt (Vilkår), Rettighetstype (Fastsettelse), Verneplikt (Vilkår) | Minsteinntekt (Vilkår), Rettighetstype (Fastsettelse), Verneplikt (Vilkår) |
| Permittering fiskeindustri (Fastsettelse) | Søknadstidspunkt (Fastsettelse) | Permittering fiskeindustri (Vilkår) | Permittering fiskeindustri (Vilkår) |
| Reell arbeidssøker (Vilkår) | Søknadstidspunkt (Fastsettelse) | Alder (Vilkår), Rettighetstype (Fastsettelse) | Alder (Vilkår), Minsteinntekt (Vilkår), Rettighetstype (Fastsettelse), Verneplikt (Vilkår) |
| Registrert som arbeidssøker (Vilkår) | Søknadstidspunkt (Fastsettelse) | Alder (Vilkår) | Alder (Vilkår), Rettighetstype (Fastsettelse) |
| Rettighetstype (Fastsettelse) | Søknadstidspunkt (Fastsettelse), Verneplikt (Vilkår) | Alder (Vilkår) | - |
| Samordning utenfor folketrygden (Fastsettelse) | Dagpengegrunnlag (Fastsettelse), Samordning reduserte ytelser (Vilkår), Sats og barnetillegg (Fastsettelse), Søknadstidspunkt (Fastsettelse) | kravPåDagpenger | Minsteinntekt (Vilkår), Verneplikt (Vilkår) |
| Samordning reduserte ytelser (Vilkår) | Sats og barnetillegg (Fastsettelse), Søknadstidspunkt (Fastsettelse), Tap av arbeidsinntekt og arbeidstid (Vilkår) | Minsteinntekt (Vilkår), Verneplikt (Vilkår) | - |
| Sanksjonsperiode (Vilkår) | Periode (Fastsettelse), Søknadstidspunkt (Fastsettelse) | kravPåDagpenger | - |
| Streik og lock-out (Vilkår) | - | Minsteinntekt (Vilkår), Verneplikt (Vilkår) | Minsteinntekt (Vilkår), Verneplikt (Vilkår) |
| Søknad (Vilkår) | Søknadstidspunkt (Fastsettelse) | - | - |
| Søknadstidspunkt (Fastsettelse) | - | - | - |
| Tap av arbeidsinntekt og arbeidstid (Vilkår) | Dagpengegrunnlag (Fastsettelse), Dagpenger ved verneplikt (Fastsettelse), Fastsettelse av arbeidstid (Fastsettelse), Permittering (Fastsettelse), Permittering fiskeindustri (Vilkår), Rettighetstype (Fastsettelse), Søknadstidspunkt (Fastsettelse) | Minsteinntekt (Vilkår), Verneplikt (Vilkår) | - |
| Tidsbegrenset bortfall (Vilkår) | Periode (Fastsettelse) | kravPåDagpenger | - |
| Tre meldeperioder uten tilstrekkelig tap (Vilkår) | Søknadstidspunkt (Fastsettelse) | Gjenopptak (Vilkår), Meldekortberegning (Fastsettelse) | - |
| Uriktige opplysninger og tilbakeholdelse (Vilkår) | - | - | - |
| Utdanning (Vilkår) | Søknadstidspunkt (Fastsettelse) | Minsteinntekt (Vilkår), Verneplikt (Vilkår) | Minsteinntekt (Vilkår), Verneplikt (Vilkår) |
| Utestengning (Vilkår) | Søknadstidspunkt (Fastsettelse) | Minsteinntekt (Vilkår), Verneplikt (Vilkår) | Minsteinntekt (Vilkår), Verneplikt (Vilkår) |
| Fastsettelse av arbeidstid (Fastsettelse) | Reell arbeidssøker (Vilkår), Samordning reduserte ytelser (Vilkår), Tap av arbeidsinntekt og arbeidstid (Vilkår) | kravPåDagpenger | Minsteinntekt (Vilkår), Verneplikt (Vilkår) |
| Verneplikt (Vilkår) | Søknadstidspunkt (Fastsettelse) | Alder (Vilkår) | Dagpengegrunnlag (Fastsettelse), Minsteinntekt (Vilkår), Rettighetstype (Fastsettelse) |
| Dagpenger ved verneplikt (Fastsettelse) | Dagpengegrunnlag (Fastsettelse), Søknadstidspunkt (Fastsettelse) | Rettighetstype (Fastsettelse) | Dagpengegrunnlag (Fastsettelse), Verneplikt (Vilkår) |
