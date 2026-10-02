# Regelverk-arkitektur

Denne guiden beskriver hvordan regelverk er strukturert i dp-behandling, og hvordan man legger til nye.

## Konsepter

Et **regelverk** er en selvstendig enhet med egne regler, hendelser, prosesser og mottak.
Eksempler: Dagpenger, Ferietillegg.

Hvert regelverk har en egen Gradle-modul og registrerer seg via `RegelverkRegistrering` — en abstrakt klasse
som fungerer som det eneste integrasjonspunktet mot resten av systemet.

## Modulstruktur

```
mitt-regelverk/
  src/main/kotlin/no/nav/dagpenger/mittregelverk/
    regelsett/
      vilkår/            # Inngangsvilkår (hvert vilkår = ett regelsett)
      beregning/         # Beregningsregler
      fastsetting/       # Fastsettingsregler
      prosessvilkår/     # Vilkår som styrer prosesstilstand
    prosess/             # Forretningsprosesser og plugins
    hendelse/            # StartHendelse-implementasjoner
    mottak/              # Rapids & Rivers-mottak
    MittRegelverkRegistrering.kt   # Registrering (koblingspunkt)
    RegelverkMittRegelverk.kt      # Regelverk-instans (regelsett-samling)
    OpplysningsTyper.kt            # Opplysningstyper for dette regelverket
```

## Slik legger du til et nytt regelverk

### 1. Opprett Gradle-modul

Lag en ny modul med avhengighet til `regelverk`-modulen:

```kotlin
// mitt-regelverk/build.gradle.kts
dependencies {
    implementation(project(":regelverk"))
    // ...
}
```

### 2. Definer regelsett og opplysningstyper

Hvert regelsett er et objekt med en `regelsett`-property:

```kotlin
object MittVilkår {
    val opplysningstype = Opplysningstype.boolsk(/* ... */)

    val regelsett = Regelsett("MittVilkår") {
        regel(opplysningstype) { /* ... */ }
    }
}
```

### Påvirkning på rettighetsperioden

Vilkår kan velge `RettighetsperiodeStrategi` i DSL-en. Standardverdien
`VilkårForRett` beholder ordinær vilkårsvurdering: utfallet og begge datogrenser
inngår i beregningen av retten. Utdanning bruker denne strategien.

Etablering velger en annen strategi:

```kotlin
påvirkningPåRettighetsperiode(RettighetsperiodeStrategi.SettTilOgMedVedOppfylt)
```

`påvirkerResultat` avgjør fortsatt om regelsettet er relevant. For et relevant
regelsett med `SettTilOgMedVedOppfylt` bruker `RettighetsperiodeUtleder` det siste
utfallet. Et positivt utfall som overlapper den siste perioden med løpende rett,
setter rettens `tilOgMed` til utfallets `tilOgMed`. Startdatoen og rettens verdi
endres ikke. Et negativt eller manglende utfall har ingen effekt, og en senere
stans eller eldre rettighetsperioder overstyres ikke.

Sluttdatostrategien krever et utfall i regelsettet. Regelsett med denne strategien
inngår ikke som inngangsvilkår i `kravPåDagpenger()`. Hvis flere relevante og
oppfylte sluttdatovilkår gir ulike sluttdatoer for samme rettighetsperiode,
feiler beregningen eksplisitt. PoC-en velger ikke dato etter regelsettenes
rekkefølge. Like sluttdatoer er tillatt, og alle bidragene tas med i utledningen.

Strategien er ikke en markering av tilleggsrettigheter for frontend.

`RettighetsperiodeUtleder` beregner periodeendringene uten å endre opplysninger.
Den returnerer også sluttdatoutfallene som bidro til hver justering.
`RettighetsperiodePlugin` henter relevante utfall, lagrer de utledede periodene
som `Faktum` med sporbar `Utledning`, og logger endringene.

### 3. Lag Regelverk-instans

Samle alle regelsett i en `Regelverk`-instans:

```kotlin
val RegelverkMittRegelverk = Regelverk(
    MittVilkår.regelsett,
    MinBeregning.regelsett,
    // ...
)
```

### 4. Implementer RegelverkRegistrering

```kotlin
class MittRegelverkRegistrering : RegelverkRegistrering(RegelverkMittRegelverk) {

    // Valgfritt: legg til ekstra opplysningstyper utover det regelverket produserer
    // override val opplysningstyper = super.opplysningstyper + minEkstraType

    override fun registrerMottak(
        rapidsConnection: RapidsConnection,
        hendelseMottaker: HendelseMottaker,
    ) {
        MittMottak(rapidsConnection, hendelseMottaker)
    }

    override fun registrerProsesser(prosessregister: Prosessregister) {
        prosessregister.registrer(MinProsess())
    }
}
```

`RegelverkRegistrering` gir deg:
- **`opplysningstyper`** — automatisk utledet fra `regelverk.produserer` + `hendelseTypeOpplysningstype`
- **`registrer()`** — template method som kaller `registrerMottak()` og `registrerProsesser()`
- **`registrerProsesser()`** — kan kalles alene i tester uten RapidsConnection

### 5. Registrer i ApplicationBuilder

Legg til registreringen i listen over regelverk:

```kotlin
// ApplicationBuilder.kt
private val regelverk = listOf(
    DagpengerRegistrering(),
    FerietilleggRegistrering(),
    MittRegelverkRegistrering(),  // ← ny
)
```

Alt annet skjer automatisk:
- Opplysningstyper samles og lagres i databasen
- Mottak kobles til RapidsConnection
- Prosesser registreres i prosessregisteret

## Pakkestruktur for regelsett

Regelsett organiseres etter funksjon:

| Pakke | Innhold | Eksempler |
|-------|---------|-----------|
| `regelsett/vilkår/` | Inngangsvilkår og rettighetsregler | Alderskrav, Minsteinntekt, Opphold |
| `regelsett/beregning/` | Beregning av ytelsen | Beregning, Beregningsperiode |
| `regelsett/fastsetting/` | Fastsetting av størrelse og periode | Dagpengegrunnlag, Egenandel |
| `regelsett/prosessvilkår/` | Vilkår som påvirker prosessflyt | OmgjøringUtenKlage |

## Designprinsipper

- **Én modul per regelverk** — regelverk skal ikke dele kode direkte, bare via felles moduler (`opplysninger`, `regelverk`, `modell`)
- **Registrering som koblingspunkt** — `RegelverkRegistrering` er det eneste stedet et regelverk kobles til mediatoren
- **Ingen kobling mellom regelverk** — MessageMediator kjenner ikke til noen spesifikke regelverk
- **Base-klassen eier felles opplysningstyper** — `hendelseTypeOpplysningstype` inkluderes automatisk for alle regelverk
- **Prosesser og regler er adskilt** — prosesser (forretningsprosesser, plugins) og regelsett (vilkår, beregning, fastsetting) har separate pakker
