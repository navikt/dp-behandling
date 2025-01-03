# Regeltre - Dagpenger (inngangsvilkår)

## Regeltre

```mermaid
graph RL
  A["Aldersgrense"] -->|"Oppslag"| B["Prøvingsdato"]
  C["Dato søker når maks alder"] -->|"LeggTilÅr"| D["Fødselsdato"]
  C["Dato søker når maks alder"] -->|"LeggTilÅr"| A["Aldersgrense"]
  E["Siste mulige dag bruker kan oppfylle alderskrav"] -->|"SisteDagIMåned"| C["Dato søker når maks alder"]
  F["Oppfyller kravet til alder"] -->|"FørEllerLik"| B["Prøvingsdato"]
  F["Oppfyller kravet til alder"] -->|"FørEllerLik"| E["Siste mulige dag bruker kan oppfylle alderskrav"]
  G["Antall år i 36 måneder"] -->|"Oppslag"| B["Prøvingsdato"]
  H["Faktor for maksimalt mulig grunnlag"] -->|"Oppslag"| B["Prøvingsdato"]
  I["6 ganger grunnbeløp"] -->|"Multiplikasjon"| J["Grunnbeløp for grunnlag"]
  I["6 ganger grunnbeløp"] -->|"Multiplikasjon"| H["Faktor for maksimalt mulig grunnlag"]
  J["Grunnbeløp for grunnlag"] -->|"Oppslag"| B["Prøvingsdato"]
  K["Tellende inntekt"] -->|"FiltrerRelevanteInntekter"| L["Inntektsopplysninger"]
  M["Oppjustert inntekt"] -->|"Oppjuster"| J["Grunnbeløp for grunnlag"]
  M["Oppjustert inntekt"] -->|"Oppjuster"| K["Tellende inntekt"]
  N["Utbetalt arbeidsinntekt periode 1"] -->|"SummerPeriode"| K["Tellende inntekt"]
  O["Utbetalt arbeidsinntekt periode 2"] -->|"SummerPeriode"| K["Tellende inntekt"]
  P["Utbetalt arbeidsinntekt periode 3"] -->|"SummerPeriode"| K["Tellende inntekt"]
  Q["Inntektperiode 1"] -->|"SummerPeriode"| M["Oppjustert inntekt"]
  R["Inntektperiode 2"] -->|"SummerPeriode"| M["Oppjustert inntekt"]
  S["Inntektperiode 3"] -->|"SummerPeriode"| M["Oppjustert inntekt"]
  T["Uavkortet grunnlag siste 12 mnd"] -->|"SumAv"| Q["Inntektperiode 1"]
  U["Uavkortet grunnlag siste 36 mnd"] -->|"SumAv"| Q["Inntektperiode 1"]
  U["Uavkortet grunnlag siste 36 mnd"] -->|"SumAv"| R["Inntektperiode 2"]
  U["Uavkortet grunnlag siste 36 mnd"] -->|"SumAv"| S["Inntektperiode 3"]
  V["Avkortet inntektperiode 1"] -->|"MinstAv"| Q["Inntektperiode 1"]
  V["Avkortet inntektperiode 1"] -->|"MinstAv"| I["6 ganger grunnbeløp"]
  W["Avkortet inntektperiode 2"] -->|"MinstAv"| R["Inntektperiode 2"]
  W["Avkortet inntektperiode 2"] -->|"MinstAv"| I["6 ganger grunnbeløp"]
  X["Avkortet inntektperiode 3"] -->|"MinstAv"| S["Inntektperiode 3"]
  X["Avkortet inntektperiode 3"] -->|"MinstAv"| I["6 ganger grunnbeløp"]
  Y["Grunnlag siste 12 mnd."] -->|"MinstAv"| Q["Inntektperiode 1"]
  Y["Grunnlag siste 12 mnd."] -->|"MinstAv"| I["6 ganger grunnbeløp"]
  Z["Inntekt siste 36 måneder"] -->|"SumAv"| V["Avkortet inntektperiode 1"]
  Z["Inntekt siste 36 måneder"] -->|"SumAv"| W["Avkortet inntektperiode 2"]
  Z["Inntekt siste 36 måneder"] -->|"SumAv"| X["Avkortet inntektperiode 3"]
  AA["Gjennomsnittlig arbeidsinntekt siste 36 måneder"] -->|"Divisjon"| Z["Inntekt siste 36 måneder"]
  AA["Gjennomsnittlig arbeidsinntekt siste 36 måneder"] -->|"Divisjon"| G["Antall år i 36 måneder"]
  AB["Uavrundet grunnlag"] -->|"HøyesteAv"| Y["Grunnlag siste 12 mnd."]
  AB["Uavrundet grunnlag"] -->|"HøyesteAv"| AA["Gjennomsnittlig arbeidsinntekt siste 36 måneder"]
  AC["Brukt beregningsregel"] -->|"Brukt"| AB["Uavrundet grunnlag"]
  AD["Grunnlag ved ordinære dagpenger"] -->|"Avrund"| AB["Uavrundet grunnlag"]
  AE["Grunnlag"] -->|"HøyesteAv"| AD["Grunnlag ved ordinære dagpenger"]
  AE["Grunnlag"] -->|"HøyesteAv"| AF["Grunnlag for verneplikt hvis kravet er oppfylt"]
  AG["Har avkortet grunnlaget i periode 1"] -->|"StørreEnn"| Q["Inntektperiode 1"]
  AG["Har avkortet grunnlaget i periode 1"] -->|"StørreEnn"| I["6 ganger grunnbeløp"]
  AH["Har avkortet grunnlaget i periode 2"] -->|"StørreEnn"| R["Inntektperiode 2"]
  AH["Har avkortet grunnlaget i periode 2"] -->|"StørreEnn"| I["6 ganger grunnbeløp"]
  AI["Har avkortet grunnlaget i periode 3"] -->|"StørreEnn"| S["Inntektperiode 3"]
  AI["Har avkortet grunnlaget i periode 3"] -->|"StørreEnn"| I["6 ganger grunnbeløp"]
  AJ["Har avkortet grunnlag"] -->|"EnAv"| AG["Har avkortet grunnlaget i periode 1"]
  AJ["Har avkortet grunnlag"] -->|"EnAv"| AH["Har avkortet grunnlaget i periode 2"]
  AJ["Har avkortet grunnlag"] -->|"EnAv"| AI["Har avkortet grunnlaget i periode 3"]
  AK["Barn"] -->|"Ekstern"| AL["søknadId"]
  AM["Antall barn som gir rett til barnetillegg"] -->|"AntallAv"| AK["Barn"]
  AN["Faktor for utregning av dagsats etter dagpengegrunnlaget"] -->|"Oppslag"| B["Prøvingsdato"]
  AO["Dagsats uten barnetillegg før samordning"] -->|"Multiplikasjon"| AE["Grunnlag"]
  AO["Dagsats uten barnetillegg før samordning"] -->|"Multiplikasjon"| AN["Faktor for utregning av dagsats etter dagpengegrunnlaget"]
  AP["Avrundet dagsats uten barnetillegg før samordning"] -->|"Avrund"| AO["Dagsats uten barnetillegg før samordning"]
  AQ["Barnetilleggets størrelse i kroner per dag for hvert barn"] -->|"Oppslag"| B["Prøvingsdato"]
  AR["Sum av barnetillegg"] -->|"Multiplikasjon"| AQ["Barnetilleggets størrelse i kroner per dag for hvert barn"]
  AR["Sum av barnetillegg"] -->|"Multiplikasjon"| AM["Antall barn som gir rett til barnetillegg"]
  AS["Dagsats med barnetillegg før samordning"] -->|"Addisjon"| AO["Dagsats uten barnetillegg før samordning"]
  AS["Dagsats med barnetillegg før samordning"] -->|"Addisjon"| AR["Sum av barnetillegg"]
  AT["Avrundet ukessats med barnetillegg før samordning"] -->|"Multiplikasjon"| AS["Dagsats med barnetillegg før samordning"]
  AT["Avrundet ukessats med barnetillegg før samordning"] -->|"Multiplikasjon"| AU["Antall arbeidsdager per uke"]
  AV["90% av grunnlag for dagpenger"] -->|"Oppslag"| B["Prøvingsdato"]
  AW["Antall arbeidsdager per år"] -->|"Oppslag"| B["Prøvingsdato"]
  AX["Maksimalt mulig grunnlag avgrenset til 90% av dagpengegrunnlaget"] -->|"Multiplikasjon"| AE["Grunnlag"]
  AX["Maksimalt mulig grunnlag avgrenset til 90% av dagpengegrunnlaget"] -->|"Multiplikasjon"| AV["90% av grunnlag for dagpenger"]
  AY["Maksimal mulig dagsats avgrenset til 90% av dagpengegrunnlaget"] -->|"Divisjon"| AX["Maksimalt mulig grunnlag avgrenset til 90% av dagpengegrunnlaget"]
  AY["Maksimal mulig dagsats avgrenset til 90% av dagpengegrunnlaget"] -->|"Divisjon"| AW["Antall arbeidsdager per år"]
  AZ["Avrundet maksimal mulig dagsats avgrenset til 90% av dagpengegrunnlaget"] -->|"Avrund"| AY["Maksimal mulig dagsats avgrenset til 90% av dagpengegrunnlaget"]
  BA["Andel av dagsats med barnetillegg som overstiger maks andel av dagpengegrunnlaget"] -->|"Substraksjon"| AS["Dagsats med barnetillegg før samordning"]
  BA["Andel av dagsats med barnetillegg som overstiger maks andel av dagpengegrunnlaget"] -->|"Substraksjon"| AZ["Avrundet maksimal mulig dagsats avgrenset til 90% av dagpengegrunnlaget"]
  BB["Andel av dagsats uten barnetillegg avkortet til maks andel av dagpengegrunnlaget"] -->|"Substraksjon"| AP["Avrundet dagsats uten barnetillegg før samordning"]
  BB["Andel av dagsats uten barnetillegg avkortet til maks andel av dagpengegrunnlaget"] -->|"Substraksjon"| BA["Andel av dagsats med barnetillegg som overstiger maks andel av dagpengegrunnlaget"]
  BC["Samordnet dagsats med barnetillegg"] -->|"Addisjon"| BD["Dagsats uten barnetillegg samordnet"]
  BC["Samordnet dagsats med barnetillegg"] -->|"Addisjon"| AR["Sum av barnetillegg"]
  BE["Dagsats med barnetillegg etter samordning og 90% regel"] -->|"MinstAv"| BC["Samordnet dagsats med barnetillegg"]
  BE["Dagsats med barnetillegg etter samordning og 90% regel"] -->|"MinstAv"| AZ["Avrundet maksimal mulig dagsats avgrenset til 90% av dagpengegrunnlaget"]
  BF["Har samordnet"] -->|"ErUlik"| BB["Andel av dagsats uten barnetillegg avkortet til maks andel av dagpengegrunnlaget"]
  BF["Har samordnet"] -->|"ErUlik"| BD["Dagsats uten barnetillegg samordnet"]
  AU["Antall arbeidsdager per uke"] -->|"Oppslag"| B["Prøvingsdato"]
  BG["Ukessats med barnetillegg etter samordning"] -->|"Multiplikasjon"| BE["Dagsats med barnetillegg etter samordning og 90% regel"]
  BG["Ukessats med barnetillegg etter samordning"] -->|"Multiplikasjon"| AU["Antall arbeidsdager per uke"]
  BH["Har barnetillegg"] -->|"StørreEnnEllerLik"| AR["Sum av barnetillegg"]
  BH["Har barnetillegg"] -->|"StørreEnnEllerLik"| AQ["Barnetilleggets størrelse i kroner per dag for hvert barn"]
  BI["Kort dagpengeperiode"] -->|"Oppslag"| B["Prøvingsdato"]
  BJ["Lang dagpengeperiode"] -->|"Oppslag"| B["Prøvingsdato"]
  BK["Terskelfaktor for 12 måneder"] -->|"Oppslag"| B["Prøvingsdato"]
  BL["Terskelfaktor for 36 måneder"] -->|"Oppslag"| B["Prøvingsdato"]
  BM["Divisior"] -->|"Oppslag"| B["Prøvingsdato"]
  BN["Terskel for 12 måneder"] -->|"Multiplikasjon"| BO["Grunnbeløp"]
  BN["Terskel for 12 måneder"] -->|"Multiplikasjon"| BK["Terskelfaktor for 12 måneder"]
  BP["Terskel for 36 måneder"] -->|"Multiplikasjon"| BO["Grunnbeløp"]
  BP["Terskel for 36 måneder"] -->|"Multiplikasjon"| BL["Terskelfaktor for 36 måneder"]
  BQ["Snittinntekt siste 36 måneder"] -->|"Divisjon"| BR["Arbeidsinntekt siste 36 mnd"]
  BQ["Snittinntekt siste 36 måneder"] -->|"Divisjon"| BM["Divisior"]
  BS["Over terskel for 12 måneder"] -->|"StørreEnnEllerLik"| BT["Arbeidsinntekt siste 12 mnd"]
  BS["Over terskel for 12 måneder"] -->|"StørreEnnEllerLik"| BN["Terskel for 12 måneder"]
  BU["Over terskel for 36 måneder"] -->|"StørreEnnEllerLik"| BQ["Snittinntekt siste 36 måneder"]
  BU["Over terskel for 36 måneder"] -->|"StørreEnnEllerLik"| BP["Terskel for 36 måneder"]
  BV["Stønadsuker ved siste 12 måneder"] -->|"HvisSannMedResultat"| BS["Over terskel for 12 måneder"]
  BV["Stønadsuker ved siste 12 måneder"] -->|"HvisSannMedResultat"| BJ["Lang dagpengeperiode"]
  BV["Stønadsuker ved siste 12 måneder"] -->|"HvisSannMedResultat"| BI["Kort dagpengeperiode"]
  BW["Stønadsuker ved siste 36 måneder"] -->|"HvisSannMedResultat"| BU["Over terskel for 36 måneder"]
  BW["Stønadsuker ved siste 36 måneder"] -->|"HvisSannMedResultat"| BJ["Lang dagpengeperiode"]
  BW["Stønadsuker ved siste 36 måneder"] -->|"HvisSannMedResultat"| BI["Kort dagpengeperiode"]
  BX["Antall stønadsuker"] -->|"HøyesteAv"| BV["Stønadsuker ved siste 12 måneder"]
  BX["Antall stønadsuker"] -->|"HøyesteAv"| BW["Stønadsuker ved siste 36 måneder"]
  BY["Stønadsuker når kravet til minste arbeidsinntekt ikke er oppfylt"] -->|"Oppslag"| B["Prøvingsdato"]
  BZ["Antall stønadsuker som gis ved ordinære dagpenger"] -->|"HvisSannMedResultat"| CA["Krav til minsteinntekt"]
  BZ["Antall stønadsuker som gis ved ordinære dagpenger"] -->|"HvisSannMedResultat"| BX["Antall stønadsuker"]
  BZ["Antall stønadsuker som gis ved ordinære dagpenger"] -->|"HvisSannMedResultat"| BY["Stønadsuker når kravet til minste arbeidsinntekt ikke er oppfylt"]
  CB["Antall dager som skal regnes med i hver uke"] -->|"Oppslag"| B["Prøvingsdato"]
  CC["Antall gjenstående stønadsdager"] -->|"Multiplikasjon"| BX["Antall stønadsuker"]
  CC["Antall gjenstående stønadsdager"] -->|"Multiplikasjon"| CB["Antall dager som skal regnes med i hver uke"]
  CD["Antall dagsats for egenandel"] -->|"Oppslag"| B["Prøvingsdato"]
  CE["Egenandel"] -->|"Multiplikasjon"| BE["Dagsats med barnetillegg etter samordning og 90% regel"]
  CE["Egenandel"] -->|"Multiplikasjon"| CD["Antall dagsats for egenandel"]
  CF["Mottar ikke andre fulle ytelser"] -->|"Oppslag"| B["Prøvingsdato"]
  CG["Oppfyller kravet til minsteinntekt eller verneplikt"] -->|"EnAv"| CA["Krav til minsteinntekt"]
  CG["Oppfyller kravet til minsteinntekt eller verneplikt"] -->|"EnAv"| CH["Har utført minst tre måneders militærtjeneste eller obligatorisk sivilforsvarstjeneste"]
  CI["Krav på dagpenger"] -->|"Alle"| F["Oppfyller kravet til alder"]
  CI["Krav på dagpenger"] -->|"Alle"| CF["Mottar ikke andre fulle ytelser"]
  CI["Krav på dagpenger"] -->|"Alle"| CJ["Oppfyller kravet til medlemskap"]
  CI["Krav på dagpenger"] -->|"Alle"| CK["Registrert som arbeidssøker på søknadstidspunktet"]
  CI["Krav på dagpenger"] -->|"Alle"| CG["Oppfyller kravet til minsteinntekt eller verneplikt"]
  CI["Krav på dagpenger"] -->|"Alle"| CL["Oppfyller kravet til opphold i Norge"]
  CI["Krav på dagpenger"] -->|"Alle"| CM["Krav til arbeidssøker"]
  CI["Krav på dagpenger"] -->|"Alle"| CN["Rettighetstype"]
  CI["Krav på dagpenger"] -->|"Alle"| CO["Utfall etter samordning"]
  CI["Krav på dagpenger"] -->|"Alle"| CP["Er medlemmet ikke påvirket av streik eller lock-out?"]
  CI["Krav på dagpenger"] -->|"Alle"| CQ["Krav til tap av arbeidsinntekt og arbeidstid"]
  CI["Krav på dagpenger"] -->|"Alle"| CR["Krav til utdanning eller opplæring"]
  CI["Krav på dagpenger"] -->|"Alle"| CS["Oppfyller krav til ikke utestengt"]
  CT["Er personen medlem av folketrygden"] -->|"Oppslag"| B["Prøvingsdato"]
  CJ["Oppfyller kravet til medlemskap"] -->|"ErSann"| CT["Er personen medlem av folketrygden"]
  CU["Registrert som arbeidssøker"] -->|"Ekstern"| B["Prøvingsdato"]
  CK["Registrert som arbeidssøker på søknadstidspunktet"] -->|"ErSann"| CU["Registrert som arbeidssøker"]
  CV["Maks lengde på opptjeningsperiode"] -->|"Oppslag"| B["Prøvingsdato"]
  CW["Første måned av opptjeningsperiode"] -->|"TrekkFraMåned"| CX["Siste avsluttende kalendermåned"]
  CW["Første måned av opptjeningsperiode"] -->|"TrekkFraMåned"| CV["Maks lengde på opptjeningsperiode"]
  L["Inntektsopplysninger"] -->|"Ekstern"| B["Prøvingsdato"]
  L["Inntektsopplysninger"] -->|"Ekstern"| CX["Siste avsluttende kalendermåned"]
  L["Inntektsopplysninger"] -->|"Ekstern"| CW["Første måned av opptjeningsperiode"]
  CY["Brutto arbeidsinntekt"] -->|"FiltrerRelevanteInntekter"| L["Inntektsopplysninger"]
  BO["Grunnbeløp"] -->|"Oppslag"| B["Prøvingsdato"]
  BT["Arbeidsinntekt siste 12 mnd"] -->|"SummerPeriode"| CY["Brutto arbeidsinntekt"]
  CZ["Antall G for krav til 12 mnd arbeidsinntekt"] -->|"Oppslag"| B["Prøvingsdato"]
  DA["Inntektskrav for siste 12 mnd"] -->|"Multiplikasjon"| BO["Grunnbeløp"]
  DA["Inntektskrav for siste 12 mnd"] -->|"Multiplikasjon"| CZ["Antall G for krav til 12 mnd arbeidsinntekt"]
  DB["Arbeidsinntekt er over kravet for siste 12 mnd"] -->|"StørreEnnEllerLik"| BT["Arbeidsinntekt siste 12 mnd"]
  DB["Arbeidsinntekt er over kravet for siste 12 mnd"] -->|"StørreEnnEllerLik"| DA["Inntektskrav for siste 12 mnd"]
  BR["Arbeidsinntekt siste 36 mnd"] -->|"SummerPeriode"| CY["Brutto arbeidsinntekt"]
  DC["Antall G for krav til 36 mnd arbeidsinntekt"] -->|"Oppslag"| B["Prøvingsdato"]
  DD["Inntektskrav for siste 36 mnd"] -->|"Multiplikasjon"| BO["Grunnbeløp"]
  DD["Inntektskrav for siste 36 mnd"] -->|"Multiplikasjon"| DC["Antall G for krav til 36 mnd arbeidsinntekt"]
  DE["Arbeidsinntekt er over kravet for siste 36 mnd"] -->|"StørreEnnEllerLik"| BR["Arbeidsinntekt siste 36 mnd"]
  DE["Arbeidsinntekt er over kravet for siste 36 mnd"] -->|"StørreEnnEllerLik"| DD["Inntektskrav for siste 36 mnd"]
  CA["Krav til minsteinntekt"] -->|"EnAv"| DB["Arbeidsinntekt er over kravet for siste 12 mnd"]
  CA["Krav til minsteinntekt"] -->|"EnAv"| DE["Arbeidsinntekt er over kravet for siste 36 mnd"]
  DF["Opphold i Norge"] -->|"Oppslag"| B["Prøvingsdato"]
  DG["Oppfyller unntak for opphold i Norge"] -->|"Oppslag"| B["Prøvingsdato"]
  CL["Oppfyller kravet til opphold i Norge"] -->|"EnAv"| DF["Opphold i Norge"]
  CL["Oppfyller kravet til opphold i Norge"] -->|"EnAv"| DG["Oppfyller unntak for opphold i Norge"]
  DH["Lovpålagt rapporteringsfrist for A-ordningen"] -->|"Oppslag"| B["Prøvingsdato"]
  DI["Arbeidsgivers rapporteringsfrist"] -->|"FørsteArbeidsdag"| DH["Lovpålagt rapporteringsfrist for A-ordningen"]
  CX["Siste avsluttende kalendermåned"] -->|"SisteavsluttendeKalenderMåned"| B["Prøvingsdato"]
  CX["Siste avsluttende kalendermåned"] -->|"SisteavsluttendeKalenderMåned"| DI["Arbeidsgivers rapporteringsfrist"]
  DJ["Det er godkjent at bruker kun søker deltidsarbeid"] -->|"Oppslag"| B["Prøvingsdato"]
  DK["Det er godkjent at bruker kun søk arbeid lokalt"] -->|"Oppslag"| B["Prøvingsdato"]
  DL["Oppfyller kravet til heltid- og deltidsarbeid"] -->|"EnAv"| DM["Kan jobbe heltid og deltid"]
  DL["Oppfyller kravet til heltid- og deltidsarbeid"] -->|"EnAv"| DJ["Det er godkjent at bruker kun søker deltidsarbeid"]
  DN["Oppfyller kravet til mobilitet"] -->|"EnAv"| DO["Kan jobbe i hele Norge"]
  DN["Oppfyller kravet til mobilitet"] -->|"EnAv"| DK["Det er godkjent at bruker kun søk arbeid lokalt"]
  DP["Oppfyller kravet til å være arbeidsfør"] -->|"EnAv"| DQ["Kan ta alle typer arbeid"]
  DR["Oppfyller kravet til å ta ethvert arbeid"] -->|"EnAv"| DS["Villig til å bytte yrke"]
  CM["Krav til arbeidssøker"] -->|"Alle"| DL["Oppfyller kravet til heltid- og deltidsarbeid"]
  CM["Krav til arbeidssøker"] -->|"Alle"| DN["Oppfyller kravet til mobilitet"]
  CM["Krav til arbeidssøker"] -->|"Alle"| DP["Oppfyller kravet til å være arbeidsfør"]
  CM["Krav til arbeidssøker"] -->|"Alle"| DR["Oppfyller kravet til å ta ethvert arbeid"]
  DT["Har rett til ordinære dagpenger uten arbeidsforhold"] -->|"IngenAv"| DU["Har rett til ordinære dagpenger gjennom arbeidsforhold"]
  DT["Har rett til ordinære dagpenger uten arbeidsforhold"] -->|"IngenAv"| DV["Har rett til dagpenger under permittering"]
  DT["Har rett til ordinære dagpenger uten arbeidsforhold"] -->|"IngenAv"| DW["Har rett til dagpenger etter konkurs"]
  DT["Har rett til ordinære dagpenger uten arbeidsforhold"] -->|"IngenAv"| DX["Har rett til dagpenger under permittering i fiskeforedlingsindustri"]
  DY["Har rett til ordinære dagpenger"] -->|"EnAv"| DU["Har rett til ordinære dagpenger gjennom arbeidsforhold"]
  DY["Har rett til ordinære dagpenger"] -->|"EnAv"| DT["Har rett til ordinære dagpenger uten arbeidsforhold"]
  CN["Rettighetstype"] -->|"EnAv"| DY["Har rett til ordinære dagpenger"]
  CN["Rettighetstype"] -->|"EnAv"| DV["Har rett til dagpenger under permittering"]
  CN["Rettighetstype"] -->|"EnAv"| DW["Har rett til dagpenger etter konkurs"]
  CN["Rettighetstype"] -->|"EnAv"| DX["Har rett til dagpenger under permittering i fiskeforedlingsindustri"]
  DZ["Mottar pensjon fra en offentlig tjenestepensjonsordning"] -->|"Oppslag"| B["Prøvingsdato"]
  EA["Mottar redusert uførepensjon fra offentlig pensjonsordning"] -->|"Oppslag"| B["Prøvingsdato"]
  EB["Mottar vartpenger"] -->|"Oppslag"| B["Prøvingsdato"]
  EC["Mottar ventelønn"] -->|"Oppslag"| B["Prøvingsdato"]
  ED["Mottar etterlønn"] -->|"Oppslag"| B["Prøvingsdato"]
  EE["Mottar garantilott fra Garantikassen for fiskere."] -->|"Oppslag"| B["Prøvingsdato"]
  EF["Pensjon fra en offentlig tjenestepensjonsordning beløp"] -->|"Oppslag"| B["Prøvingsdato"]
  EG["Uførepensjon fra offentlig pensjonsordning beløp"] -->|"Oppslag"| B["Prøvingsdato"]
  EH["Vartpenger beløp"] -->|"Oppslag"| B["Prøvingsdato"]
  EI["Ventelønn beløp"] -->|"Oppslag"| B["Prøvingsdato"]
  EJ["Etterlønn beløp"] -->|"Oppslag"| B["Prøvingsdato"]
  EK["Garantilott fra Garantikassen for fiskere beløp"] -->|"Oppslag"| B["Prøvingsdato"]
  EL["Sum av ytelser utenfor folketrygden"] -->|"SumAv"| EF["Pensjon fra en offentlig tjenestepensjonsordning beløp"]
  EL["Sum av ytelser utenfor folketrygden"] -->|"SumAv"| EG["Uførepensjon fra offentlig pensjonsordning beløp"]
  EL["Sum av ytelser utenfor folketrygden"] -->|"SumAv"| EH["Vartpenger beløp"]
  EL["Sum av ytelser utenfor folketrygden"] -->|"SumAv"| EI["Ventelønn beløp"]
  EL["Sum av ytelser utenfor folketrygden"] -->|"SumAv"| EJ["Etterlønn beløp"]
  EL["Sum av ytelser utenfor folketrygden"] -->|"SumAv"| EK["Garantilott fra Garantikassen for fiskere beløp"]
  EM["Hvor mange prosent av G skal brukes som terskel ved samordning"] -->|"Oppslag"| B["Prøvingsdato"]
  EN["Beløp tilsvarende nedre terskel av G"] -->|"Multiplikasjon"| J["Grunnbeløp for grunnlag"]
  EN["Beløp tilsvarende nedre terskel av G"] -->|"Multiplikasjon"| EM["Hvor mange prosent av G skal brukes som terskel ved samordning"]
  EO["Samordnet ukessats uten barnetillegg"] -->|"Multiplikasjon"| EP["Samordnet dagsats uten barnetillegg"]
  EO["Samordnet ukessats uten barnetillegg"] -->|"Multiplikasjon"| AU["Antall arbeidsdager per uke"]
  EQ["Minste mulige ukessats som som kan brukes"] -->|"MinstAv"| EO["Samordnet ukessats uten barnetillegg"]
  EQ["Minste mulige ukessats som som kan brukes"] -->|"MinstAv"| EN["Beløp tilsvarende nedre terskel av G"]
  ER["Ukessats trukket ned for ytelser utenfor folketrygden"] -->|"Substraksjon"| EO["Samordnet ukessats uten barnetillegg"]
  ER["Ukessats trukket ned for ytelser utenfor folketrygden"] -->|"Substraksjon"| EL["Sum av ytelser utenfor folketrygden"]
  ES["Samordnet ukessats med ytelser utenfor folketrygden"] -->|"HøyesteAv"| EQ["Minste mulige ukessats som som kan brukes"]
  ES["Samordnet ukessats med ytelser utenfor folketrygden"] -->|"HøyesteAv"| ER["Ukessats trukket ned for ytelser utenfor folketrygden"]
  BD["Dagsats uten barnetillegg samordnet"] -->|"Divisjon"| ES["Samordnet ukessats med ytelser utenfor folketrygden"]
  BD["Dagsats uten barnetillegg samordnet"] -->|"Divisjon"| AU["Antall arbeidsdager per uke"]
  ET["Skal samordnes med ytelser utenfor folketrygden"] -->|"EnAv"| EU["Oppgitt andre ytelser utenfor NAV i søknaden"]
  ET["Skal samordnes med ytelser utenfor folketrygden"] -->|"EnAv"| DZ["Mottar pensjon fra en offentlig tjenestepensjonsordning"]
  ET["Skal samordnes med ytelser utenfor folketrygden"] -->|"EnAv"| EA["Mottar redusert uførepensjon fra offentlig pensjonsordning"]
  ET["Skal samordnes med ytelser utenfor folketrygden"] -->|"EnAv"| EB["Mottar vartpenger"]
  ET["Skal samordnes med ytelser utenfor folketrygden"] -->|"EnAv"| EC["Mottar ventelønn"]
  ET["Skal samordnes med ytelser utenfor folketrygden"] -->|"EnAv"| ED["Mottar etterlønn"]
  ET["Skal samordnes med ytelser utenfor folketrygden"] -->|"EnAv"| EE["Mottar garantilott fra Garantikassen for fiskere."]
  ET["Skal samordnes med ytelser utenfor folketrygden"] -->|"EnAv"| EV["Mottar andre økonomiske ytelser fra arbeidsgiver eller tidligere arbeidsgiver enn lønn"]
  EW["Sykepenger etter lovens kapittel 8"] -->|"Ekstern"| B["Prøvingsdato"]
  EX["Pleiepenger etter lovens kapittel 9"] -->|"Ekstern"| B["Prøvingsdato"]
  EY["Omsorgspenger etter lovens kapittel 9"] -->|"Ekstern"| B["Prøvingsdato"]
  EZ["Opplæringspenger etter lovens kapittel 9"] -->|"Ekstern"| B["Prøvingsdato"]
  FA["Foreldrepenger etter lovens kapittel 14"] -->|"Ekstern"| B["Prøvingsdato"]
  FB["Svangerskapspenger etter lovens kapittel 14"] -->|"Ekstern"| B["Prøvingsdato"]
  FC["Uføretrygd etter lovens kapittel 12"] -->|"Oppslag"| B["Prøvingsdato"]
  FD["Sykepenger dagsats"] -->|"Oppslag"| B["Prøvingsdato"]
  FE["Pleiepenger dagsats"] -->|"Oppslag"| B["Prøvingsdato"]
  FF["Omsorgspenger dagsats"] -->|"Oppslag"| B["Prøvingsdato"]
  FG["Opplæringspenger dagsats"] -->|"Oppslag"| B["Prøvingsdato"]
  FH["Uføre dagsats"] -->|"Oppslag"| B["Prøvingsdato"]
  FI["Svangerskapspenger dagsats"] -->|"Oppslag"| B["Prøvingsdato"]
  FJ["Foreldrepenger dagsats"] -->|"Oppslag"| B["Prøvingsdato"]
  FK["Sum andre ytelser"] -->|"Addisjon"| FD["Sykepenger dagsats"]
  FK["Sum andre ytelser"] -->|"Addisjon"| FE["Pleiepenger dagsats"]
  FK["Sum andre ytelser"] -->|"Addisjon"| FF["Omsorgspenger dagsats"]
  FK["Sum andre ytelser"] -->|"Addisjon"| FG["Opplæringspenger dagsats"]
  FK["Sum andre ytelser"] -->|"Addisjon"| FH["Uføre dagsats"]
  FK["Sum andre ytelser"] -->|"Addisjon"| FJ["Foreldrepenger dagsats"]
  FK["Sum andre ytelser"] -->|"Addisjon"| FI["Svangerskapspenger dagsats"]
  EP["Samordnet dagsats uten barnetillegg"] -->|"Substraksjon"| BB["Andel av dagsats uten barnetillegg avkortet til maks andel av dagpengegrunnlaget"]
  EP["Samordnet dagsats uten barnetillegg"] -->|"Substraksjon"| FK["Sum andre ytelser"]
  FL["Samordnet dagsats er negativ eller 0"] -->|"StørreEnnEllerLik"| BB["Andel av dagsats uten barnetillegg avkortet til maks andel av dagpengegrunnlaget"]
  FL["Samordnet dagsats er negativ eller 0"] -->|"StørreEnnEllerLik"| FK["Sum andre ytelser"]
  CO["Utfall etter samordning"] -->|"EnAv"| FL["Samordnet dagsats er negativ eller 0"]
  CO["Utfall etter samordning"] -->|"EnAv"| BH["Har barnetillegg"]
  FM["Medlem har reduserte ytelser fra folketrygden (Samordning)"] -->|"EnAv"| EW["Sykepenger etter lovens kapittel 8"]
  FM["Medlem har reduserte ytelser fra folketrygden (Samordning)"] -->|"EnAv"| EX["Pleiepenger etter lovens kapittel 9"]
  FM["Medlem har reduserte ytelser fra folketrygden (Samordning)"] -->|"EnAv"| EY["Omsorgspenger etter lovens kapittel 9"]
  FM["Medlem har reduserte ytelser fra folketrygden (Samordning)"] -->|"EnAv"| EZ["Opplæringspenger etter lovens kapittel 9"]
  FM["Medlem har reduserte ytelser fra folketrygden (Samordning)"] -->|"EnAv"| FC["Uføretrygd etter lovens kapittel 12"]
  FM["Medlem har reduserte ytelser fra folketrygden (Samordning)"] -->|"EnAv"| FA["Foreldrepenger etter lovens kapittel 14"]
  FM["Medlem har reduserte ytelser fra folketrygden (Samordning)"] -->|"EnAv"| FB["Svangerskapspenger etter lovens kapittel 14"]
  FN["Deltar medlemmet i streik eller er omfattet av lock-out?"] -->|"Oppslag"| B["Prøvingsdato"]
  FO["Ledig ved samme bedrift eller arbeidsplass, og blir påvirket av utfallet?"] -->|"Oppslag"| B["Prøvingsdato"]
  CP["Er medlemmet ikke påvirket av streik eller lock-out?"] -->|"IngenAv"| FN["Deltar medlemmet i streik eller er omfattet av lock-out?"]
  CP["Er medlemmet ikke påvirket av streik eller lock-out?"] -->|"IngenAv"| FO["Ledig ved samme bedrift eller arbeidsplass, og blir påvirket av utfallet?"]
  FP["Søknadsdato"] -->|"Ekstern"| AL["søknadId"]
  FQ["Ønsker dagpenger fra dato"] -->|"Ekstern"| AL["søknadId"]
  FR["Søknadstidspunkt"] -->|"SisteAv"| FP["Søknadsdato"]
  FR["Søknadstidspunkt"] -->|"SisteAv"| FQ["Ønsker dagpenger fra dato"]
  B["Prøvingsdato"] -->|"SisteAv"| FR["Søknadstidspunkt"]
  FS["Har tapt arbeid"] -->|"Oppslag"| B["Prøvingsdato"]
  FT["Krav på lønn fra tidligere arbeidsgiver"] -->|"Oppslag"| B["Prøvingsdato"]
  FU["Ikke krav på lønn fra tidligere arbeidsgiver"] -->|"IngenAv"| FT["Krav på lønn fra tidligere arbeidsgiver"]
  FV["Krav til tap av arbeidsinntekt"] -->|"Alle"| FS["Har tapt arbeid"]
  FV["Krav til tap av arbeidsinntekt"] -->|"Alle"| FU["Ikke krav på lønn fra tidligere arbeidsgiver"]
  FW["Krav til prosentvis tap av arbeidstid"] -->|"Oppslag"| B["Prøvingsdato"]
  FX["Beregningsregel: Arbeidstid siste 6 måneder"] -->|"Oppslag"| B["Prøvingsdato"]
  FY["Beregningsregel: Arbeidstid siste 12 måneder"] -->|"Oppslag"| B["Prøvingsdato"]
  FZ["Beregeningsregel: Arbeidstid siste 36 måneder"] -->|"Oppslag"| B["Prøvingsdato"]
  GA["Beregnet vanlig arbeidstid per uke før tap"] -->|"Oppslag"| B["Prøvingsdato"]
  GB["Fastsatt vanlig arbeidstid etter ordinær eller verneplikt"] -->|"HvisSannMedResultat"| GC["Grunnlaget for verneplikt er høyere enn dagpengegrunnlaget"]
  GB["Fastsatt vanlig arbeidstid etter ordinær eller verneplikt"] -->|"HvisSannMedResultat"| GD["Fastsatt vanlig arbeidstid for verneplikt"]
  GB["Fastsatt vanlig arbeidstid etter ordinær eller verneplikt"] -->|"HvisSannMedResultat"| GA["Beregnet vanlig arbeidstid per uke før tap"]
  GE["Ny arbeidstid per uke"] -->|"Oppslag"| B["Prøvingsdato"]
  GF["Maksimal vanlig arbeidstid"] -->|"Oppslag"| B["Prøvingsdato"]
  GG["Fastsatt arbeidstid per uke før tap"] -->|"MinstAv"| GB["Fastsatt vanlig arbeidstid etter ordinær eller verneplikt"]
  GG["Fastsatt arbeidstid per uke før tap"] -->|"MinstAv"| GF["Maksimal vanlig arbeidstid"]
  GH["Tap av arbeidstid er minst terskel"] -->|"SjekkAvTerskel"| GE["Ny arbeidstid per uke"]
  GH["Tap av arbeidstid er minst terskel"] -->|"SjekkAvTerskel"| GG["Fastsatt arbeidstid per uke før tap"]
  GH["Tap av arbeidstid er minst terskel"] -->|"SjekkAvTerskel"| FW["Krav til prosentvis tap av arbeidstid"]
  GI["Beregningsregel: Tapt arbeidstid"] -->|"EnAv"| FX["Beregningsregel: Arbeidstid siste 6 måneder"]
  GI["Beregningsregel: Tapt arbeidstid"] -->|"EnAv"| FY["Beregningsregel: Arbeidstid siste 12 måneder"]
  GI["Beregningsregel: Tapt arbeidstid"] -->|"EnAv"| FZ["Beregeningsregel: Arbeidstid siste 36 måneder"]
  CQ["Krav til tap av arbeidsinntekt og arbeidstid"] -->|"Alle"| FV["Krav til tap av arbeidsinntekt"]
  CQ["Krav til tap av arbeidsinntekt og arbeidstid"] -->|"Alle"| GH["Tap av arbeidstid er minst terskel"]
  CQ["Krav til tap av arbeidsinntekt og arbeidstid"] -->|"Alle"| GI["Beregningsregel: Tapt arbeidstid"]
  GJ["Deltar i arbeidsmarkedstiltak"] -->|"Oppslag"| B["Prøvingsdato"]
  GK["Deltar i opplæring for innvandrere"] -->|"Oppslag"| B["Prøvingsdato"]
  GL["Deltar i grunnskoleopplæring, videregående opplæring og opplæring i grunnleggende ferdigheter"] -->|"Oppslag"| B["Prøvingsdato"]
  GM["Deltar i høyere yrkesfaglig utdanning"] -->|"Oppslag"| B["Prøvingsdato"]
  GN["Deltar i høyere utdanning"] -->|"Oppslag"| B["Prøvingsdato"]
  GO["Deltar på kurs mv"] -->|"Oppslag"| B["Prøvingsdato"]
  GP["Godkjent unntak for utdanning eller opplæring?"] -->|"EnAv"| GJ["Deltar i arbeidsmarkedstiltak"]
  GP["Godkjent unntak for utdanning eller opplæring?"] -->|"EnAv"| GK["Deltar i opplæring for innvandrere"]
  GP["Godkjent unntak for utdanning eller opplæring?"] -->|"EnAv"| GL["Deltar i grunnskoleopplæring, videregående opplæring og opplæring i grunnleggende ferdigheter"]
  GP["Godkjent unntak for utdanning eller opplæring?"] -->|"EnAv"| GM["Deltar i høyere yrkesfaglig utdanning"]
  GP["Godkjent unntak for utdanning eller opplæring?"] -->|"EnAv"| GN["Deltar i høyere utdanning"]
  GP["Godkjent unntak for utdanning eller opplæring?"] -->|"EnAv"| GO["Deltar på kurs mv"]
  GQ["Har svart ja på spørsmål om utdanning eller opplæring"] -->|"ErSann"| GR["Tar utdanning eller opplæring?"]
  GS["Har svart nei på spørsmål om utdanning eller opplæring"] -->|"ErUsann"| GR["Tar utdanning eller opplæring?"]
  GT["Oppfyller kravet på unntak for utdanning eller opplæring"] -->|"Alle"| GQ["Har svart ja på spørsmål om utdanning eller opplæring"]
  GT["Oppfyller kravet på unntak for utdanning eller opplæring"] -->|"Alle"| GP["Godkjent unntak for utdanning eller opplæring?"]
  CR["Krav til utdanning eller opplæring"] -->|"EnAv"| GT["Oppfyller kravet på unntak for utdanning eller opplæring"]
  CR["Krav til utdanning eller opplæring"] -->|"EnAv"| GS["Har svart nei på spørsmål om utdanning eller opplæring"]
  GU["Bruker er utestengt fra dagpenger"] -->|"Oppslag"| B["Prøvingsdato"]
  CS["Oppfyller krav til ikke utestengt"] -->|"IngenAv"| GU["Bruker er utestengt fra dagpenger"]
  GV["Avtjent verneplikt"] -->|"Ekstern"| AL["søknadId"]
  CH["Har utført minst tre måneders militærtjeneste eller obligatorisk sivilforsvarstjeneste"] -->|"ErSann"| GV["Avtjent verneplikt"]
  GW["Antall G som gis som grunnlag ved verneplikt"] -->|"Oppslag"| B["Prøvingsdato"]
  GX["Grunnlag for gis ved verneplikt"] -->|"Multiplikasjon"| J["Grunnbeløp for grunnlag"]
  GX["Grunnlag for gis ved verneplikt"] -->|"Multiplikasjon"| GW["Antall G som gis som grunnlag ved verneplikt"]
  GY["Periode som gis ved verneplikt"] -->|"Oppslag"| B["Prøvingsdato"]
  GD["Fastsatt vanlig arbeidstid for verneplikt"] -->|"Oppslag"| B["Prøvingsdato"]
  GZ["Grunnlag for verneplikt hvis kravet ikke er oppfylt"] -->|"Oppslag"| B["Prøvingsdato"]
  AF["Grunnlag for verneplikt hvis kravet er oppfylt"] -->|"HvisSannMedResultat"| CH["Har utført minst tre måneders militærtjeneste eller obligatorisk sivilforsvarstjeneste"]
  AF["Grunnlag for verneplikt hvis kravet er oppfylt"] -->|"HvisSannMedResultat"| GX["Grunnlag for gis ved verneplikt"]
  AF["Grunnlag for verneplikt hvis kravet er oppfylt"] -->|"HvisSannMedResultat"| GZ["Grunnlag for verneplikt hvis kravet ikke er oppfylt"]
  GC["Grunnlaget for verneplikt er høyere enn dagpengegrunnlaget"] -->|"StørreEnn"| AF["Grunnlag for verneplikt hvis kravet er oppfylt"]
  GC["Grunnlaget for verneplikt er høyere enn dagpengegrunnlaget"] -->|"StørreEnn"| AD["Grunnlag ved ordinære dagpenger"]
  HA["EttBeregnetVirkningstidspunkt"] -->|"FraOgMedForOpplysning"| CI["Krav på dagpenger"]
```