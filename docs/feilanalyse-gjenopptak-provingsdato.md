# Feil i gjenopptak og meldekortberegning

Analyse av to sammenhengende feil: at rekkefølgen på saksbehandlerens endringer avgjorde utfallet av
et gjenopptak, og produksjonsfeilen `Mangler utbetaling for dag 2026-08-17` i meldekortberegningen.

Del 1 er skrevet for produktleder og andre uten kjennskap til regelmotoren. Del 2 er for utviklere.

## Del 1 — Oppsummering

### Hva skjedde

En saksbehandler behandlet et gjenopptak der brukeren skulle på utdanning i en periode. Rekkefølgen
saksbehandleren la inn opplysningene i, avgjorde om saken ble riktig. Samme opplysninger, ulik
rekkefølge, ulikt resultat.

Saken endte med avslag der den skulle endt med gjenopptak. Etterpå feilet meldekortberegningen for
perioden med en teknisk melding om at utbetalingen manglet.

### Hvorfor det er alvorlig

Saksbehandleren hadde ingen måte å se at noe var galt. Systemet ga ingen feilmelding ved innlegging —
det regnet feil, stille. Feilen dukket først opp senere, og da som en kryptisk teknisk melding uten
sammenheng med det saksbehandleren faktisk gjorde.

### Hva som var årsaken

Systemet regner ut fra en **prøvingsdato** — datoen regelverket vurderes fra. Ved gjenopptak flyttes
denne datoen fremover.

To ting gikk galt når den flyttet seg:

Endringer saksbehandleren gjorde **bakover i tid** ble hoppet over. Systemet regnet bare fra den nye
datoen og framover, så rettingene i den eldre perioden ble aldri vurdert.

Sammensatte vilkår ble stående på gammel verdi. Vilkåret «tap av både arbeidsinntekt og arbeidstid»
var regnet ut på et tidspunkt da tapt arbeidstid ennå ikke var oppfylt. Da arbeidstiden senere ble
lagt inn, regnet ikke systemet vilkåret på nytt — det så på den gamle utregningen som fortsatt
gyldig. Dermed sto vilkåret som ikke oppfylt, og brukeren fikk avslag.

Begge deler er rettet.

### Konsekvens og omfang

Feilen krevde en bestemt kombinasjon: gjenopptak, etterfulgt av endringer bakover i tid. Vi har
reprodusert den fra en faktisk sak i produksjon, men **vi vet ennå ikke hvor mange saker som er
berørt**. Det bør undersøkes særskilt.

Berørte saker kan ha fått avslag der de skulle hatt innvilgelse.

### Hva som gjenstår

Én svakhet er kartlagt, men ikke rettet: når regelmotoren går i vranglås, går behandlingen likevel
videre og feiler senere med en misvisende melding. Neste gang en vranglås oppstår av en annen grunn,
får vi samme forvirrende symptom. Dette bør bli en egen sak.

## Del 2 — Teknisk analyse

### Feil 1 — endringer bakover i tid ble forkastet

`Søknadsprosess.regelkjøring()` valgte én regelkjøringsdato:

```kotlin
val regelkjøringsdato = maxOf(prøvingsdato, ubehandlede.first())
```

Når prøvingsdato hadde hoppet fremover, klemte `maxOf` bort ubehandlede saksbehandlerendringer som lå
bak i tid. De ble aldri evaluert, og utledede opplysninger i de periodene ble stående utdaterte.

Rettingen legger disse datoene inn som ekstra regelkjøringsdatoer. Det vanskelige er å kjenne igjen
*når* hoppet har skjedd, uten å legge til ekstradatoer i behandlinger der de gir vranglås.

Første forsøk så på hvem som hadde flyttet datoen. Det viste seg å være skjørt: både regelmotoren og
`PrøvingsdatoPlugin` kan gjøre det, og hvem som rekker først varierer mellom produksjon og test.

Sjekken er derfor ren tilstand:

```kotlin
if (!prøvingsdato.gyldighetsperiode.fraOgMed.isEqual(prøvingsdato.verdi)) return false
return prøvingsdato.gyldighetsperiode.fraOgMed.isAfter(søknadstidspunkt.gyldighetsperiode.fraOgMed)
```

