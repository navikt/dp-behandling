# 📋 Juridisk Domene-Review: Kvoteallokering i dp-behandling

**Dato:** 2024  
**Reviewer rolle:** Juridisk/domene-reviewer  
**Status:** ⚠️ **TRENGER JUSTERING**

---

## Executive Summary

Kvotemodellen har **god struktur for sporbarhet** og **riktig lovhjemmel**, men implementasjonen **mangler sanksjonshierarki**. Feltene `forbruksrekkefølge`, `prioritetIGruppe` og `forbruksgruppe` er **deklarert men aldri brukt**. 

**Kritisk funn:** Sanksjonsperiode (§ 4-10) og Tidsbegrenset bortfall (§ 4-20) forbrukes **parallelt, ikke etterfølgende**. Dette strider mot gjeldende juridisk forståelse.

---

## 1. Konsepter og Navn ✅ (med reservasjoner)

### `ETTERFØLGENDE` og `PARALLELL`

| ✅ Sterke sider | ⚠️ Problemer |
|---|---|
| Klare, engelske fagtermer | **Feltet er deklarert men brukes ALDRI** |
| Gjenspeiler juridisk virkelighet | Implementasjonen gjør PARALLELL uavhengig av konfigurasjonen |
| Internasjonalt forståelig | Ingen implementert logikk for prioritering |

**Juridisk navn-forslag:**
- `ETTERFØLGENDE` → Behold (betyr: forbruk sekvensielt etter prioritet)
- `PARALLELL` → Behold (betyr: forbruk samtidig)

**Funn i kode:**
```kotlin
// Sanksjonsperiode.kt
forbruksrekkefølge = Forbruksrekkefølge.ETTERFØLGENDE,  // ❌ ALDRI BRUKT
prioritetIGruppe = 1,                                    // ❌ ALDRI BRUKT

// TidsbegrensetBortfall.kt  
forbruksrekkefølge = Forbruksrekkefølge.ETTERFØLGENDE,  // ❌ ALDRI BRUKT
prioritetIGruppe = 2,                                    // ❌ ALDRI BRUKT
```

---

### `prioritetIGruppe` – Mulig bedre navn

| Forslag | Vurdering |
|---------|-----------|
| `prioritetIGruppe` | ✅ Presist teknisk navn. Uklart for jurist. |
| `sanksjonshierarki` | ⚠️ For spesifikk, bare for sanksjoner. |
| `forbruks­prioritet` | ⚠️ Teknisk språk. |
| `rekkefølgekvenns` | ❌ Ikke-eksisterende ord. Unnga. |
| **`prosessorden` eller `forbruksorden`** | ✅ Juridisk forståelig. |

**Anbefaling:** Behold `prioritetIGruppe` som teknisk navn, men **dokumenter tydeelig** at det handler om **sanksjonshierarki** (prioritet 1 = sanksjonsperiode § 4-10 forbrukes først).

---

### `forbruksgruppe` – Navn er OK

✅ Godt navn. Klart at det grupperer relaterte kvotekonsepter.

**Bruk i dag:**
- `forbruksgruppe = "sanksjon"` → For både Sanksjonsperiode og TidsbegrensetBortfall
- `forbruksgruppe = "stønadsdag"` → For Dagpengeperiode

**Problem:** Begge sanksjonstyper er i samme gruppe, men skal ha **ulik prioritet**. Gruppering alene holder ikke.

---

## 2. Hierarki og Prioritet ❌ FEIL IMPLEMENTERT

### Lovkrav (Dagpengeloven)

**✅ Lovhjemler er riktig definert:**
```kotlin
Sanksjonsperiode:          § 4-10 (gjeldende hjemmel for sanksjonsperiode)
TidsbegrensetBortfall:     § 4-20 (tidsbegrenset bortfall av dagpenger)
Dagpengeperiode:           § 4-4  (dagpengeperioden)
```

**✅ Rekkefølge er juridisk korrekt:**
- § 4-10 skal **forbrukes FØR** § 4-20
- § 4-20 skal **forbrukes FØR** stønadsdager (§ 4-4)

