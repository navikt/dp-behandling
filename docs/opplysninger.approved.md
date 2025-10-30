# Dokumentasjon av opplysninger

Dette er opplysninger som blir brukt av regelverket. 

 UUID og datatype er en unik identifikator for opplysningstypen. Den skal _ALDRI_ endres. Beskrivelse og behovId kan endres. 
 
 For nye opplysningtyper, generer en ny UUID og legg til.
 
 Generering av UUID kan gjøres med UUIDv7.ny() i Kotlin
## Regelsett
### § 0-0. Meldekortberegning
*Type:* Fastsettelse
#### Opplysninger
|UUID|Beskrivelse|Logisk datatype|Datatype|Behov|
|---|---|---|---|---|
|01948ea0-36e8-72cc-aa4f-16bc446ed3bd|Arbeidsdag|Boolsk|boolean||
|01948ea0-e25c-7c47-8429-a05045d80eca|Arbeidstimer på en arbeidsdag|Desimaltall|double||
|01948ea0-ffdc-7964-ab55-52a7e35e1020|Dag som fører til forbruk av dagpengeperiode|Boolsk|boolean||
|01948ea2-22f3-7da8-9547-90d0c64e74e0|Terskel for hvor mye arbeid som kan utføres samtidig med dagpenger|Desimaltall|double||
|01956ab8-126c-7636-803e-a5d87eda2015|Har meldt seg via meldekort|Boolsk|boolean||
|01956abd-2871-7517-a332-b462c0c31292|Meldeperiode|PeriodeDataType|Periode||
|01957069-d7d5-7f7c-b359-c00686fbf1f7|Penger som skal utbetales|Penger|Beløp||
|01973a27-d8b3-7ffd-a81a-a3826963b079|Forbrukt egenandel|Penger|Beløp||
|01992934-66e4-7606-bdd3-c6c9dd420ffd|Antall dager som er forbrukt|Heltall|int||
|01992956-e349-76b1-8f68-c9d481df3a32|Antall dager som gjenstår|Heltall|int||
|01994cfd-9a27-762e-81fa-61f550467c95|Penger som skal utbetales for perioden|Penger|Beløp||
|01997b70-6e6e-702a-a296-18ae5fb9621d|Oppfyller kravet til tapt arbeidstid i perioden|Boolsk|boolean||
|01997b70-a12c-7622-bff8-82a20687e640|Gjenstående egenandel|Penger|Beløp||
### § 0-0. Krav på dagpenger
*Type:* Vilkår
#### Opplysninger
|UUID|Beskrivelse|Logisk datatype|Datatype|Behov|
|---|---|---|---|---|
|01990a09-0eab-7957-b88f-14484a50e194|Har løpende rett på dagpenger|Boolsk|boolean||
### § 0-0. Rettighetstype
*Type:* Fastsettelse
#### Avhenger på data fra
- [§ 3-1. Søknadstidspunkt](#-3-1-søknadstidspunkt)
- [§ 4-19. Dagpenger etter avtjent verneplikt](#-4-19-dagpenger-etter-avtjent-verneplikt)
#### Opplysninger
|UUID|Beskrivelse|Logisk datatype|Datatype|Behov|
|---|---|---|---|---|
|0194881f-9444-7a73-a458-0af81c034d85|Har rett til ordinære dagpenger gjennom arbeidsforhold|Boolsk|boolean|Ordinær|
|0194881f-9444-7a73-a458-0af81c034d86|Bruker er permittert|Boolsk|boolean|Permittert|
|0194881f-9444-7a73-a458-0af81c034d87|Forskutterte lønnsgarantimidler i form av dagpenger|Boolsk|boolean|Lønnsgaranti|
|0194881f-9444-7a73-a458-0af81c034d88|Permittert fra fiskeindustrien|Boolsk|boolean|PermittertFiskeforedling|
|0194881f-9444-7a73-a458-0af81c034d89|Har rett til ordinære dagpenger uten arbeidsforhold|Boolsk|boolean||
|0194881f-9444-7a73-a458-0af81c034d8a|Ordinære dagpenger|Boolsk|boolean||
|0194881f-9444-7a73-a458-0af81c034d8b|Rettighetstype|Boolsk|boolean||
|0194ff86-a035-7eb0-9c60-21899f7cc0c1|Kravet til reell arbeidssøker er vurdert|Boolsk|boolean||
|01980cf4-9010-7bcf-b578-ca5a825d64ef|Skal kravet til verneplikt vurderes|Boolsk|boolean||
### § 3-1. Søknadstidspunkt
*Type:* Fastsettelse
#### Avklaringer
- VirkningstidspunktForLangtFramItid - [Virkningstidspunkt ligger for langt fram i tid](./avklaringer.approved.md#virkningstidspunkt-ligger-for-langt-fram-i-tid)
#### Opplysninger
|UUID|Beskrivelse|Logisk datatype|Datatype|Behov|
|---|---|---|---|---|
|0194881f-91d1-7df2-ba1d-4533f37fcc73|Søknadsdato|Dato|LocalDate|Søknadsdato|
|0194881f-91d1-7df2-ba1d-4533f37fcc74|Ønsker dagpenger fra dato|Dato|LocalDate|ØnskerDagpengerFraDato|
|0194881f-91d1-7df2-ba1d-4533f37fcc75|Søknadstidspunkt|Dato|LocalDate||
|0194881f-91d1-7df2-ba1d-4533f37fcc76|Prøvingsdato|Dato|LocalDate||
|0194881f-91d1-7df2-ba1d-4533f37fcc77|søknadId|Tekst|String|søknadId|
### § 4-2. Opphold i Norge
*Type:* Vilkår
#### Avklaringer
- Bostedsland - [Bruker har oppgitt bostedsland som ikke er Norge](./avklaringer.approved.md#bruker-har-oppgitt-bostedsland-som-ikke-er-norge)
#### Avhenger på data fra
- [§ 3-1. Søknadstidspunkt](#-3-1-søknadstidspunkt)
#### Opplysninger
|UUID|Beskrivelse|Logisk datatype|Datatype|Behov|
|---|---|---|---|---|
|0194881f-9443-72b4-8b30-5f6cdb24d549|Bruker oppholder seg i Norge|Boolsk|boolean||
|0194881f-9443-72b4-8b30-5f6cdb24d54a|Oppfyller unntak for opphold i Norge|Boolsk|boolean||
|0194881f-9443-72b4-8b30-5f6cdb24d54b|Oppfyller kravet til opphold i Norge eller unntak|Boolsk|boolean||
|0194881f-9443-72b4-8b30-5f6cdb24d54c|Bruker er medlem av folketrygden|Boolsk|boolean||
|0194881f-9443-72b4-8b30-5f6cdb24d54d|Oppfyller kravet til medlemskap|Boolsk|boolean||
|0194881f-9443-72b4-8b30-5f6cdb24d54e|Kravet til opphold i Norge er oppfylt|Boolsk|boolean||
|0196ab10-0cff-7301-99d6-65be50a50201|Bostedsland er Norge|Boolsk|boolean|BostedslandErNorge|
### § 1-2. Frist for levering av opplysninger
*Type:* Fastsettelse
#### Avhenger på data fra
- [§ 3-1. Søknadstidspunkt](#-3-1-søknadstidspunkt)
#### Opplysninger
|UUID|Beskrivelse|Logisk datatype|Datatype|Behov|
|---|---|---|---|---|
|0194881f-9414-7823-8d29-0e25b7feb7ce|Lovpålagt rapporteringsfrist for A-ordningen|Dato|LocalDate||
|0194881f-9414-7823-8d29-0e25b7feb7cf|Arbeidsgivers rapporteringsfrist|Dato|LocalDate||
|0194881f-9414-7823-8d29-0e25b7feb7d0|Siste avsluttende kalendermåned|Dato|LocalDate||
### § 4-3. Tap av arbeidsinntekt og arbeidstid
*Type:* Vilkår
#### Avklaringer
- TapAvArbeidsinntektOgArbeidstid - [Velg kun en beregningsregel for tap av arbeidsinntekt og arbeidstid](./avklaringer.approved.md#velg-kun-en-beregningsregel-for-tap-av-arbeidsinntekt-og-arbeidstid)
- BeregnetArbeidstid - [Sjekk om beregnet arbeidstid er korrekt](./avklaringer.approved.md#sjekk-om-beregnet-arbeidstid-er-korrekt)
#### Avhenger på data fra
- [§ 0-0. Rettighetstype](#-0-0-rettighetstype)
- [§ 3-1. Søknadstidspunkt](#-3-1-søknadstidspunkt)
- [§ 4-3. Fastsettelse av arbeidstid](#-4-3-fastsettelse-av-arbeidstid)
- [§ 6-7. Permittering i fiskeforedlingsindustrien, sjømatindustrien og fiskeoljeindustrien](#-6-7-permittering-i-fiskeforedlingsindustrien,-sjømatindustrien-og-fiskeoljeindustrien)
- [§ 4-19. Dagpenger etter avtjent verneplikt](#-4-19-dagpenger-etter-avtjent-verneplikt)
#### Opplysninger
|UUID|Beskrivelse|Logisk datatype|Datatype|Behov|
|---|---|---|---|---|
|0194881f-9435-72a8-b1ce-9575cbc2a75e|Har krav på lønn fra arbeidsgiver|Boolsk|boolean|AndreØkonomiskeYtelser|
|0194881f-9435-72a8-b1ce-9575cbc2a761|Oppfyller vilkåret til tap av arbeidsinntekt|Boolsk|boolean||
|0194881f-9435-72a8-b1ce-9575cbc2a762|Krav til prosentvis tap av arbeidstid|Desimaltall|double||
|0194881f-9435-72a8-b1ce-9575cbc2a763|Beregningsregel: Tapt arbeidstid|Boolsk|boolean||
|0194881f-9435-72a8-b1ce-9575cbc2a764|Beregningsregel: Arbeidstid siste 6 måneder|Boolsk|boolean||
|0194881f-9435-72a8-b1ce-9575cbc2a765|Beregningsregel: Arbeidstid siste 12 måneder|Boolsk|boolean||
|0194881f-9435-72a8-b1ce-9575cbc2a766|Beregningsregel: Arbeidstid siste 36 måneder|Boolsk|boolean||
|0194881f-9435-72a8-b1ce-9575cbc2a767|Beregnet vanlig arbeidstid per uke før tap|Desimaltall|double||
|0194881f-9435-72a8-b1ce-9575cbc2a768|Maksimal vanlig arbeidstid|Desimaltall|double||
|0194881f-9435-72a8-b1ce-9575cbc2a76b|Ny arbeidstid per uke|Desimaltall|double||
|0194881f-9435-72a8-b1ce-9575cbc2a76c|Fastsatt vanlig arbeidstid etter ordinær eller verneplikt|Desimaltall|double||
|0194881f-9435-72a8-b1ce-9575cbc2a76e|Oppfyller vilkåret om tap av arbeidstid|Boolsk|boolean||
|0194881f-9435-72a8-b1ce-9575cbc2a76f|Oppfyller vilkåret om tap av arbeidsinntekt og arbeidstid|Boolsk|boolean||
|019522d6-846d-7173-a892-67f10016d8d2|Ordinært krav til prosentvis tap av arbeidstid|Desimaltall|double||
|0196b4a7-23b5-7b2c-aa95-e4167d900de8|Arbeidstidsreduksjonen er ikke brukt tidligere i en full stønadsperiode|Boolsk|boolean||
### § 4-3. Fastsettelse av arbeidstid
*Type:* Fastsettelse
#### Avhenger på data fra
- [§ 4-3. Tap av arbeidsinntekt og arbeidstid](#-4-3-tap-av-arbeidsinntekt-og-arbeidstid)
- [§ 4-5. Reelle arbeidssøkere](#-4-5-reelle-arbeidssøkere)
- [§ 4-25. Samordning med reduserte ytelser fra folketrygden, eller redusert avtalefestet pensjon](#-4-25-samordning-med-reduserte-ytelser-fra-folketrygden,-eller-redusert-avtalefestet-pensjon)
#### Opplysninger
|UUID|Beskrivelse|Logisk datatype|Datatype|Behov|
|---|---|---|---|---|
|0194881f-9435-72a8-b1ce-9575cbc2a76a|Fastsatt arbeidstid per uke før tap|Desimaltall|double||
### § 4-4. Krav til minsteinntekt
*Type:* Vilkår
#### Avklaringer
- SvangerskapsrelaterteSykepenger - [Sjekk om søker har fått sykepenger på grunn av svangerskap som skal med i minsteinntekt](./avklaringer.approved.md#sjekk-om-søker-har-fått-sykepenger-på-grunn-av-svangerskap-som-skal-med-i-minsteinntekt)
- InntektNesteKalendermåned - [Sjekk om inntekt for neste måned er relevant](./avklaringer.approved.md#sjekk-om-inntekt-for-neste-måned-er-relevant)
#### Avhenger på data fra
- [§ 3-1. Søknadstidspunkt](#-3-1-søknadstidspunkt)
- [§ 1-2. Frist for levering av opplysninger](#-1-2-frist-for-levering-av-opplysninger)
#### Opplysninger
|UUID|Beskrivelse|Logisk datatype|Datatype|Behov|
|---|---|---|---|---|
|0194881f-9413-77ce-92ec-d29700f0423f|Antall G for krav til 12 mnd arbeidsinntekt|Desimaltall|double||
|0194881f-9413-77ce-92ec-d29700f04240|Antall G for krav til 36 mnd arbeidsinntekt|Desimaltall|double||
|0194881f-9413-77ce-92ec-d29700f04241|Arbeidsinntekt siste 12 måneder|Penger|Beløp||
|0194881f-9413-77ce-92ec-d29700f04242|Arbeidsinntekt siste 36 måneder|Penger|Beløp||
|0194881f-9413-77ce-92ec-d29700f04243|Grunnbeløp|Penger|Beløp||
|0194881f-9413-77ce-92ec-d29700f04244|Inntektsopplysninger|InntektDataType|Inntekt|Inntekt|
|0194881f-9413-77ce-92ec-d29700f04245|Brutto arbeidsinntekt|InntektDataType|Inntekt||
|0194881f-9413-77ce-92ec-d29700f04246|Maks lengde på opptjeningsperiode|Heltall|int||
|0194881f-9413-77ce-92ec-d29700f04247|Første måned av opptjeningsperiode|Dato|LocalDate||
|0194881f-9413-77ce-92ec-d29700f04248|Inntektskrav for siste 12 måneder|Penger|Beløp||
|0194881f-9413-77ce-92ec-d29700f04249|Inntektskrav for siste 36 måneder|Penger|Beløp||
|0194881f-9413-77ce-92ec-d29700f0424a|Arbeidsinntekt er over kravet for siste 12 måneder|Boolsk|boolean||
|0194881f-9413-77ce-92ec-d29700f0424b|Arbeidsinntekt er over kravet for siste 36 måneder|Boolsk|boolean||
|0194881f-9413-77ce-92ec-d29700f0424c|Oppfyller kravet til minsteinntekt|Boolsk|boolean||
### § 4-5. Reelle arbeidssøkere
*Type:* Vilkår
#### Avklaringer
- ReellArbeidssøkerUnntak - [Sjekk om søker oppfyller unntak til å være reell arbeidssøker](./avklaringer.approved.md#sjekk-om-søker-oppfyller-unntak-til-å-være-reell-arbeidssøker)
- IkkeRegistrertSomArbeidsøker - [Søker er ikke registrert som arbeidssøker](./avklaringer.approved.md#søker-er-ikke-registrert-som-arbeidssøker)
#### Avhenger på data fra
- [§ 3-1. Søknadstidspunkt](#-3-1-søknadstidspunkt)
#### Opplysninger
|UUID|Beskrivelse|Logisk datatype|Datatype|Behov|
|---|---|---|---|---|
|0194881f-9435-72a8-b1ce-9575cbc2a75f|Ønsket arbeidstid|Desimaltall|double|ØnsketArbeidstid|
|0194881f-9435-72a8-b1ce-9575cbc2a769|Minimum vanlig arbeidstid|Desimaltall|double||
|0194881f-9435-72a8-b1ce-9575cbc2a76d|Villig til å jobbe minimum arbeidstid|Boolsk|boolean||
|0194881f-9441-7d1b-a06a-6727543a141e|Kan jobbe heltid og deltid|Boolsk|boolean|KanJobbeDeltid|
|0194881f-9441-7d1b-a06a-6727543a141f|Det er godkjent at bruker kun søker deltidsarbeid|Boolsk|boolean||
|0194881f-9442-707b-a6ee-e96c06877bd8|Oppfyller kravet til heltid- og deltidsarbeid|Boolsk|boolean||
|0194881f-9442-707b-a6ee-e96c06877bd9|Kan jobbe i hele Norge|Boolsk|boolean|KanJobbeHvorSomHelst|
|0194881f-9442-707b-a6ee-e96c06877bda|Det er godkjent at bruker kun søker arbeid lokalt|Boolsk|boolean||
|0194881f-9442-707b-a6ee-e96c06877bdb|Oppfyller kravet til mobilitet|Boolsk|boolean||
|0194881f-9442-707b-a6ee-e96c06877bdc|Kan ta alle typer arbeid|Boolsk|boolean|HelseTilAlleTyperJobb|
|0194881f-9442-707b-a6ee-e96c06877bdd|Oppfyller kravet til å være arbeidsfør|Boolsk|boolean||
|0194881f-9442-707b-a6ee-e96c06877bde|Villig til å bytte yrke|Boolsk|boolean|VilligTilÅBytteYrke|
|0194881f-9442-707b-a6ee-e96c06877bdf|Oppfyller kravet til å ta ethvert arbeid|Boolsk|boolean||
|0194881f-9442-707b-a6ee-e96c06877be2|Reell arbeidssøker|Boolsk|boolean||
|0194929e-2036-7ac1-ada3-5cbe05a83f08|Har helsemessige begrensninger og kan ikke ta alle typer arbeid|Boolsk|boolean||
### § 4-5. Reelle arbeidssøkere - registrert som arbeidssøker
*Type:* Vilkår
#### Avhenger på data fra
- [§ 3-1. Søknadstidspunkt](#-3-1-søknadstidspunkt)
#### Opplysninger
|UUID|Beskrivelse|Logisk datatype|Datatype|Behov|
|---|---|---|---|---|
|0194881f-9442-707b-a6ee-e96c06877be0|Registrert som arbeidssøker|Boolsk|boolean|RegistrertSomArbeidssøker|
|0194881f-9442-707b-a6ee-e96c06877be1|Oppfyller kravet til å være registrert som arbeidssøker|Boolsk|boolean||
### § 4-6. Dagpenger under utdanning, opplæring, etablering av egen virksomhet m.v
*Type:* Vilkår
#### Avhenger på data fra
- [§ 3-1. Søknadstidspunkt](#-3-1-søknadstidspunkt)
#### Opplysninger
|UUID|Beskrivelse|Logisk datatype|Datatype|Behov|
|---|---|---|---|---|
|0194881f-9445-734c-a7ee-045edf29b522|Brukeren er under utdanning eller opplæring|Boolsk|boolean|TarUtdanningEllerOpplæring|
|0194881f-9445-734c-a7ee-045edf29b523|Godkjent unntak for utdanning eller opplæring?|Boolsk|boolean||
|0194881f-9445-734c-a7ee-045edf29b524|Har svart ja på spørsmål om utdanning eller opplæring|Boolsk|boolean||
|0194881f-9445-734c-a7ee-045edf29b525|Har svart nei på spørsmål om utdanning eller opplæring|Boolsk|boolean||
|0194881f-9445-734c-a7ee-045edf29b526|Oppfyller kravet på unntak for utdanning eller opplæring|Boolsk|boolean||
|0194881f-9445-734c-a7ee-045edf29b527|Deltar i arbeidsmarkedstiltak|Boolsk|boolean||
|0194881f-9445-734c-a7ee-045edf29b528|Deltar i opplæring for innvandrere|Boolsk|boolean||
|0194881f-9445-734c-a7ee-045edf29b529|Deltar i grunnskoleopplæring, videregående opplæring og opplæring i grunnleggende ferdigheter|Boolsk|boolean||
|0194881f-9445-734c-a7ee-045edf29b52a|Deltar i høyere yrkesfaglig utdanning|Boolsk|boolean||
|0194881f-9445-734c-a7ee-045edf29b52b|Deltar i høyere utdanning|Boolsk|boolean||
|0194881f-9445-734c-a7ee-045edf29b52c|Deltar på kurs mv|Boolsk|boolean||
|0194881f-9445-734c-a7ee-045edf29b52d|Oppfyller krav til utdanning eller opplæring|Boolsk|boolean||
### § 4-7. Dagpenger til permitterte
*Type:* Vilkår
#### Avklaringer
- HarOppgittPermittering - [Sjekk om bruker skal ha dagpenger som permittert](./avklaringer.approved.md#sjekk-om-bruker-skal-ha-dagpenger-som-permittert)
#### Avhenger på data fra
- [§ 0-0. Rettighetstype](#-0-0-rettighetstype)
#### Opplysninger
|UUID|Beskrivelse|Logisk datatype|Datatype|Behov|
|---|---|---|---|---|
|0194d105-bd54-7b2b-9dc6-6e6664951894|Årsaken til permitteringen er godkjent|Boolsk|boolean||
|0194d111-db2f-7395-bcfb-959f245fd2a6|Oppfyller kravet til permittering|Boolsk|boolean||
|0194d119-90b7-7416-a8b0-9e9cf3587d48|Permitteringen er midlertidig driftsinnskrenkning eller driftsstans|Boolsk|boolean||
### § 4-7. Dagpenger til permitterte
*Type:* Fastsettelse
#### Avhenger på data fra
- [§ 3-1. Søknadstidspunkt](#-3-1-søknadstidspunkt)
#### Opplysninger
|UUID|Beskrivelse|Logisk datatype|Datatype|Behov|
|---|---|---|---|---|
|0195042d-918e-7fae-8fb7-7f38eed42710|Periode som gis ved permittering|Heltall|int||
### § 6-7. Permittering i fiskeforedlingsindustrien, sjømatindustrien og fiskeoljeindustrien
*Type:* Vilkår
#### Avklaringer
- HarOppgittPermitteringFiskeindustri - [Sjekk om bruker skal ha dagpenger som permittert fra fiskeindustrien](./avklaringer.approved.md#sjekk-om-bruker-skal-ha-dagpenger-som-permittert-fra-fiskeindustrien)
#### Avhenger på data fra
- [§ 0-0. Rettighetstype](#-0-0-rettighetstype)
- [§ 3-1. Søknadstidspunkt](#-3-1-søknadstidspunkt)
#### Opplysninger
|UUID|Beskrivelse|Logisk datatype|Datatype|Behov|
|---|---|---|---|---|
|019522b0-c722-76d4-8d7f-78f556c51f72|Oppfyller kravet til permittering i fiskeindustrien|Boolsk|boolean||
|019522b8-0f1b-7536-8691-fd824bca86de|Årsaken til permitteringen fra fiskeindustrien er godkjent|Boolsk|boolean||
|019522b8-494f-7012-898c-d202e3b90061|Permitteringen fra fiskeindustrien er midlertidig driftsinnskrenkning eller driftsstans|Boolsk|boolean||
|019522d2-9bb1-7960-b1e2-a959566e2428|Krav til prosentvis tap av arbeidstid ved permittering fra fiskeindustrien|Desimaltall|double||
### § 6-7. Permittering i fiskeforedlingsindustrien, sjømatindustrien og fiskeoljeindustrien
*Type:* Fastsettelse
#### Avhenger på data fra
- [§ 3-1. Søknadstidspunkt](#-3-1-søknadstidspunkt)
#### Opplysninger
|UUID|Beskrivelse|Logisk datatype|Datatype|Behov|
|---|---|---|---|---|
|0195235a-599b-7b27-97a8-bc6142066a87|Periode som gis ved permittering fra fiskeindustrien|Heltall|int||
### § 4-8. Meldeplikt og møteplikt
*Type:* Vilkår
#### Opplysninger
|UUID|Beskrivelse|Logisk datatype|Datatype|Behov|
|---|---|---|---|---|
### § 4-9. Egenandel
*Type:* Fastsettelse
#### Avhenger på data fra
- [§ 0-0. Rettighetstype](#-0-0-rettighetstype)
- [§ 3-1. Søknadstidspunkt](#-3-1-søknadstidspunkt)
- [§ 4-12. Dagpengenes størrelse](#-4-12-dagpengenes-størrelse)
#### Opplysninger
|UUID|Beskrivelse|Logisk datatype|Datatype|Behov|
|---|---|---|---|---|
|0194881f-943f-78d9-b874-00a4944c54ef|Egenandel|Penger|Beløp||
|0194881f-943f-78d9-b874-00a4944c54f0|Antall dagsats for egenandel|Desimaltall|double||
|019523aa-7941-7dd2-8c43-0644d7b43f57|Tre ganger dagsats|Penger|Beløp||
|019523aa-980d-7805-b6ed-d701e7827998|Ingen egenandel|Penger|Beløp||
### § 4-11. Dagpengegrunnlag
*Type:* Fastsettelse
#### Avklaringer
- NyttGrunnbeløpForGrunnlag - [Grunnbeløpet for dagpengegrunnlag kan være utdatert](./avklaringer.approved.md#grunnbeløpet-for-dagpengegrunnlag-kan-være-utdatert)
#### Avhenger på data fra
- [§ 3-1. Søknadstidspunkt](#-3-1-søknadstidspunkt)
- [§ 4-4. Krav til minsteinntekt](#-4-4-krav-til-minsteinntekt)
- [§ 4-19. Dagpenger etter avtjent verneplikt](#-4-19-dagpenger-etter-avtjent-verneplikt)
#### Opplysninger
|UUID|Beskrivelse|Logisk datatype|Datatype|Behov|
|---|---|---|---|---|
|0194881f-940f-7af9-9387-052e028b29ec|Oppjustert inntekt|InntektDataType|Inntekt||
|0194881f-940f-7af9-9387-052e028b29ed|Tellende inntekt|InntektDataType|Inntekt||
|0194881f-940f-7af9-9387-052e028b29ee|Grunnbeløp for grunnlag|Penger|Beløp||
|0194881f-9410-7481-b263-4606fdd10ca7|Faktor for maksimalt mulig grunnlag|Desimaltall|double||
|0194881f-9410-7481-b263-4606fdd10ca8|6 ganger grunnbeløp|Penger|Beløp||
|0194881f-9410-7481-b263-4606fdd10ca9|Antall år i 36 måneder|Desimaltall|double||
|0194881f-9410-7481-b263-4606fdd10caa|Inntekt etter avkortning og oppjustering siste 12 måneder|Penger|Beløp||
|0194881f-9410-7481-b263-4606fdd10cab|Inntekt siste 36 måneder|Penger|Beløp||
|0194881f-9410-7481-b263-4606fdd10cac|Gjennomsnittlig inntekt etter avkortning og oppjustering siste 36 måneder|Penger|Beløp||
|0194881f-9410-7481-b263-4606fdd10cad|Utbetalt inntekt periode 1|Penger|Beløp||
|0194881f-9410-7481-b263-4606fdd10cae|Utbetalt inntekt periode 2|Penger|Beløp||
|0194881f-9410-7481-b263-4606fdd10caf|Utbetalt inntekt periode 3|Penger|Beløp||
|0194881f-9410-7481-b263-4606fdd10cb0|Inntektperiode 1|Penger|Beløp||
|0194881f-9410-7481-b263-4606fdd10cb1|Inntektperiode 2|Penger|Beløp||
|0194881f-9410-7481-b263-4606fdd10cb2|Inntektperiode 3|Penger|Beløp||
|0194881f-9410-7481-b263-4606fdd10cb3|Avkortet inntektperiode 1|Penger|Beløp||
|0194881f-9410-7481-b263-4606fdd10cb4|Avkortet inntektperiode 2|Penger|Beløp||
|0194881f-9410-7481-b263-4606fdd10cb5|Avkortet inntektperiode 3|Penger|Beløp||
|0194881f-9410-7481-b263-4606fdd10cb6|Har avkortet grunnlaget i periode 1|Boolsk|boolean||
|0194881f-9410-7481-b263-4606fdd10cb7|Har avkortet grunnlaget i periode 2|Boolsk|boolean||
|0194881f-9410-7481-b263-4606fdd10cb8|Har avkortet grunnlaget i periode 3|Boolsk|boolean||
|0194881f-9410-7481-b263-4606fdd10cb9|Har avkortet grunnlag|Boolsk|boolean||
|0194881f-9410-7481-b263-4606fdd10cba|Brukt beregningsregel|Tekst|String||
|0194881f-9410-7481-b263-4606fdd10cbb|Uavrundet grunnlag|Penger|Beløp||
|0194881f-9410-7481-b263-4606fdd10cbc|Grunnlag ved ordinære dagpenger|Penger|Beløp||
|0194881f-9410-7481-b263-4606fdd10cbd|Dagpengegrunnlag|Penger|Beløp||
|0194881f-9410-7481-b263-4606fdd10cbe|Uavkortet grunnlag siste 12 mnd|Penger|Beløp||
|0194881f-9410-7481-b263-4606fdd10cbf|Uavkortet grunnlag siste 36 mnd|Penger|Beløp||
### § 4-12. Dagpengenes størrelse
*Type:* Fastsettelse
#### Avklaringer
- BarnMåGodkjennes - [Sjekk hvilke barn som skal gi barnetillegg](./avklaringer.approved.md#sjekk-hvilke-barn-som-skal-gi-barnetillegg)
#### Avhenger på data fra
- [§ 3-1. Søknadstidspunkt](#-3-1-søknadstidspunkt)
- [§ 4-11. Dagpengegrunnlag](#-4-11-dagpengegrunnlag)
- [§ 4-26. Samordning med ytelser utenfor folketrygden](#-4-26-samordning-med-ytelser-utenfor-folketrygden)
#### Opplysninger
|UUID|Beskrivelse|Logisk datatype|Datatype|Behov|
|---|---|---|---|---|
|0194881f-9428-74d5-b160-f63a4c61a23b|Barn|BarnDatatype|BarnListe|Barnetillegg|
|0194881f-9428-74d5-b160-f63a4c61a23c|Antall barn som gir rett til barnetillegg|Heltall|int||
|0194881f-9428-74d5-b160-f63a4c61a23d|Barnetilleggets størrelse i kroner per dag for hvert barn|Penger|Beløp||
|0194881f-9428-74d5-b160-f63a4c61a23e|Faktor for utregning av dagsats etter dagpengegrunnlaget|Desimaltall|double||
|0194881f-9428-74d5-b160-f63a4c61a23f|Dagsats uten barnetillegg før samordning|Penger|Beløp||
|0194881f-9428-74d5-b160-f63a4c61a240|Avrundet ukessats med barnetillegg før samordning|Penger|Beløp||
|0194881f-9428-74d5-b160-f63a4c61a241|Dagsats uten barnetillegg før samordning|Penger|Beløp||
|0194881f-9428-74d5-b160-f63a4c61a242|Andel av dagsats med barnetillegg som overstiger maks andel av dagpengegrunnlaget|Penger|Beløp||
|0194881f-9428-74d5-b160-f63a4c61a243|Andel av dagsats uten barnetillegg avkortet til maks andel av dagpengegrunnlaget|Penger|Beløp||
|0194881f-9428-74d5-b160-f63a4c61a244|Sum av barnetillegg|Penger|Beløp||
|0194881f-9428-74d5-b160-f63a4c61a245|Dagsats med barnetillegg før samordning|Penger|Beløp||
|0194881f-9428-74d5-b160-f63a4c61a246|90% av grunnlag for dagpenger|Desimaltall|double||
|0194881f-9428-74d5-b160-f63a4c61a247|Antall arbeidsdager per år|Heltall|int||
|0194881f-9428-74d5-b160-f63a4c61a248|Maksimalt mulig grunnlag avgrenset til 90% av dagpengegrunnlaget|Penger|Beløp||
|0194881f-9428-74d5-b160-f63a4c61a249|Antall arbeidsdager per uke|Heltall|int||
|0194881f-9428-74d5-b160-f63a4c61a24a|Maksimal mulig dagsats avgrenset til 90% av dagpengegrunnlaget|Penger|Beløp||
|0194881f-9428-74d5-b160-f63a4c61a24b|Avrundet maksimal mulig dagsats avgrenset til 90% av dagpengegrunnlaget|Penger|Beløp||
|0194881f-9428-74d5-b160-f63a4c61a24c|Har barnetillegg|Boolsk|boolean||
|0194881f-9428-74d5-b160-f63a4c61a24d|Samordnet dagsats med barnetillegg|Penger|Beløp||
|0194881f-9428-74d5-b160-f63a4c61a24e|Ukessats med barnetillegg etter samordning|Penger|Beløp||
|0194881f-9428-74d5-b160-f63a4c61a24f|Dagsats med barnetillegg etter samordning og 90 % regel|Penger|Beløp||
|0194881f-9428-74d5-b160-f63a4c61a250|Har samordnet|Boolsk|boolean||
### § 4-15. Antall stønadsuker (stønadsperiode)
*Type:* Fastsettelse
#### Avhenger på data fra
- [§ 3-1. Søknadstidspunkt](#-3-1-søknadstidspunkt)
- [§ 4-4. Krav til minsteinntekt](#-4-4-krav-til-minsteinntekt)
- [§ 4-11. Dagpengegrunnlag](#-4-11-dagpengegrunnlag)
#### Opplysninger
|UUID|Beskrivelse|Logisk datatype|Datatype|Behov|
|---|---|---|---|---|
|0194881f-943d-77a7-969c-147999f15449|Antall dager som skal regnes med i hver uke|Heltall|int||
|0194881f-943d-77a7-969c-147999f1544a|Kort dagpengeperiode|Heltall|int||
|0194881f-943d-77a7-969c-147999f1544b|Lang dagpengeperiode|Heltall|int||
|0194881f-943d-77a7-969c-147999f1544c|Terskel for 12 måneder|Penger|Beløp||
|0194881f-943d-77a7-969c-147999f1544d|Terskel for 36 måneder|Penger|Beløp||
|0194881f-943d-77a7-969c-147999f1544f|Terskelfaktor for 12 måneder|Desimaltall|double||
|0194881f-943d-77a7-969c-147999f15450|Terskelfaktor for 36 måneder|Desimaltall|double||
|0194881f-943d-77a7-969c-147999f15451|Snittinntekt siste 36 måneder|Penger|Beløp||
|0194881f-943d-77a7-969c-147999f15452|Stønadsuker ved siste 12 måneder|Heltall|int||
|0194881f-943d-77a7-969c-147999f15453|Stønadsuker ved siste 36 måneder|Heltall|int||
|0194881f-943d-77a7-969c-147999f15454|Over terskel for 12 måneder|Boolsk|boolean||
|0194881f-943d-77a7-969c-147999f15455|Over terskel for 36 måneder|Boolsk|boolean||
|0194881f-943d-77a7-969c-147999f15456|Antall stønadsuker|Heltall|int||
|0194881f-943d-77a7-969c-147999f15457|Antall stønadsdager|Heltall|int||
|0194881f-943d-77a7-969c-147999f15458|Stønadsuker når kravet til minste arbeidsinntekt ikke er oppfylt|Heltall|int||
|0194881f-943d-77a7-969c-147999f15459|Antall stønadsuker (stønadsperiode)|Heltall|int||
### § 4-19. Dagpenger etter avtjent verneplikt
*Type:* Vilkår
#### Avklaringer
- Verneplikt - [Sjekk om søker oppfyller vilkåret til dagpenger ved avtjent verneplikt](./avklaringer.approved.md#sjekk-om-søker-oppfyller-vilkåret-til-dagpenger-ved-avtjent-verneplikt)
#### Avhenger på data fra
- [§ 3-1. Søknadstidspunkt](#-3-1-søknadstidspunkt)
#### Opplysninger
|UUID|Beskrivelse|Logisk datatype|Datatype|Behov|
|---|---|---|---|---|
|01948d3c-4bea-7802-9d18-5342a5e2be99|Avtjent verneplikt|Boolsk|boolean|Verneplikt|
|01948d43-e218-76f1-b29b-7e604241d98a|Oppfyller kravet til verneplikt|Boolsk|boolean||
### § 4-19. Dagpenger etter avtjent verneplikt
*Type:* Fastsettelse
#### Avhenger på data fra
- [§ 0-0. Rettighetstype](#-0-0-rettighetstype)
- [§ 3-1. Søknadstidspunkt](#-3-1-søknadstidspunkt)
- [§ 4-11. Dagpengegrunnlag](#-4-11-dagpengegrunnlag)
#### Opplysninger
|UUID|Beskrivelse|Logisk datatype|Datatype|Behov|
|---|---|---|---|---|
|0194881f-9421-766c-9dc6-41fe6c9a1dff|Antall G som gis som grunnlag ved verneplikt|Desimaltall|double||
|0194881f-9421-766c-9dc6-41fe6c9a1e00|Grunnlag for gis ved verneplikt|Penger|Beløp||
|0194881f-9421-766c-9dc6-41fe6c9a1e01|Periode som gis ved verneplikt|Heltall|int||
|0194881f-9421-766c-9dc6-41fe6c9a1e02|Fastsatt vanlig arbeidstid for verneplikt|Desimaltall|double||
|0194881f-9421-766c-9dc6-41fe6c9a1e03|Grunnlag for verneplikt hvis kravet er oppfylt|Penger|Beløp||
|0194881f-9421-766c-9dc6-41fe6c9a1e04|Grunnlag for verneplikt hvis kravet ikke er oppfylt|Penger|Beløp||
|0194881f-9421-766c-9dc6-41fe6c9a1e05|Grunnlaget for verneplikt er høyere enn dagpengegrunnlaget|Boolsk|boolean||
### § 4-22. Bortfall ved streik og lock-out
*Type:* Vilkår
#### Opplysninger
|UUID|Beskrivelse|Logisk datatype|Datatype|Behov|
|---|---|---|---|---|
|0194881f-91df-746a-a8ac-4a6b2b30685d|Brukeren deltar i streik eller er omfattet av lock-out|Boolsk|boolean||
|0194881f-91df-746a-a8ac-4a6b2b30685e|Brukeren er ledig ved samme bedrift eller arbeidsplass, og blir påvirket av utfallet|Boolsk|boolean||
|0194881f-91df-746a-a8ac-4a6b2b30685f|Brukeren er ikke påvirket av streik eller lock-out|Boolsk|boolean||
### § 4-23. Bortfall på grunn av alder
*Type:* Vilkår
#### Avhenger på data fra
- [§ 3-1. Søknadstidspunkt](#-3-1-søknadstidspunkt)
#### Opplysninger
|UUID|Beskrivelse|Logisk datatype|Datatype|Behov|
|---|---|---|---|---|
|0194881f-940b-76ff-acf5-ba7bcb367233|Fødselsdato|Dato|LocalDate|Fødselsdato|
|0194881f-940b-76ff-acf5-ba7bcb367234|Aldersgrense|Heltall|int||
|0194881f-940b-76ff-acf5-ba7bcb367235|Dato søker når maks alder|Dato|LocalDate||
|0194881f-940b-76ff-acf5-ba7bcb367236|Siste mulige dag bruker kan oppfylle alderskrav|Dato|LocalDate||
|0194881f-940b-76ff-acf5-ba7bcb367237|Oppfyller kravet til alder|Boolsk|boolean||
### § 4-24. Medlem som har fulle ytelser etter folketrygdloven eller avtalefestet pensjon
*Type:* Vilkår
#### Avklaringer
- FulleYtelser - [Sjekk om søker har andre fulle ytelser](./avklaringer.approved.md#sjekk-om-søker-har-andre-fulle-ytelser)
#### Opplysninger
|UUID|Beskrivelse|Logisk datatype|Datatype|Behov|
|---|---|---|---|---|
|0194881f-943f-78d9-b874-00a4944c54f1|Oppfyller vilkåret om å ikke motta andre fulle ytelser|Boolsk|boolean||
### § 4-25. Samordning med reduserte ytelser fra folketrygden, eller redusert avtalefestet pensjon
*Type:* Vilkår
#### Avklaringer
- Samordning - [Sjekk om det er andre ytelser fra folketrygden som skal samordnes](./avklaringer.approved.md#sjekk-om-det-er-andre-ytelser-fra-folketrygden-som-skal-samordnes)
#### Avhenger på data fra
- [§ 3-1. Søknadstidspunkt](#-3-1-søknadstidspunkt)
- [§ 4-3. Tap av arbeidsinntekt og arbeidstid](#-4-3-tap-av-arbeidsinntekt-og-arbeidstid)
- [§ 4-12. Dagpengenes størrelse](#-4-12-dagpengenes-størrelse)
#### Opplysninger
|UUID|Beskrivelse|Logisk datatype|Datatype|Behov|
|---|---|---|---|---|
|0194881f-9433-70e9-a85b-c246150c45cd|Sykepenger etter lovens kapittel 8|Boolsk|boolean|Sykepenger|
|0194881f-9433-70e9-a85b-c246150c45ce|Pleiepenger etter lovens kapittel 9|Boolsk|boolean|Pleiepenger|
|0194881f-9433-70e9-a85b-c246150c45cf|Omsorgspenger etter lovens kapittel 9|Boolsk|boolean|Omsorgspenger|
|0194881f-9433-70e9-a85b-c246150c45d0|Opplæringspenger etter lovens kapittel 9|Boolsk|boolean|Opplæringspenger|
|0194881f-9433-70e9-a85b-c246150c45d1|Uføretrygd etter lovens kapittel 12|Boolsk|boolean||
|0194881f-9433-70e9-a85b-c246150c45d2|Foreldrepenger etter lovens kapittel 14|Boolsk|boolean|Foreldrepenger|
|0194881f-9433-70e9-a85b-c246150c45d3|Svangerskapspenger etter lovens kapittel 14|Boolsk|boolean|Svangerskapspenger|
|0194881f-9433-70e9-a85b-c246150c45d4|Sykepenger dagsats|Penger|Beløp||
|0194881f-9433-70e9-a85b-c246150c45d5|Pleiepenger dagsats|Penger|Beløp||
|0194881f-9433-70e9-a85b-c246150c45d6|Omsorgspenger dagsats|Penger|Beløp||
|0194881f-9433-70e9-a85b-c246150c45d7|Opplæringspenger dagsats|Penger|Beløp||
|0194881f-9433-70e9-a85b-c246150c45d8|Uføre dagsats|Penger|Beløp||
|0194881f-9433-70e9-a85b-c246150c45d9|Foreldrepenger dagsats|Penger|Beløp||
|0194881f-9433-70e9-a85b-c246150c45da|Svangerskapspenger dagsats|Penger|Beløp||
|0194881f-9434-79e8-a64d-1a23cc5d86e9|Sum andre ytelser|Penger|Beløp||
|0194881f-9434-79e8-a64d-1a23cc5d86ea|Medlem har reduserte ytelser fra folketrygden (Samordning)|Boolsk|boolean||
|0194881f-9434-79e8-a64d-1a23cc5d86eb|Samordnet dagsats uten barnetillegg|Penger|Beløp||
|0194881f-9434-79e8-a64d-1a23cc5d86ec|Samordnet dagsats er større enn 0|Boolsk|boolean||
|0194881f-9434-79e8-a64d-1a23cc5d86ed|Antall timer arbeidstiden skal samordnes mot|Desimaltall|double||
|0194881f-9434-79e8-a64d-1a23cc5d86ee|Samordnet fastsatt arbeidstid|Desimaltall|double||
|0194881f-9434-79e8-a64d-1a23cc5d86ef|Utfall etter samordning|Boolsk|boolean||
|0196afaf-afbd-7079-b2cf-3669ad9d86aa|Uføretrygden er gitt med virkningstidspunkt i inneværende år eller innenfor de to siste kalenderår|Boolsk|boolean||
|0196afbf-e32d-775a-ad10-f476e26dcb6f|Uførebeløp som skal samordnes|Penger|Beløp||
|0196afc0-6807-7fa3-83e4-cf7f621f3a7e|Sum hvis Uføre ikke skal samordnes|Penger|Beløp||
### § 4-26. Samordning med ytelser utenfor folketrygden
*Type:* Fastsettelse
#### Avklaringer
- YtelserUtenforFolketrygden - [Sjekk om det er ytelser utenfor folketrygden som skal samordnes](./avklaringer.approved.md#sjekk-om-det-er-ytelser-utenfor-folketrygden-som-skal-samordnes)
#### Avhenger på data fra
- [§ 3-1. Søknadstidspunkt](#-3-1-søknadstidspunkt)
- [§ 4-11. Dagpengegrunnlag](#-4-11-dagpengegrunnlag)
- [§ 4-12. Dagpengenes størrelse](#-4-12-dagpengenes-størrelse)
- [§ 4-25. Samordning med reduserte ytelser fra folketrygden, eller redusert avtalefestet pensjon](#-4-25-samordning-med-reduserte-ytelser-fra-folketrygden,-eller-redusert-avtalefestet-pensjon)
#### Opplysninger
|UUID|Beskrivelse|Logisk datatype|Datatype|Behov|
|---|---|---|---|---|
|0194881f-942e-7cb0-aa59-05ea449d88e0|Oppgitt andre ytelser utenfor NAV i søknaden|Boolsk|boolean|OppgittAndreYtelserUtenforNav|
|0194881f-942e-7cb0-aa59-05ea449d88e1|Mottar pensjon fra en offentlig tjenestepensjonsordning|Boolsk|boolean||
|0194881f-942e-7cb0-aa59-05ea449d88e2|Mottar redusert uførepensjon fra offentlig pensjonsordning|Boolsk|boolean||
|0194881f-942e-7cb0-aa59-05ea449d88e3|Mottar vartpenger|Boolsk|boolean||
|0194881f-942e-7cb0-aa59-05ea449d88e4|Mottar ventelønn|Boolsk|boolean||
|0194881f-942e-7cb0-aa59-05ea449d88e5|Mottar etterlønn|Boolsk|boolean||
|0194881f-942e-7cb0-aa59-05ea449d88e6|Mottar garantilott fra Garantikassen for fiskere.|Boolsk|boolean||
|0194881f-942e-7cb0-aa59-05ea449d88e7|Pensjon fra en offentlig tjenestepensjonsordning beløp|Penger|Beløp||
|0194881f-942e-7cb0-aa59-05ea449d88e8|Uførepensjon fra offentlig pensjonsordning beløp|Penger|Beløp||
|0194881f-942f-7bde-ab16-68ffd19e9a26|Vartpenger beløp|Penger|Beløp||
|0194881f-942f-7bde-ab16-68ffd19e9a27|Ventelønn beløp|Penger|Beløp||
|0194881f-942f-7bde-ab16-68ffd19e9a28|Etterlønn beløp|Penger|Beløp||
|0194881f-942f-7bde-ab16-68ffd19e9a29|Garantilott fra Garantikassen for fiskere beløp|Penger|Beløp||
|0194881f-942f-7bde-ab16-68ffd19e9a2b|Hvor mange prosent av G skal brukes som terskel ved samordning|Desimaltall|double||
|0194881f-942f-7bde-ab16-68ffd19e9a2c|Beløp tilsvarende nedre terskel av G|Penger|Beløp||
|0194881f-942f-7bde-ab16-68ffd19e9a2d|Skal samordnes med ytelser utenfor folketrygden|Boolsk|boolean||
|0194881f-942f-7bde-ab16-68ffd19e9a2e|Sum av ytelser utenfor folketrygden|Penger|Beløp||
|0194881f-942f-7bde-ab16-68ffd19e9a2f|Samordnet ukessats uten barnetillegg|Penger|Beløp||
|0194881f-942f-7bde-ab16-68ffd19e9a30|Minste mulige ukessats som som kan brukes|Penger|Beløp||
|0194881f-942f-7bde-ab16-68ffd19e9a31|Ukessats trukket ned for ytelser utenfor folketrygden|Penger|Beløp||
|0194881f-942f-7bde-ab16-68ffd19e9a32|Samordnet ukessats med ytelser utenfor folketrygden|Penger|Beløp||
|0194881f-942f-7bde-ab16-68ffd19e9a33|Dagsats uten barnetillegg samordnet|Penger|Beløp||
### § 4-28. Utestengning
*Type:* Vilkår
#### Opplysninger
|UUID|Beskrivelse|Logisk datatype|Datatype|Behov|
|---|---|---|---|---|
|0194881f-9447-7e36-a569-3e9f42bff9f6|Bruker er utestengt fra dagpenger|Boolsk|boolean||
|0194881f-9447-7e36-a569-3e9f42bff9f7|Oppfyller krav til ikke utestengt|Boolsk|boolean||