Kravet om at gyldighetsperioden starter på selve verdien skiller et **rent hopp** fra en prøvingsdato
saksbehandler har satt manuelt:

| Situasjon                     | verdi      | gjelder fra    | Ekstradatoer |
| ----------------------------- | ---------- | -------------- | ------------ |
| Manuelt satt av saksbehandler | 2026-08-03 | **2026-08-01** | nei          |
| Rent hopp ved gjenopptak      | 2026-08-17 | 2026-08-17     | ja           |

Uten den første betingelsen tar vi med datoer i behandlinger der `PrøvingsdatoPlugin` flytter datoen
fremover igjen for hver runde. Da lander vi på samme plan og får `REGELKJØRING_LOOP`.

### Feil 2 — utledede opplysninger ble ikke replanlagt

Dette var den egentlige årsaken til avslaget. Tilstanden ved forslag til vedtak:

```text
kravTilTaptArbeidstid:       17.aug– = true    ← ny opplysning
kravTilTapAvArbeidsinntekt:  11.aug– = true
kombinasjonen:               11.aug– = false   ← utledet av den GAMLE (27.juli–16.aug)
```

Kombinasjonsvilkåret ble utledet ved 11. august, da tapt arbeidstid fortsatt var `false`. Det fikk
åpen periode fra 11. august. Da regelkjøringen senere kom til 17. august, fant den en kombinasjon som
dekket datoen, og planla den ikke på nytt.

Den gamle avhengigheten var verken **erstattet** eller markert **utdatert** — den lever fortsatt for
sin egen periode, 27. juli til 16. august. Ingen av de eksisterende grenene i `lagPlanFraUtledning`
fanget derfor situasjonen.

Ny gren i `Regel.lagPlan`:

```kotlin
private fun harAndreGjeldendeAvhengigheter(
    utledetAv: Utledning,
    opplysninger: LesbarOpplysninger,
): Boolean {
    val gjeldende = opplysninger.finnFlere(avhengerAv)
    if (gjeldende.size != avhengerAv.size) return false
    val brukte = utledetAv.opplysninger.mapTo(mutableSetOf()) { it.id }
    return gjeldende.any { it.id !in brukte }
}
```

`opplysninger` er her viewet for prøvingsdatoen. Er produktet utledet av andre opplysninger enn de som
gjelder på datoen, hører produktet til en tidligere periode og må utledes på nytt.

Dette er en generell retting i regelmotoren. Den treffer alle regler, ikke bare prøvingsdato.

### De to rettingene overlapper ikke

Begge trengs. Verifisert ved å slå av én om gangen:

| Variant                            | Resultat                                    |
| ---------------------------------- | ------------------------------------------- |
| Ingen ekstradatoer                 | `ScenarioTest` + `GjenopptakTest` feiler     |
| Ekstradatoer alltid                | `ScenarioTest` + `ArbeidssøkerTest` feiler   |
| Kun `fom > søknadstidspunkt.fom`   | `ArbeidssøkerTest` feiler (`REGELKJØRING_LOOP`) |
| Begge ledd                         | grønn                                        |

`Regel.lagPlan` virker bare på datoer som faktisk kjøres. `prøvingsdatoErFlyttetFremover` avgjør
hvilke datoer som kjøres. Uten den blir 27. juli aldri regelkjøringsdato, og da har `lagPlan`
ingenting å reagere på.

### Hvorfor prøvingsdato har `egenVerdi` og sin egen regel

`prøvingsdato` er deklarert med `gyldighetsperiode = egenVerdi`: opplysningens periode starter på sin
egen verdi. Prøvingsdato 17. august gjelder fra og med 17. august.

Uten `egenVerdi` faller den tilbake på `minsteMulige`, som arver perioden fra opplysningene den er
utledet av. Da styres periodeinndelingen av hvor inputene tilfeldigvis er splittet, ikke av hvor
regelverksvurderingen skifter. Prøvingsdato-perioden ville sluttet 10. august i stedet for 16. august.

`egenVerdi` er også premisset for `prøvingsdatoErFlyttetFremover`. Sjekken `fom == verdi` er bare
meningsfull når strategien garanterer den sammenhengen.