### Implementering – KRITISK FEIL

**❌ Faktisk oppførsel:**

I `BeregningsperiodeFabrikk.hentGjenståendeBortfall()`:
```kotlin
private fun hentGjenståendeBortfall(førsteDag: LocalDate): Int =
    kvoter
        .filter { it.forbrukKriterium == Beregning.erBortfallsdag }
        .sumOf { kvote -> kvote.gjenståendeVed(opplysninger, førsteDag) }  // ⚠️ SUMMERER ALT
```

**Problemet:**
- Både Sanksjonsperiode og TidsbegrensetBortfall bruker **samme forbrukKriterium**: `Beregning.erBortfallsdag`
- Funksjonen **summerer alle gjenstående bortfallsdager** uten å respektere prioritet
- Resultat: **Begge forbrukes samtidig (PARALLELT)** uavhengig av prioritet

**Testbevis fra commit `0d5d1b8a5`:**
```kotlin
// Test: Sanksjonsperiode 2 dager + TidsbegrensetBortfall 5 dager
// Forventet (juridisk): Sanksjon først (2), så bortfall (5)
// Faktisk (gjeldende kode): Summert til 7 dager forbrukt samtidig
// 7 dager markert som bortfall (5 + 2 fra to samtidige sanksjoner)
```

### Konsekvens

En person med **både sanksjonsperiode og tidsbegrenset bortfall** vil få **feil allokering**:

| Dag | Lovmessig korrekt | Gjeldende kode |
|-----|---------|---------|
| 1   | Sanksjon (prioritet 1) | Sanksjon + Bortfall (blandet) |
| 2   | Sanksjon (prioritet 1) | Sanksjon + Bortfall (blandet) |
| 3   | Bortfall (prioritet 2) | Sanksjon + Bortfall (blandet) |
| 4   | Bortfall (prioritet 2) | Sanksjon + Bortfall (blandet) |
| 5   | Bortfall (prioritet 2) | Sanksjon + Bortfall (blandet) |
| 6   | Bortfall (prioritet 2) | Sanksjon + Bortfall (blandet) |
| 7   | Bortfall (prioritet 2) | Sanksjon + Bortfall (blandet) |
| 8-N | Stønadsdager | Stønadsdager | 

**Juridisk vurdering:** ❌ **FEIL**

---

## 3. Sporbarhet ✅ (med ressurser)

### Kan du se hvilken sanksjon som ligger på hvilken dag?

**✅ JA – når det er bare en type.**

For Sanksjonsperiode alene:
- `Beregning.erBortfallsdag` → samme for alle sanksjoner
- `Beregning.forbruktSanksjonsdager` → teller ned
- `Beregning.gjenståendeSanksjonsdager` → viser rest
- `Beregning.sisteSanksjonsdagMedForbruk` → siste dag med aktivitet

**❌ PROBLEM: Kan IKKE skille Sanksjonsperiode fra TidsbegrensetBortfall**

Begge bruker samme `erBortfallsdag`-flagg:
```kotlin
// Sanksjonsperiode
forbrukKriterium = Beregning.erBortfallsdag

// TidsbegrensetBortfall  
forbrukKriterium = Beregning.erBortfallsdag

// Resultat: Ingen måte å vite fra opplysningene alene hvilken type bortfall
```

**Konsekvens for revisjon/audit:** En revisor kan se at dag 1-3 er "bortfall", men IKKE se at dag 1-2 er sanksjonsperiode og dag 3 er tidsbegrenset bortfall.

### Sporbarhet for revisjoner

| Spørsmål | Mulig? | Ressurs |
|---------|--------|---------|
| Antall forbrukte sanksjonsdager? | ✅ JA | `forbruktSanksjonsdager` |
| Antall forbrukte bortfallsdager? | ✅ JA | `forbruktBortfallsdager` |
| Hvilken dag har sanksjon? | ⚠️ DELVIS | Trenger tidsserier + lovhjemmel |
| Hvorfor ble dag X markert som bortfall? | ❌ NEI | Mangler årsak/hierarki |
| Kan rekalkulere allokering? | ✅ JA | Kan køre på nytt fra kvotedefinisjon |

