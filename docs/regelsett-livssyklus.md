# Regelsett: skalKjøres, skalRevurderes og påvirkerResultat

Dette dokumentet forklarer de tre "livssyklus"-funksjonene på et `Regelsett`
(`skalVurderes`/`skalKjøres`, `skalRevurderes` og `påvirkerResultat`), hvordan de
brukes internt, og vanlige feil.

## Hva de gjør

| DSL-funksjon (builder) | Felt på `Regelsett` | Default | Brukes av |
|---|---|---|---|
| `skalVurderes { ... }` | `skalKjøres` | `{ true }` | `Forretningsprosess.ønsketResultat`, `Forretningsprosess.produsenter`, `Regelkjøring.kanKjøre` |
| `skalRevurderes { ... }` | `skalRevurderes` | `{ true }` | `Forretningsprosess.produsenter` |
| `påvirkerResultat { ... }` | `påvirkerResultat` | `{ true }` | `Regelverk.relevanteVilkår`, `Regelverk.relevanteFastsettelser` |

Alle tre er predikater `(LesbarOpplysninger) -> Boolean` som evalueres på nytt
hver gang regelverket kjøres — de er **ikke** cachet per behandling.

### `skalKjøres` — "skal dette regelsettet i det hele tatt kjøre?"

Styrer om regelsettets regler overhodet får lov til å produsere opplysninger
denne runden. Hvis `false`:
- Regelsettets `ønsketInformasjon` tas ikke med i `ønsketResultat` for
  prosessen (`Forretningsprosess.ønsketResultat`).
- Regelsettet er ikke med i `produsenter()`, så ingen av dets regler kjøres —
  opplysningene det ville produsert forblir uproduserte (eller beholder
  eksisterende verdi fra tidligere behandling, se `kanKjøre`).

Typisk brukt til å uttrykke en forutsetning: "dette vilkåret er bare aktuelt
når X er oppfylt", f.eks. `oppfyllerKravetTilMinsteinntektEllerVerneplikt(it)`.

### `skalRevurderes` — "skal vi kjøre reglene på nytt, eller beholde forrige svar?"

Kun relevant **når `skalKjøres` allerede er sann** (se
`Forretningsprosess.produsenter()`, som filtrerer på begge etter hverandre).
Brukes til regelsett hvor resultatet er "sticky" — man ønsker ikke å regne det
om på nytt hvis det allerede finnes en verdi (f.eks.
`skalRevurderes { it.mangler(egenandel) }`). Default er `{ true }`, altså
"regn på nytt hver gang".

### `påvirkerResultat` — "er dette regelsettet relevant for vedtaket/avslaget?"

Brukes helt separat fra kjøring — typisk for å bygge lister av vilkår/
fastsettelser som skal presenteres i vedtaksbrev, metrikker og
rettighetsperiode-beregning (`relevanteVilkår`, `relevanteFastsettelser`).
Dette leser **eksisterende opplysninger** (typisk `betingelser`/`utfall`), det
kjører ingen regler.

## Viktig invariant (ofte brutt)

> Et regelsett kan bare være "relevant for resultatet" dersom det faktisk har
> fått lov til å kjøre.

Med andre ord bør følgende alltid holde for enhver `opplysninger`:

```
påvirkerResultat(opplysninger) implies skalKjøres(opplysninger)
```

Hvis `påvirkerResultat` er sann mens `skalKjøres` er usann, blir regelsettet
tatt med i `relevanteVilkår`/`relevanteFastsettelser` selv om det aldri har
produsert (eller oppdatert) opplysningene sine. Konsumentene
(`Vedtak.lagVedtakDTO`, `BehandlingMetrikker`, rettighetsperiode-beregning)
leser da enten ingenting (opplysningen finnes ikke → stille hull i
vedtaksbrevet) eller en foreldet verdi fra en tidligere behandling.

Dette var akkurat feilen i
[`655c3c72`](../.git) for `PermitteringFraFiskeindustrien`: `skalVurderes`
krevde `oppfyllerKravetTilMinsteinntektEllerVerneplikt(it) &&
it.erSann(permitteringFiskeforedling)`, mens `påvirkerResultat` kun sjekket
`erPermittertFraFisk()` — altså en løsere betingelse. Regelsettet kunne bli
markert som relevant selv om minsteinntektsvilkåret gjorde at det aldri kjørte.

### Samme mønster ble funnet (og rettet) i `Dagpengeperiode` og `SamordingUtenforFolketrygden`

Samme asymmetri fantes i:
- `Dagpengeperiode.kt`: `skalVurderes { kravPåDagpenger(it) }` vs.
  `påvirkerResultat { oppfyllerKravetTilMinsteinntektEllerVerneplikt(it) }`
- `SamordingUtenforFolketrygden.kt`: samme par som over

