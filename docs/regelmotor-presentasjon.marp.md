---
marp: true
theme: default
paginate: true
title: Regelmotoren i dp-behandling
description: Hvordan basis, opplysning, regelkjøring, regelverk og behandling henger sammen
---

# Regelmotoren i dp-behandling

**Tema:** Hvordan **basis**, **opplysning**, **regelkjøring**, **regelverk** og **behandling** henger sammen.

---

# Målbildet

1. Forstå hva som er juridisk/teknisk **grunnlag (basis)**.
2. Se hvordan opplysninger flyter fra behov til vurdering.
3. Se hvordan regelkjøringen bygger et resultat.
4. Koble dette til behandlingens livssyklus og vedtak.

---

# 1) Hva betyr "basis" i denne løsningen?

I praksis er "basis" det som en behandling står på:

- **Start hendelse** fra hendelsen (f.eks. søknad/meldekort).
- **Arv fra tidligere behandling** i samme kjede (materielle opplysninger).
- **Nye opplysninger** som kommer inn underveis.

**Kjernepoeng:** Ny behandling kan være *basert på* tidligere ferdig behandling, men vurderes på nytt med gjeldende regler og opplysninger.

---

# 2) Opplysning: minste byggestein

En opplysning er en verdi med juridisk betydning, og har:

- **type** (hva slags opplysning),
- **verdi** (innhold),
- **kilde** (system, saksbehandler, utledning),
- **gyldighetsperiode** (fra/til),
- **sporbarhet** (hvilke opplysninger/regler den bygger på).

Opplysninger kan være:
- **Faktum** (bekreftet), eller
- **Hypotese** (foreløpig/utledet).

---

# 3) Eksempler på opplysninger

Eksempler fra dagpenger-regelverket:

- **Søknadsdato** (dato)
- **Registrert som arbeidssøker** (boolsk)
- **Arbeidsinntekt siste 12 måneder** (beløp)
- **Oppfyller kravet til minsteinntekt** (boolsk)
- **Dagpengegrunnlag** (beløp)
- **Antall stønadsuker (stønadsperiode)** (heltall)

Nyttig skille:
- **Input-opplysninger**: kommer fra bruker/register/saksbehandler.
- **Utledede opplysninger**: beregnes av regler.

---

# 4) Regelverk: fra hjemmel til maskinell struktur

Regelverket er organisert i **regelsett**.

Hvert regelsett har:
- **hjemmel** (f.eks. paragraf),
- **type** (vilkår, fastsettelse, prosess),
- hvilke opplysninger det **produserer**,
- hvilke opplysninger det **avhenger av**,
- eventuelle **avklaringer**.

**Juridisk lesing:** Hjemler er strukturert som kjørbare avhengigheter.

---

# 5) Hvordan opplysninger utleder andre opplysninger

Utledning skjer som en kjede:

1. En regel peker på hvilke opplysninger den **avhenger av**.
2. Når disse finnes for prøvingsdatoen, beregner regelen en ny opplysning.
3. Ny opplysning lagres med **utledning** (hvilken regel + hvilke opplysninger den bygger på).
4. Gyldighetsperiode settes ut fra grunnlaget.
5. Den nye opplysningen kan igjen bli input til neste regel.

**Poeng:** Hver utledet opplysning har forklarbar "beviskjede" bakover i data og regel.

---

# 6) Regelkjøring: hva skjer faktisk?

Regelmotoren gjør dette i sløyfer:

1. Setter opp aktive regler for aktuell dato/periode.
2. Lager en produksjonsplan for ønskede resultater.
3. Kjører interne regler og utleder opplysninger.
4. Stopper der eksterne opplysninger mangler.
5. Sender **behov** om manglende data.
6. Fortsetter når svar kommer inn.

**Resultat:** Enten ferdig vurdering, eller presist informasjonsbehov.

---

# 7) Behandling: rammen rundt regelmotoren

Behandling er prosessen som styrer tilstandene:

- Under opprettelse
- Under behandling
- Forslag til vedtak
- Til godkjenning / til beslutning (ved behov)
- Ferdig eller avbrutt

Når regelkjøring er ferdig vurderes:
- finnes åpne avklaringer?
- kreves manuell kontroll/totrinn?
- kan saken ferdigstilles automatisk?

---

# 8) Avklaringer: når jussen trenger menneskelig vurdering

Avklaringer opprettes av kontrollpunkter når systemet ser:

- motstridende opplysninger,
- risikomomenter,
- forhold som må vurderes manuelt.

Avklaringer kan:
- lukkes fordi grunnlaget endres,
- lukkes systemisk,
- kvitteres av saksbehandler.

**Effekt:** Regelmotor + avklaringer gir kontrollert samspill mellom automatikk og skjønn.

---

# 9) Fra hendelse til vedtak (helhet)

Hendelse  
→ Behandling opprettes  
→ Opplysninger (arvede + nye)  
→ Regelkjøring  

Hvis data mangler:  
Regelkjøring → Behov sendes ut → Svar på opplysninger → Regelkjøring

Når regelkjøring er ferdig:  
→ Avklaringer vurderes  
→ (åpne avklaringer) Forslag til vedtak → Godkjenning/Beslutning → Ferdig vedtak  
→ (ingen avklaringer) Ferdig vedtak

---

# 10) Juridisk trygghet i modellen

Modellen gir sporbarhet på tre nivåer:

1. **Opplysningsnivå:** verdi, kilde, gyldighet, utledning.
2. **Regelnivå:** hvilke regler som er kjørt og hvorfor.
3. **Behandlingsnivå:** tilstander, avklaringer, godkjenning/beslutning.

Dette gjør det mulig å forklare:
- hvorfor resultatet ble som det ble,
- hva som manglet underveis,
- hva som krevde manuell avklaring.

---

# 11) Ordliste (juridisk ↔ teknisk)

| Juridisk begrep | I løsningen |
|---|---|
| Faktum | Opplysning (Faktum) |
| Rettslig vilkår | Regelsett av type vilkår |
| Subsumsjon | Regelkjøring/utledning |
| Vurderingsgrunnlag | Basis + opplysninger |
| Begrunnelse/spor | Utledning + hendelses- og tilstandsspor |

---

# 12) Kilder (internt i repo)

- `docs/README.md` (overordnet behandlingsflyt)
- `docs/opplysning/README.md` (begreper og modell)
- `docs/opplysninger.approved.md` (regelsett, avhengigheter, roller)
- `docs/regeltre-dagpenger.approved.md` (regeltre)

Supplerende kode:
- `opplysninger/.../Opplysning.kt`, `Opplysninger.kt`
- `opplysninger/.../Regelverk.kt`, `Regelsett.kt`, `Regelkjøring.kt`
- `modell/.../Behandling.kt`