---

## 4. Utvidbarhet ⚠️ Modellen strekker, men ikke implementert

### Hvis NAV legger inn § 4-30

**✅ Modellen STREKKER for nye regler:**

```kotlin
// Eksempel: Ny regel § 4-30 (f.eks. "Høy sykefravær")
KvoteDefinisjon(
    hjemmel = hjemmel(4, 30, "..."),
    kilder = listOf(KvoteKilde(antallDagerFraSykefravær, aktiveresAv = harHøytSykefravær)),
    forbrukKriterium = Beregning.erBortfallsdag,  // ✅ Kan gjenbruke
    forbruktTeller = Beregning.forbruktSykefraværsdager,  // ✅ Ny type
    gjenstående = Beregning.gjenståendeSykefraværsdager,  // ✅ Ny type
    forbruksrekkefølge = Forbruksrekkefølge.ETTERFØLGENDE,
    forbruksgruppe = "fravær",  // ✅ Ny gruppe
    prioritetIGruppe = 3,  // ✅ Etter både sanksjon og bortfall
)
```

**✅ Fordeler:**
- `KvoteDefinisjon` er generell og utvidbar
- `Hjemmel`-feltet gjør det enkelt å linke til lovparagraf
- Separate teller for hver type sikrer sporbarhet

**❌ Problemer for utvidelse:**
- `prioritetIGruppe` er **aldri implementert**, så ny regel blir automatisk PARALLELL
- Må implementere prioriterings-logikk før nye regler fungerer korrekt

---

### Hvis egenandel blir kompleks

**Status i dag:**
```kotlin
Beregning.forbruktEgenandel      // Beløp
Beregning.gjenståendeEgenandel   // Beløp (som Beløp, ikke Int)
```

**✅ Modellen kan håndtere:**
- Flere kilder til egenandel (f.eks. inntektsbasert + fast)
- Oppdateringer av egenandel underveis
- Gruppering av egenandel-kilder

**Eksempel:**
```kotlin
// Hvis egenandel skulle bli mer kompleks
KvoteDefinisjon(
    hjemmel = hjemmel(4, 2, "Egenandel"),
    kilder = listOf(
        KvoteKilde(fasttEgenandel),
        KvoteKilde(inntektsbasertEgenandel, aktiveresAv = harAlternativInntekt)
    ),
    forbrukKriterium = Beregning.erEgenandelDag,  // ✅ Kan legge til
    // ...
)
```

**❌ Problem:** Egenandel er nå behandlet annerledes enn sanksjoner/bortfall. Ingen kvote-definisjon for den. Asymmetrisk design.

---

### Hvis sanksjoner blir påvirket av inntektsendringer underveis

**Scenario:** Person får inntekt midt i sanksjonsperioden → sanksjon skal oppheves.

**Status i dag:**
- Sanksjonsperiode er **én gang beregnet ved vedtak**
- Hvis inntekt endrer seg, må man gjøre **omgjøring**
- Systemet støtter ikke **dynamisk sanksjonsendring underveis i periode**

**Modellen strekker, men implementasjonen gjør det ikke:**
```kotlin
// KvoteDefinisjon kan støtte aktivering/deaktivering:
KvoteKilde(antallSanksjonsdager, aktiveresAv = harSanksjon)  // ✅ Eksisterer!

// Men det finnes ingen logikk for å:
// 1. Sjekke aktiveresAv hver dag
// 2. Deaktivere kvote hvis aktiveresAv blir false
// 3. Omallokere dager hvis sanksjon fjernes
```

---

## 5. Lesbarhet for saksbehandler / jurist ❌ Problematisk

### Kunne en saksbehandler lese koden og forstå at det er lovmessig korrekt?

**❌ NEI – koden er **uklar på sanksjonshierarki**.**