Dette var slurv (bekreftet), ikke en tilsiktet frikobling — begge er nå rettet
til å bruke `kravPåDagpenger` for begge funksjonene, i tråd med søsken-
regelsettene `Egenandel.kt`/`DagpengenesStørrelse.kt` som allerede gjorde dette
riktig.

## Hvorfor invarianten opprinnelig ikke kunne bakes inn direkte i `Regelsett`

Det mest nærliggende strukturelle grepet — å redefinere `påvirkerResultat`
som `{ skalKjøres(it) && råPåvirkerResultat(it) }` direkte på `Regelsett` — ble
først forsøkt, men førte til **uendelig rekursjon**:

`Sanksjonsperiode.kt` og `TidsbegrensetBortfall.kt` var opprinnelig
`Vilkår`-regelsett som brukte `skalVurderes { kravPåDagpenger(it) }`.
`kravPåDagpenger` kaller `Regelverk.relevanteVilkår`, som filtrerer **alle**
`Vilkår`-regelsett (inkludert regelsettet selv) på `påvirkerResultat`. Hvis
`påvirkerResultat` i seg selv var definert til å avhenge av `skalKjøres`,
fikk vi:

```
skalKjøres → kravPåDagpenger → relevanteVilkår → (seg selv) påvirkerResultat → skalKjøres → …
```

## Rotårsaken ble fjernet: Sanksjonsperiode/TidsbegrensetBortfall er ikke vilkår

Ved nærmere ettersyn er `Sanksjonsperiode` og `TidsbegrensetBortfall`
domenemessig **ikke** vilkår for retten til dagpenger — de fastsetter en
konsekvens/reduksjon (sanksjon, tidsbegrenset bortfall) *gitt at* retten
allerede er oppfylt. De skal ikke kunne påvirke selve `harRett`, kun forbruk av
kvote. At de likevel var registrert som `RegelsettType.Vilkår` var det som
skapte den selvrefererende avhengigheten til `kravPåDagpenger` beskrevet over.

Begge er derfor omklassifisert til `RegelsettType.Fastsettelse`
(`fastsettelse(...)`-builder i stedet for `vilkår(...)`, og
`utfall(harSanksjon) { ... }` → `regel(harSanksjon) { ... }`, uten
funksjonelt tap siden `betingelser`/`utfall`-feltene uansett er tomme for
Fastsettelse-regelsett).

Konsekvenser av dette, alle verifisert med grønne tester:
- `Regelverk.relevanteVilkår` inkluderer dem ikke lenger i det hele tatt, så
  `kravPåDagpenger` sin AND-sammenstilling er upåvirket av dem (de var uansett
  alltid selv-tilfredsstillende og kunne aldri i praksis blokkere resultatet —
  se forrige seksjon).
- Ingen gjenværende `Vilkår`-regelsett har `skalVurderes`/`skalKjøres` som
  avhenger av `kravPåDagpenger` (verifisert for alle regelsett som fortsatt
  ligger i `regelsett/vilkår/`-pakken). Sirkulariteten er dermed strukturelt
  umulig, ikke bare unngått ved konvensjon.
- API-konsekvens: de to regelsettene flyttet fra `vilkår`- til
  `fastsettelser`-lista i saksbehandler-API-et
  (`BehandlingApiMapper.kt`/`openapi/behandling-api.yaml`), og
  `opplysningTilVilkårMap` i `Vedtak.kt` mistet de to tilhørende
  mappingene (de kunne uansett aldri opptre i et avslagsvedtak, se over).
- Filplassering: `Sanksjonsperiode.kt`/`TidsbegrensetBortfall.kt` er flyttet
  fra `regel/regelsett/vilkår/` til `regel/regelsett/fastsetting/` (kun
  pakke/mappe, ikke `object`-navn), for at plasseringen skal stemme med den
  nye `RegelsettType`. Alle importer i konsumenter (bl.a.
  `RegelverkDagpenger.kt`, `Vedtak.kt`, `BehandlingApiMapper.kt`, samt
  feature-/scenario-tester) er oppdatert til det nye pakkenavnet.

## Den valgte løsningen: én trygg, generell invariant

`Regelsett` har fått en ny metode, generell for alle typer regelsett (ikke
per builder-subtype):

```kotlin
fun erRelevantForResultatet(opplysninger: LesbarOpplysninger): Boolean =
    skalKjøres(opplysninger) && påvirkerResultat(opplysninger)
```

Siden sirkulariteten nå er strukturelt umulig, er selve
`Regelverk.relevanteVilkår`/`relevanteFastsettelser` oppdatert til å bruke
`erRelevantForResultatet` **direkte** — det finnes ingen egen "rå" og "trygg"
variant lenger:

```kotlin
fun relevanteVilkår(opplysninger: LesbarOpplysninger): List<Regelsett> =
    regelsett.filter { it.type == RegelsettType.Vilkår }
        .filter { it.erRelevantForResultatet(opplysninger) }

fun relevanteFastsettelser(opplysninger: LesbarOpplysninger): List<Regelsett> =
    regelsett.filter { it.type == RegelsettType.Fastsettelse }
        .filter { it.erRelevantForResultatet(opplysninger) }
```

