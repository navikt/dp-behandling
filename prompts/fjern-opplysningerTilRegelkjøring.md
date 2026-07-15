# Fjern behovet for `opplysningerTilRegelkjøring`

## Bakgrunn

`Regelkjøring` har en konstruktørparameter:

```kotlin
val opplysningerTilRegelkjøring: LesbarOpplysninger.(LocalDate) -> LesbarOpplysninger =
    { prøvingsdato -> forDato(prøvingsdato) }
```

Den lar en `Forretningsprosess` overstyre hvordan `Regelkjøring` filtrerer/velger
opplysninger for en gitt prøvingsdato, i stedet for å bruke default (`forDato(prøvingsdato)`).

Historikk: `Manuellprosess` hadde tidligere en egen, feil implementasjon av denne
(`{ forDato(prøvingsdato(this)) }`, som ignorerte parameteren og regnet ut sin egen,
potensielt utdaterte prøvingsdato på nytt ved hvert kall). Dette var årsaken til en reell
bug i Eksport-regelsettet (se `EksportTest`). Bugen er fikset ved at `Manuellprosess` nå
bruker default-oppførselen, og produksjonskoden har dermed ingen prosess som lenger
overstyrer denne parameteren.

I dag er parameteren likevel fortsatt i bruk noen steder — kun i **tester**:

- `dagpenger/src/test/kotlin/no/nav/dagpenger/features/OpplysningerTilRegelkjøring.kt`
  (brukt av `OpptjeningstidSteg`, `SamordningSteg`, `SamordningUtenforFolketrygdenSteg`):
  pinner regelkjøringen til `Søknadstidspunkt.prøvingsdato` når den finnes, ellers
  `forDato(it)`.
- `ferietillegg/src/test/kotlin/no/nav/dagpenger/ferietillegg/features/OpplysningerTilRegelkjøring.kt`
  (brukt av `FerietilleggSteg`): `{ this }` — ingen filtrering i det hele tatt.

Disse er trolig arbeidsrundt for at Cucumber-testene skal kunne sette opp en fast,
forutsigbar tilstand uavhengig av prøvingsdato-utledningen i disse test-scenarioene,
snarere enn et reelt behov i domenet.

## Oppgave

Vurder om disse to test-spesifikke overstyringene faktisk kan fjernes, slik at
`opplysningerTilRegelkjøring`-parameteren (og de tilhørende "brukes kun av tester"
sekundærkonstruktørene i `Regelkjøring.kt`) kan fjernes helt fra kodebasen — for å
redusere kompleksitet og fjerne en mekanisme som allerede har forårsaket én reell bug.

Konkret:

1. Kartlegg nøyaktig hvorfor `OpptjeningstidSteg`, `SamordningSteg`,
   `SamordningUtenforFolketrygdenSteg` og `FerietilleggSteg` trenger sin egen
   `opplysningerTilRegelkjøring`. Hva ville gått galt om de i stedet brukte default
   (`forDato(prøvingsdato)`)?
   - For `dagpenger`-varianten: er `Søknadstidspunkt.prøvingsdato`-pinning fortsatt
     nødvendig, eller var det et arbeidsrundt for et problem som siden er løst
     (f.eks. samme type prøvingsdato-drift-bug som ble funnet og fikset i
     `Manuellprosess`)?
   - For `ferietillegg`-varianten (`{ this }`, ingen filtrering): hvorfor trengs det å
     hoppe over `forDato`-filtreringen helt? Er dette dekning for et testoppsett som
     mangler nødvendige gyldighetsperioder, eller et reelt behov?
2. Prøv å fjerne overstyringene ett steg om gangen (start med én test-fil), la stegene
   falle tilbake på default, og kjør de berørte cucumber-testene
   (`:dagpenger:test`, `:ferietillegg:test`) for å se om de fortsatt består.
3. Hvis noen tester feiler: undersøk om feilen skyldes en reell forskjell i
   testoppsettet (som bør fikses i testdataene/scenarioet), eller om default-oppførselen
   i `Regelkjøring` selv har en svakhet som bør rettes generelt (ikke bare
   arbeidsrundt lokalt i testen).
4. Når/hvis alle overstyringer er fjernet: fjern selve
   `opplysningerTilRegelkjøring`-parameteren fra `Regelkjøring`s primærkonstruktør og de
   sekundærkonstruktørene som kun finnes for å eksponere den til tester
   (merket `// brukes av tester` i `Regelkjøring.kt`), samt de nå ubrukte
   `OpplysningerTilRegelkjøring.kt`-filene i `dagpenger` og `ferietillegg`.
5. Kjør full testsuite (`./gradlew build` eller minimum berørte moduler) og
   `ktlintCheck` for å bekrefte at ingenting er brutt.

## Ikke gjør

- Ikke fjern parameteren "for sikkerhets skyld" hvis testene faktisk trenger den — da
  må heller det underliggende problemet i testoppsettet fikses ordentlig, eller
  parameteren beholdes med en klar begrunnelse for hvorfor den fortsatt trengs.
- Ikke gjør endringer i produksjonssemantikk for å tvinge frem fjerning av
  test-only-mekanismen.

## Relevante filer

- `opplysninger/src/main/kotlin/no/nav/dagpenger/opplysning/Regelkjøring.kt`
- `dagpenger/src/test/kotlin/no/nav/dagpenger/features/OpplysningerTilRegelkjøring.kt`
- `dagpenger/src/test/kotlin/no/nav/dagpenger/features/OpptjeningstidSteg.kt`
- `dagpenger/src/test/kotlin/no/nav/dagpenger/features/SamordningSteg.kt`
- `dagpenger/src/test/kotlin/no/nav/dagpenger/features/SamordningUtenforFolketrygdenSteg.kt`
- `ferietillegg/src/test/kotlin/no/nav/dagpenger/ferietillegg/features/OpplysningerTilRegelkjøring.kt`
- `ferietillegg/src/test/kotlin/no/nav/dagpenger/ferietillegg/features/FerietilleggSteg.kt`
- `dagpenger/src/main/kotlin/no/nav/dagpenger/regel/prosess/Manuellprosess.kt` (referanse:
  hvordan den tilsvarende produksjonsbugen ble funnet og fikset)