Problemer:
1. **Hierarki er "skjult" i `prioritetIGruppe`** – som ALDRI brukes
2. **Samme `forbrukKriterium` for begge sanksjoner** → gjør det umulig å skille dem i kode
3. **Kommentar mangler** som forklarer § 4-10 skal komme før § 4-20
4. **Logger-output** fra test viser "5 + 2 dager" er summert, ikke hierarkisert

### Kunne en jurist verifisere at reglene følges?

**⚠️ DELVIS – bare hvis juridisk sakkunnskap er høy.**

En erfaren dagpenger-jurist kan se:
- ✅ Lovhjemlene er riktig sitert (§ 4-10 og § 4-20)
- ✅ Kvotene seppes ut som egne `KvoteDefinisjon`-er
- ❌ **MEN**: Det er uklart at prioritet blir respektert
- ❌ **Kode som viser at prioritet ikke implementeres** (`sumOf` uten prioritering)

**For å forstå det kreves:**
- Kjennskap til Kotlin + data structures
- Forståelse av at `prioritetIGruppe` aldri brukes
- Innsikt i at begge bruker samme `forbrukKriterium`

**Konklusjon:** En saksbehandler ville **IKKE** forstå systemet. En jurist **ville mistenke** noe var galt.

---

### Er det tydelig at dette handler om sanksjonshierarki (ikke bare teknisk allokering)?

**❌ NEI.**

Dagens design fremstår som "generell kvoteallokering" – som tilfeldigvis brukes til sanksjoner. Det er **ikke tydelig at prioritet er juridisk påkrevd**.

**Bedre design ville være:**

```kotlin
// I STEDET FOR denne generelle modellen:
data class KvoteDefinisjon(
    // ... generell kvote-logikk
    forbruksrekkefølge: Forbruksrekkefølge = PARALLELL,  // ❌ Ikke brukt
    prioritetIGruppe: Int? = null,                       // ❌ Ikke brukt
)

// LAG EN EKSPLISITT SANKSJONSHIERARKI-MODELL:
data class SanksjonsAllokeringStrategy(
    val hierarki: List<Sanksjonsnivå> = listOf(
        Sanksjonsnivå(§ = "4-10", navn = "Selvforskyldt arbeidsløshet", prioritet = 1),
        Sanksjonsnivå(§ = "4-20", navn = "Tidsbegrenset bortfall", prioritet = 2),
    )
)

// ELLER: Lag eksplisitt rekkefølge-objekt:
sealed class Forbrukslogikk {
    class Hierarki(val prioriteter: List<Pair<Hjemmel, Int>>) : Forbrukslogikk()
    class Parallell : Forbrukslogikk()
}
```

Nå ville det være **umiddelbar klar** at sanksjonshierarki er juridisk påkrevd.

---

## 6. Spesifikke funn per punkt

| Punkt | Status | Funn |
|-------|--------|------|
| **§ 4-10 før § 4-20?** | ❌ FEIL | Hierarki er deklarert men ikke implementert |
| **Sanksjoner forbrukes før stønadsdager?** | ✅ JA | Både sanksjons- og bortfallsdager forbrukes FØR stønadsdager |
| **Mulighet for andre hierarkier?** | ⚠️ DELVIS | Modellen tillater det, men logikk er ikke implementert |
| **Sporbarhet per dag?** | ⚠️ DELVIS | Per type (sanksjon vs. bortfall), men ikke per undertype |
| **Rekalkulering mulig?** | ✅ JA | Kan kjøre Kvoteteller på nytt fra definisjon |
| **Juridisk tydelig?** | ❌ NEI | Krever dyp Kotlin-kunskap for å forstå hierarki |

---

## 7. Forslag til forbedringer

### 🔴 KRITISK (må fikses før produksjon)

**1. Implementer prioritets-logikk i `hentGjenståendeBortfall()`**