Samtidig gjør `egenVerdi` standard replanlegging ikke-konvergent. Derfor må regelen være
`prøvingsdato(søknadstidspunkt)` og ikke `sisteAv(søknadstidspunkt)`. Beregningen er identisk —
`sisteAv` over ett argument er `maxOrNull` over én verdi — men `Prøvingsdato`-regelen overstyrer
`lagPlanNårAvhengerErErstattet` med et kikk-steg:

```kotlin
val kandidat = kjør(opplysninger, gjelderFor)
if (kandidat.isAfter(gjelderFor)) return   // kan aldri dekke denne dagen
```

Med `sisteAv` blir kjeden slik:

```text
Evaluerer dag 11.august
  → Søknadstidspunkt er erstattet (ny verdi 17.august)
  → standard lagPlanNårAvhengerErErstattet → plan.add(this)
  → regelen produserer 17.august, egenVerdi gir fom = 17.august
  → den dekker ikke 11.august
  → avhengigheten er fortsatt erstattet → samme plan neste runde
  → RegelkjøringLoopException
```

Generelt: enhver regel som produserer en opplysning med `egenVerdi` trenger denne beskyttelsen.
Standard replanlegging antar at et replanlagt produkt dekker dagen som utløste replanleggingen. Med
`egenVerdi` holder ikke antakelsen for verdier som peker framover i tid.

### Kausalkjeden til `Mangler utbetaling`

```text
Regelkjøringen stabiliserer seg ikke
  → RegelkjøringLoopException
  → catch-blokken i Behandling.kt returnerer FØR kjørRegelkjøringFerdig
  → MeldekortBeregningPlugin kjører aldri → ingen Beregning.utbetaling
  → behandlingen går likevel til ForslagTilVedtak
  → IllegalStateException: Mangler utbetaling for dag 2026-08-17
```

`Mangler utbetaling` er en **følgefeil**, ikke en beregningsfeil. Den peker på feil sted i koden.

### Endrede filer

| Fil                  | Endring                                                    |
| -------------------- | ---------------------------------------------------------- |
| `Søknadsprosess.kt`  | `prøvingsdatoErFlyttetFremover` styrer ekstra regelkjøringsdatoer |
| `Regel.kt`           | Ny gren `harAndreGjeldendeAvhengigheter` i `lagPlanFraUtledning` |
| `GjenopptakTest.kt`  | Reproduksjon av produksjonssaken                            |

### Verdt å merke seg

**Produksjon og test kjørte samme kode med motsatt utfall.** Hvem som rakk å sette prøvingsdato
først — regelmotoren eller pluginet — varierte mellom miljøene. Enhver deteksjon basert på *hvem* som
gjorde en endring, arver den skjørheten. Se på tilstanden i stedet.

**Testriggen skjulte feilen.** Så lenge `Mennesket` svarte `ØnskerDagpengerFraDato` uten
gyldighetsperiode, ble `ønsketdato` en evig opplysning ved gjenopptak. `søknadstidspunkt` fikk aldri
ny periode, prøvingsdatoen flyttet seg aldri, og feilen lot seg ikke reprodusere i det hele tatt.

**Et forsøk med egen `Pluginkilde` ble forkastet.** Ideen var å skille «plugin har skrevet dette» fra
«saksbehandler har bestemt dette», slik at bare det første kunne avløses. Det virket, men da feil 2
var rettet, var hele suiten grønn uten. Vi valgte den mindre endringen — den slapp en DB-migrasjon og
endringer i seks filer. Skillet er fortsatt en reell latent svakhet: en plugin-skrevet verdi
kortslutter regeltreet under seg, akkurat som et saksbehandlerfaktum.

**Testendring:** `GjenopptakTest` forventer 4 rettighetsperioder. Perioden 27. juli til 16. august er
én sammenhengende periode uten rett. At produksjonsdataene splittet ved 11. august, var et artefakt av
at søknadstidspunktet endret seg uten at utfallet gjorde det.

### Oppfølging

1. Kartlegg omfanget i produksjon — hvilke saker er berørt?
2. Egen sak: behandlingen skal ikke gå til `ForslagTilVedtak` når plugins er hoppet over på grunn av
   `RegelkjøringLoopException`.
3. Vurder om plugins som skriver kildeløse fakta har samme latente problem som beskrevet over.