Dette gjør at et regelsett aldri kan fremstå som relevant for vedtaket uten
selv å ha kjørt — for **alle** forbrukere, både internt (`kravPåDagpenger`) og
eksternt (`Vedtak.lagVedtakDTO`, `BehandlingMetrikker`, `RettighetsperiodePlugin`)
— uten noe manuelt unntak eller separate funksjonsnavn å holde styr på.

## Trenger vi lenger en regresjonstest for invarianten?

Nei. Vurderingen underveis var å innføre en permanent invariant-test
(`påvirkerResultat ⇒ skalKjøres`, evaluert per regelsett/opplysningssett) som
et sikkerhetsnett. Den ble forkastet til fordel for å **fjerne
forutsetningen for at brudd kan oppstå**:

- `erRelevantForResultatet` gjør invarianten strukturelt sann for alle
  forbrukere, uansett om et enkelt regelsetts `påvirkerResultat` skulle
  divergere fra `skalKjøres` — konsumentene leser aldri det rå feltet alene.
- Selve muligheten for selvrefererende, rekursjonsutsatt bruk av
  `kravPåDagpenger` inne i et `Vilkår`-regelsett sin `skalVurderes` er fjernet
  ved omklassifiseringen over.

En invariant-test ville kun ha fanget symptomet (asymmetrisk
`skalVurderes`/`påvirkerResultat`) for enkeltregelsett — ikke roten (at
`relevanteVilkår`/`relevanteFastsettelser` kunne "lekke" et regelsett som
aldri kjørte). Med roten fjernet er testen overflødig; den ble derfor bevisst
ikke lagt inn permanent.

## Feltet `påvirkerResultat` er nå privat

Etter at rotårsaken (den selvrefererende bruken av `kravPåDagpenger`) ble
fjernet, er det ingen gjenværende legitim grunn til å lese det rå
`påvirkerResultat`-feltet direkte utenfra `Regelsett` — `erRelevantForResultatet`
er alltid riktig valg. Feltet er derfor gjort `private` på `Regelsett`.

Dette avdekket umiddelbart et **reelt, tredje tilfelle** av nøyaktig samme
bug-mønster som utløste denne hele analysen: `BehandlingApiMapper.kt` satte
`RegelsettDTO.relevantForResultat` (feltet saksbehandler ser i API-et) direkte
fra `påvirkerResultat(...)`, uten å sjekke `skalKjøres`. Rettet til
`erRelevantForResultatet(...)`.

To testfiler (`MinsteinntektTest.kt`, `VernepliktTest.kt`) som testet
`Minsteinntekt`/`Verneplikt` sin **lovlige** divergens (`skalKjøres` sann,
`påvirkerResultat` varierer — se avsnittet om invarianten) leste også feltet
direkte. Disse er oppdatert til å kalle `erRelevantForResultatet` i stedet:
- For `Minsteinntekt` var ingen datajustering nødvendig, siden testens egen
  `alder`-parameter allerede tilsvarer regelsettets `skalKjøres`-gate.
- For `Verneplikt` er `kravTilAlder = true` lagt til i testens opplysninger,
  siden regelsettets `skalKjøres` gates på `kravTilAlder` (en annen opplysning
  enn `skalVernepliktVurderes`, som testen selv varierer) - testen verifiserer
  fortsatt nøyaktig samme interne logikk som før, nå bare gjennom den riktige,
  sammensatte inngangen.

Denne innstrammingen er et konkret eksempel på mål 3 i den opprinnelige
analysen (constraints som fanger opp ulogisk bruk tidligere): å gjøre feltet
`private` tvang kompilatoren til å avdekke alle gjenværende steder som leste
det rå signalet, i stedet for å stole på at forfattere husker å bruke
`erRelevantForResultatet`.

## Anbefalinger (oppdatert status)

1. **Gjort, fullstendig**: invarianten `påvirkerResultat ⇒ skalKjøres` er nå
   strukturelt garantert for alle forbrukere av `relevanteVilkår`/
   `relevanteFastsettelser`, uten unntak og uten behov for regresjonstest.
2. Vurder om `påvirkerResultat` trenger en egen blokk i det hele tatt i de
   ~15 regelsett der den er ordrett lik `skalVurderes` (uendret anbefaling,
   ikke forfulgt videre denne runden).
3. `Dagpengeperiode.kt`/`SamordingUtenforFolketrygden.kt` sin asymmetri var
   slurv (bekreftet av domeneeier) og er rettet til å bruke `kravPåDagpenger`
   for begge funksjonene, i tråd med søsken-regelsettene
   `Egenandel.kt`/`DagpengenesStørrelse.kt`.