```kotlin
private fun hentGjenståendeBortfall(førsteDag: LocalDate): Int =
    kvoter
        .filter { it.forbrukKriterium == Beregning.erBortfallsdag }
        .sortedBy { it.prioritetIGruppe ?: Int.MAX_VALUE }  // 🔧 SORTER PÅ PRIORITET
        .sumOf { kvote -> kvote.gjenståendeVed(opplysninger, førsteDag) }

// ELLER: Hvis man skal ha heterogen gruppe (sanksjon + bortfall):
private fun hentGjenståendeBortfall(førsteDag: LocalDate): Int {
    val kvotePrioriert = kvoter
        .filter { it.forbrukKriterium == Beregning.erBortfallsdag }
        .groupBy { it.forbruksgruppe }
        .flatMap { (gruppe, kvotesIGruppe) ->
            kvotesIGruppe.sortedBy { it.prioritetIGruppe ?: Int.MAX_VALUE }
        }
    // Nå kan Beregningsperiode allokere dem sekvensielt basert på prioritet
    return kvotePrioriert.sumOf { kvote -> kvote.gjenståendeVed(opplysninger, førsteDag) }
}
```

**Alternativ (bedre):** Pass liste av prioriterte bortfallsdager direkte til `Beregningsperiode`, som så allokerer dem i rekkefølge.

**2. Lag separate opplysningstyper for hver sanksjon**

```kotlin
// I STEDET FOR:
Beregning.erBortfallsdag  // Brukes for både sanksjon og bortfall

// BRUK:
Beregning.erSanksjondag           // Sanksjonsperiode § 4-10
Beregning.erTidsbegrensetBortfall // Tidsbegrenset bortfall § 4-20
Beregning.erBortfallsdag          // Alias for "any bortfall"?
```

Dette gjør sporing + juridisk klarhet mye bedre.

**3. Dokumenter hierarki eksplisitt**

```kotlin
/**
 * Sanksjonsallokering følger Dagpengeloven med prioritet:
 * 1. § 4-10: Sanksjonsperiode ved selvforskyldt arbeidsløshet (prioritet 1)
 * 2. § 4-20: Tidsbegrenset bortfall av dagpenger (prioritet 2)
 * 3. § 4-4:  Ordinære stønadsdager (ingen prioritet, parallelt forbruk)
 *
 * Dette sikrer at sanksjoner ALLTID forbrukes før bortfall, og begge før ordinær utbetaling.
 */
private fun hentGjenståendeBortfall(førsteDag: LocalDate): Int = ...
```

### 🟡 VIKTIG (bør fikses snart)

**4. Rename for juridisk klarhet**

- Behold `Forbruksrekkefølge.ETTERFØLGENDE` og `PARALLELL`
- Behold `prioritetIGruppe`
- **LAG KOMMENTAR i KvoteDefinisjon som forklarer at dette handler om sanksjonshierarki**

```kotlin
data class KvoteDefinisjon(
    // ...
    /**
     * Prioritet innenfor forbruksgruppe.
     * Brukes til å ordne kvoter SEKVENSIELT når forbruksrekkefølge = ETTERFØLGENDE.
     * Eksempel: § 4-10 (prioritet 1) forbrukes før § 4-20 (prioritet 2).
     */
    val prioritetIGruppe: Int? = null,
)
```

**5. Separer sanksjon fra egenandel i design**

Egenandel burde ha sin egen allokerings-strategi (ikke være blandet med sanksjon/bortfall).

**6. Legg til test som viser hierarki**

```kotlin
@Test
fun `sanksjonsperiode forbrukes før tidsbegrenset bortfall` () {
    // Person med 2 dagers sanksjon + 5 dagers bortfall
    // Meldekort: 7 arbeidsdager
    // Forventet: dag 1-2 sanksjon, dag 3-7 bortfall, dag 8-N normal utbetaling
    // IKKE: alle 7 dager "blandet" som nå
}
```

### 🟢 ØNSKELIG (forbedring)

**7. Eksplisitt `SanksjonHierarki`-modell**

I stedet for den generelle `KvoteDefinisjon` for sanksjoner, lag:

```kotlin
data class SanksjonsDag(
    val dato: LocalDate,
    val sanksjonType: SanksjonType,  // SANKSJONSPERIODE, BORTFALL, osv.
    val hjemmel: Hjemmel,
)

enum class SanksjonType {
    SANKSJONSPERIODE,           // § 4-10, prioritet 1
    TIDSBEGRENSET_BORTFALL,     // § 4-20, prioritet 2
    ;
    val prioritet: Int get() = when (this) {
        SANKSJONSPERIODE -> 1
        TIDSBEGRENSET_BORTFALL -> 2
    }
}
```

Dette gjør det **umulig å implementere feil**, og **all saksbehandler/jurist** forstår umiddelbart at hierarki er påkrevd.

---

## Sammenfatning: Juridisk korrekthet

| Område | Status | Alvorlighetsgrad |
|--------|--------|------------------|
| **Lovhjemler** | ✅ Korrekt | ✓ OK |
| **Sanksjonshierarki implementert** | ❌ FEIL | 🔴 KRITISK |
| **Sporbarhet** | ✅ Tilstrekkelig | ✓ OK |
| **Revisor kan verifisere** | ⚠️ Vanskelig | 🟡 VIKTIG |
| **Saksbehandler forstår juridikk** | ❌ NEI | 🟡 VIKTIG |
| **Kode reflekterer juridikk** | ❌ NEI | 🟡 VIKTIG |
| **Utvidbar for nye regler** | ✅ JA (design-nivå) | ✓ OK |

---

## Konklusjon

### Juridisk vurdering: ⚠️ **TRENGER JUSTERING**

✅ **Sterke sider:**
- Lovhjemler er riktig sitert
- Kvote-modellen er generell og utvidbar
- Sporbarhet per sanksjon-type er implementert
- Design-nivået er godt

❌ **Kritiske feil:**
- Sanksjonshierarki (§ 4-10 før § 4-20) er **ALDRI implementert**
- Begge sanksjonstyper forbrukes **PARALLELT** i stedet for **ETTERFØLGENDE**
- `prioritetIGruppe` og `forbruksrekkefølge` er deklarert men aldri brukt
- Juridisk logikk er ikke tydelig i koden

⚠️ **Risiko:**
- Person med sanksjonsperiode + bortfall får **FEIL allokering**
- Revisor kan **IKKE verifisere** at systemet følger loven
- Saksbehandler forstår **IKKE** at hierarki er påkrevd

### Anbefalte neste steg:

1. **KRITISK:** Implementer prioritets-logikk så sanksjonsperiode (prioritet 1) alltid forbrukes FØR bortfall (prioritet 2)
2. **VIKTIG:** Lag eksplisitt test som viser korrekt hierarki
3. **VIKTIG:** Dokumenter juridikken direkte i koden med kommentarer om § 4-10 og § 4-20
4. **ØNSKELIG:** Refaktor til eksplisitt `SanksjonHierarki`-modell for å gjøre det umulig å implementere feil
5. **ØNSKELIG:** Separer egenandel-logikk fra sanksjon-logikk

---

## Vedlegg: Kildekoder referert

- `opplysninger/src/main/kotlin/no/nav/dagpenger/opplysning/Kvote.kt` – KvoteDefinisjon
- `dagpenger/src/main/kotlin/no/nav/dagpenger/regel/regelsett/beregning/Beregningsperiode.kt` – Allokerings-logikk
- `dagpenger/src/main/kotlin/no/nav/dagpenger/regel/regelsett/beregning/BeregningsperiodeFabrikk.kt` – hentGjenståendeBortfall()
- `dagpenger/src/main/kotlin/no/nav/dagpenger/regel/regelsett/vilkår/Sanksjonsperiode.kt` – § 4-10
- `dagpenger/src/main/kotlin/no/nav/dagpenger/regel/regelsett/vilkår/TidsbegrensetBortfall.kt` – § 4-20
- `mediator/src/test/kotlin/no/nav/dagpenger/scenario/SanksjonTest.kt` – Test som viser parallell forbruk
- Git commit `0d5d1b8a5` – Evidens for parallelt forbruk

