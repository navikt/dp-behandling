# Dokumentasjon av opplysninger

Dette er opplysninger som blir brukt av regelverket. 

 UUID og datatype er en unik identifikator for opplysningstypen. Den skal _ALDRI_ endres. Beskrivelse og behovId kan endres. 
 
 For nye opplysningtyper, generer en ny UUID og legg til.
 
 Generering av UUID kan gjøres med UUIDv7.ny() i Kotlin

|UUID|Beskrivelse|Behov|Logisk datatype|Datatype|
|--|---|---|---|---|
|0194881f-91d1-7df2-ba1d-4533f37fcc73|Søknadsdato|Søknadsdato|Dato|LocalDate|
|0194881f-91d1-7df2-ba1d-4533f37fcc74|Ønsker dagpenger fra dato|ØnskerDagpengerFraDato|Dato|LocalDate|
|0194881f-91d1-7df2-ba1d-4533f37fcc75|Søknadstidspunkt|Søknadstidspunkt|Dato|LocalDate|
|0194881f-91d1-7df2-ba1d-4533f37fcc76|Prøvingsdato|Prøvingsdato|Dato|LocalDate|
|0194881f-91d1-7df2-ba1d-4533f37fcc77|søknadId|søknadId|Tekst|String|
|0194881f-91df-746a-a8ac-4a6b2b30685d|Deltar medlemmet i streik eller er omfattet av lock-out?|Deltar medlemmet i streik eller er omfattet av lock-out?|Boolsk|boolean|
|0194881f-91df-746a-a8ac-4a6b2b30685e|Ledig ved samme bedrift eller arbeidsplass, og blir påvirket av utfallet?|Ledig ved samme bedrift eller arbeidsplass, og blir påvirket av utfallet?|Boolsk|boolean|
|0194881f-91df-746a-a8ac-4a6b2b30685f|Er medlemmet ikke påvirket av streik eller lock-out?|Er medlemmet ikke påvirket av streik eller lock-out?|Boolsk|boolean|
|0194881f-940b-76ff-acf5-ba7bcb367233|Fødselsdato|Fødselsdato|Dato|LocalDate|
|0194881f-940b-76ff-acf5-ba7bcb367234|Aldersgrense|Aldersgrense|Heltall|int|
|0194881f-940b-76ff-acf5-ba7bcb367235|Dato søker når maks alder|Dato søker når maks alder|Dato|LocalDate|
|0194881f-940b-76ff-acf5-ba7bcb367236|Siste mulige dag bruker kan oppfylle alderskrav|Siste mulige dag bruker kan oppfylle alderskrav|Dato|LocalDate|
|0194881f-940b-76ff-acf5-ba7bcb367237|Oppfyller kravet til alder|Oppfyller kravet til alder|Boolsk|boolean|
|0194881f-940f-7af9-9387-052e028b29ec|Oppjustert inntekt|Oppjustert inntekt|InntektDataType|Inntekt|
|0194881f-940f-7af9-9387-052e028b29ed|Tellende inntekt|Tellende inntekt|InntektDataType|Inntekt|
|0194881f-940f-7af9-9387-052e028b29ee|Grunnbeløp for grunnlag|Grunnbeløp for grunnlag|Penger|Beløp|
|0194881f-9410-7481-b263-4606fdd10ca7|Faktor for maksimalt mulig grunnlag|Faktor for maksimalt mulig grunnlag|Desimaltall|double|
|0194881f-9410-7481-b263-4606fdd10ca8|6 ganger grunnbeløp|6 ganger grunnbeløp|Penger|Beløp|
|0194881f-9410-7481-b263-4606fdd10ca9|Antall år i 36 måneder|Antall år i 36 måneder|Desimaltall|double|
|0194881f-9410-7481-b263-4606fdd10caa|Grunnlag siste 12 mnd.|Grunnlag siste 12 mnd.|Penger|Beløp|
|0194881f-9410-7481-b263-4606fdd10cab|Inntekt siste 36 måneder|Inntekt siste 36 måneder|Penger|Beløp|
|0194881f-9410-7481-b263-4606fdd10cac|Gjennomsnittlig arbeidsinntekt siste 36 måneder|Gjennomsnittlig arbeidsinntekt siste 36 måneder|Penger|Beløp|
|0194881f-9410-7481-b263-4606fdd10cad|Utbetalt arbeidsinntekt periode 1|Utbetalt arbeidsinntekt periode 1|Penger|Beløp|
|0194881f-9410-7481-b263-4606fdd10cae|Utbetalt arbeidsinntekt periode 2|Utbetalt arbeidsinntekt periode 2|Penger|Beløp|
|0194881f-9410-7481-b263-4606fdd10caf|Utbetalt arbeidsinntekt periode 3|Utbetalt arbeidsinntekt periode 3|Penger|Beløp|
|0194881f-9410-7481-b263-4606fdd10cb0|Inntektperiode 1|Inntektperiode 1|Penger|Beløp|
|0194881f-9410-7481-b263-4606fdd10cb1|Inntektperiode 2|Inntektperiode 2|Penger|Beløp|
|0194881f-9410-7481-b263-4606fdd10cb2|Inntektperiode 3|Inntektperiode 3|Penger|Beløp|
|0194881f-9410-7481-b263-4606fdd10cb3|Avkortet inntektperiode 1|Avkortet inntektperiode 1|Penger|Beløp|
|0194881f-9410-7481-b263-4606fdd10cb4|Avkortet inntektperiode 2|Avkortet inntektperiode 2|Penger|Beløp|
|0194881f-9410-7481-b263-4606fdd10cb5|Avkortet inntektperiode 3|Avkortet inntektperiode 3|Penger|Beløp|
|0194881f-9410-7481-b263-4606fdd10cb6|Har avkortet grunnlaget i periode 1|Har avkortet grunnlaget i periode 1|Boolsk|boolean|
|0194881f-9410-7481-b263-4606fdd10cb7|Har avkortet grunnlaget i periode 2|Har avkortet grunnlaget i periode 2|Boolsk|boolean|
|0194881f-9410-7481-b263-4606fdd10cb8|Har avkortet grunnlaget i periode 3|Har avkortet grunnlaget i periode 3|Boolsk|boolean|
|0194881f-9410-7481-b263-4606fdd10cb9|Har avkortet grunnlag|Har avkortet grunnlag|Boolsk|boolean|
|0194881f-9410-7481-b263-4606fdd10cba|Brukt beregningsregel|Brukt beregningsregel|Tekst|String|
|0194881f-9410-7481-b263-4606fdd10cbb|Uavrundet grunnlag|Uavrundet grunnlag|Penger|Beløp|
|0194881f-9410-7481-b263-4606fdd10cbc|Grunnlag ved ordinære dagpenger|Grunnlag ved ordinære dagpenger|Penger|Beløp|
|0194881f-9410-7481-b263-4606fdd10cbd|Grunnlag|Grunnlag|Penger|Beløp|
|0194881f-9410-7481-b263-4606fdd10cbe|Uavkortet grunnlag siste 12 mnd|Uavkortet grunnlag siste 12 mnd|Penger|Beløp|
|0194881f-9410-7481-b263-4606fdd10cbf|Uavkortet grunnlag siste 36 mnd|Uavkortet grunnlag siste 36 mnd|Penger|Beløp|
|0194881f-9413-77ce-92ec-d29700f0423f|Antall G for krav til 12 mnd arbeidsinntekt|Antall G for krav til 12 mnd arbeidsinntekt|Desimaltall|double|
|0194881f-9413-77ce-92ec-d29700f04240|Antall G for krav til 36 mnd arbeidsinntekt|Antall G for krav til 36 mnd arbeidsinntekt|Desimaltall|double|
|0194881f-9413-77ce-92ec-d29700f04241|Arbeidsinntekt siste 12 mnd|InntektSiste12Mnd|Penger|Beløp|
|0194881f-9413-77ce-92ec-d29700f04242|Arbeidsinntekt siste 36 mnd|InntektSiste36Mnd|Penger|Beløp|
|0194881f-9413-77ce-92ec-d29700f04243|Grunnbeløp|Grunnbeløp|Penger|Beløp|
|0194881f-9413-77ce-92ec-d29700f04244|Inntektsopplysninger|Inntekt|InntektDataType|Inntekt|
|0194881f-9413-77ce-92ec-d29700f04245|Brutto arbeidsinntekt|Brutto arbeidsinntekt|InntektDataType|Inntekt|
|0194881f-9413-77ce-92ec-d29700f04246|Maks lengde på opptjeningsperiode|Maks lengde på opptjeningsperiode|Heltall|int|
|0194881f-9413-77ce-92ec-d29700f04247|Første måned av opptjeningsperiode|OpptjeningsperiodeFraOgMed|Dato|LocalDate|
|0194881f-9413-77ce-92ec-d29700f04248|Inntektskrav for siste 12 mnd|Inntektskrav for siste 12 mnd|Penger|Beløp|
|0194881f-9413-77ce-92ec-d29700f04249|Inntektskrav for siste 36 mnd|Inntektskrav for siste 36 mnd|Penger|Beløp|
|0194881f-9413-77ce-92ec-d29700f0424a|Arbeidsinntekt er over kravet for siste 12 mnd|Arbeidsinntekt er over kravet for siste 12 mnd|Boolsk|boolean|
|0194881f-9413-77ce-92ec-d29700f0424b|Arbeidsinntekt er over kravet for siste 36 mnd|Arbeidsinntekt er over kravet for siste 36 mnd|Boolsk|boolean|
|0194881f-9413-77ce-92ec-d29700f0424c|Oppfyller kravet til minsteinntekt|Oppfyller kravet til minsteinntekt|Boolsk|boolean|
|0194881f-9414-7823-8d29-0e25b7feb7ce|Lovpålagt rapporteringsfrist for A-ordningen|Lovpålagt rapporteringsfrist for A-ordningen|Dato|LocalDate|
|0194881f-9414-7823-8d29-0e25b7feb7cf|Arbeidsgivers rapporteringsfrist|Arbeidsgivers rapporteringsfrist|Dato|LocalDate|
|0194881f-9414-7823-8d29-0e25b7feb7d0|Siste avsluttende kalendermåned|SisteAvsluttendeKalenderMåned|Dato|LocalDate|
|0194881f-9421-766c-9dc6-41fe6c9a1dff|Antall G som gis som grunnlag ved verneplikt|Antall G som gis som grunnlag ved verneplikt|Desimaltall|double|
|0194881f-9421-766c-9dc6-41fe6c9a1e00|Grunnlag for gis ved verneplikt|Grunnlag for gis ved verneplikt|Penger|Beløp|
|0194881f-9421-766c-9dc6-41fe6c9a1e01|Periode som gis ved verneplikt|Periode som gis ved verneplikt|Heltall|int|
|0194881f-9421-766c-9dc6-41fe6c9a1e02|Fastsatt vanlig arbeidstid for verneplikt|Fastsatt vanlig arbeidstid for verneplikt|Desimaltall|double|
|0194881f-9421-766c-9dc6-41fe6c9a1e03|Grunnlag for verneplikt hvis kravet er oppfylt|Grunnlag for verneplikt hvis kravet er oppfylt|Penger|Beløp|
|0194881f-9421-766c-9dc6-41fe6c9a1e04|Grunnlag for verneplikt hvis kravet ikke er oppfylt|Grunnlag for verneplikt hvis kravet ikke er oppfylt|Penger|Beløp|
|0194881f-9421-766c-9dc6-41fe6c9a1e05|Grunnlaget for verneplikt er høyere enn dagpengegrunnlaget|Grunnlaget for verneplikt er høyere enn dagpengegrunnlaget|Boolsk|boolean|
|0194881f-9428-74d5-b160-f63a4c61a23b|Barn|Barnetillegg|BarnDatatype|BarnListe|
|0194881f-9428-74d5-b160-f63a4c61a23c|Antall barn som gir rett til barnetillegg|Antall barn som gir rett til barnetillegg|Heltall|int|
|0194881f-9428-74d5-b160-f63a4c61a23d|Barnetilleggets størrelse i kroner per dag for hvert barn|Barnetilleggets størrelse i kroner per dag for hvert barn|Penger|Beløp|
|0194881f-9428-74d5-b160-f63a4c61a23e|Faktor for utregning av dagsats etter dagpengegrunnlaget|Faktor for utregning av dagsats etter dagpengegrunnlaget|Desimaltall|double|
|0194881f-9428-74d5-b160-f63a4c61a23f|Dagsats uten barnetillegg før samordning|Dagsats uten barnetillegg før samordning|Penger|Beløp|
|0194881f-9428-74d5-b160-f63a4c61a240|Avrundet ukessats med barnetillegg før samordning|Avrundet ukessats med barnetillegg før samordning|Penger|Beløp|
|0194881f-9428-74d5-b160-f63a4c61a241|Avrundet dagsats uten barnetillegg før samordning|Avrundet dagsats uten barnetillegg før samordning|Penger|Beløp|
|0194881f-9428-74d5-b160-f63a4c61a242|Andel av dagsats med barnetillegg som overstiger maks andel av dagpengegrunnlaget|Andel av dagsats med barnetillegg som overstiger maks andel av dagpengegrunnlaget|Penger|Beløp|
|0194881f-9428-74d5-b160-f63a4c61a243|Andel av dagsats uten barnetillegg avkortet til maks andel av dagpengegrunnlaget|Andel av dagsats uten barnetillegg avkortet til maks andel av dagpengegrunnlaget|Penger|Beløp|
|0194881f-9428-74d5-b160-f63a4c61a244|Sum av barnetillegg|Sum av barnetillegg|Penger|Beløp|
|0194881f-9428-74d5-b160-f63a4c61a245|Dagsats med barnetillegg før samordning|Dagsats med barnetillegg før samordning|Penger|Beløp|
|0194881f-9428-74d5-b160-f63a4c61a246|90% av grunnlag for dagpenger|90% av grunnlag for dagpenger|Desimaltall|double|
|0194881f-9428-74d5-b160-f63a4c61a247|Antall arbeidsdager per år|Antall arbeidsdager per år|Heltall|int|
|0194881f-9428-74d5-b160-f63a4c61a248|Maksimalt mulig grunnlag avgrenset til 90% av dagpengegrunnlaget|Maksimalt mulig grunnlag avgrenset til 90% av dagpengegrunnlaget|Penger|Beløp|
|0194881f-9428-74d5-b160-f63a4c61a249|Antall arbeidsdager per uke|Antall arbeidsdager per uke|Heltall|int|
|0194881f-9428-74d5-b160-f63a4c61a24a|Maksimal mulig dagsats avgrenset til 90% av dagpengegrunnlaget|Maksimal mulig dagsats avgrenset til 90% av dagpengegrunnlaget|Penger|Beløp|
|0194881f-9428-74d5-b160-f63a4c61a24b|Avrundet maksimal mulig dagsats avgrenset til 90% av dagpengegrunnlaget|Avrundet maksimal mulig dagsats avgrenset til 90% av dagpengegrunnlaget|Penger|Beløp|
|0194881f-9428-74d5-b160-f63a4c61a24c|Har barnetillegg|Har barnetillegg|Boolsk|boolean|
|0194881f-9428-74d5-b160-f63a4c61a24d|Samordnet dagsats med barnetillegg|Samordnet dagsats med barnetillegg|Penger|Beløp|
|0194881f-9428-74d5-b160-f63a4c61a24e|Ukessats med barnetillegg etter samordning|Ukessats med barnetillegg etter samordning|Penger|Beløp|
|0194881f-9428-74d5-b160-f63a4c61a24f|Dagsats med barnetillegg etter samordning og 90% regel|Dagsats med barnetillegg etter samordning og 90% regel|Penger|Beløp|
|0194881f-9428-74d5-b160-f63a4c61a250|Har samordnet|Har samordnet|Boolsk|boolean|
|0194881f-942e-7cb0-aa59-05ea449d88e0|Oppgitt andre ytelser utenfor NAV i søknaden|OppgittAndreYtelserUtenforNav|Boolsk|boolean|
|0194881f-942e-7cb0-aa59-05ea449d88e1|Mottar pensjon fra en offentlig tjenestepensjonsordning|Mottar pensjon fra en offentlig tjenestepensjonsordning|Boolsk|boolean|
|0194881f-942e-7cb0-aa59-05ea449d88e2|Mottar redusert uførepensjon fra offentlig pensjonsordning|Mottar redusert uførepensjon fra offentlig pensjonsordning|Boolsk|boolean|
|0194881f-942e-7cb0-aa59-05ea449d88e3|Mottar vartpenger|Mottar vartpenger|Boolsk|boolean|
|0194881f-942e-7cb0-aa59-05ea449d88e4|Mottar ventelønn|Mottar ventelønn|Boolsk|boolean|
|0194881f-942e-7cb0-aa59-05ea449d88e5|Mottar etterlønn|Mottar etterlønn|Boolsk|boolean|
|0194881f-942e-7cb0-aa59-05ea449d88e6|Mottar garantilott fra Garantikassen for fiskere.|Mottar garantilott fra Garantikassen for fiskere.|Boolsk|boolean|
|0194881f-942e-7cb0-aa59-05ea449d88e7|Pensjon fra en offentlig tjenestepensjonsordning beløp|Pensjon fra en offentlig tjenestepensjonsordning beløp|Penger|Beløp|
|0194881f-942e-7cb0-aa59-05ea449d88e8|Uførepensjon fra offentlig pensjonsordning beløp|Uførepensjon fra offentlig pensjonsordning beløp|Penger|Beløp|
|0194881f-942f-7bde-ab16-68ffd19e9a26|Vartpenger beløp|Vartpenger beløp|Penger|Beløp|
|0194881f-942f-7bde-ab16-68ffd19e9a27|Ventelønn beløp|Ventelønn beløp|Penger|Beløp|
|0194881f-942f-7bde-ab16-68ffd19e9a28|Etterlønn beløp|Etterlønn beløp|Penger|Beløp|
|0194881f-942f-7bde-ab16-68ffd19e9a29|Garantilott fra Garantikassen for fiskere beløp|Garantilott fra Garantikassen for fiskere beløp|Penger|Beløp|
|0194881f-942f-7bde-ab16-68ffd19e9a2a|Mottar andre økonomiske ytelser fra arbeidsgiver eller tidligere arbeidsgiver enn lønn|AndreØkonomiskeYtelser|Boolsk|boolean|
|0194881f-942f-7bde-ab16-68ffd19e9a2b|Hvor mange prosent av G skal brukes som terskel ved samordning|Hvor mange prosent av G skal brukes som terskel ved samordning|Desimaltall|double|
|0194881f-942f-7bde-ab16-68ffd19e9a2c|Beløp tilsvarende nedre terskel av G|Beløp tilsvarende nedre terskel av G|Penger|Beløp|
|0194881f-942f-7bde-ab16-68ffd19e9a2d|Skal samordnes med ytelser utenfor folketrygden|Skal samordnes med ytelser utenfor folketrygden|Boolsk|boolean|
|0194881f-942f-7bde-ab16-68ffd19e9a2e|Sum av ytelser utenfor folketrygden|Sum av ytelser utenfor folketrygden|Penger|Beløp|
|0194881f-942f-7bde-ab16-68ffd19e9a2f|Samordnet ukessats uten barnetillegg|Samordnet ukessats uten barnetillegg|Penger|Beløp|
|0194881f-942f-7bde-ab16-68ffd19e9a30|Minste mulige ukessats som som kan brukes|Minste mulige ukessats som som kan brukes|Penger|Beløp|
|0194881f-942f-7bde-ab16-68ffd19e9a31|Ukessats trukket ned for ytelser utenfor folketrygden|Ukessats trukket ned for ytelser utenfor folketrygden|Penger|Beløp|
|0194881f-942f-7bde-ab16-68ffd19e9a32|Samordnet ukessats med ytelser utenfor folketrygden|Samordnet ukessats med ytelser utenfor folketrygden|Penger|Beløp|
|0194881f-942f-7bde-ab16-68ffd19e9a33|Dagsats uten barnetillegg samordnet|Dagsats uten barnetillegg samordnet|Penger|Beløp|
|0194881f-9433-70e9-a85b-c246150c45cd|Sykepenger etter lovens kapittel 8|Sykepenger|Boolsk|boolean|
|0194881f-9433-70e9-a85b-c246150c45ce|Pleiepenger etter lovens kapittel 9|Pleiepenger|Boolsk|boolean|
|0194881f-9433-70e9-a85b-c246150c45cf|Omsorgspenger etter lovens kapittel 9|Omsorgspenger|Boolsk|boolean|
|0194881f-9433-70e9-a85b-c246150c45d0|Opplæringspenger etter lovens kapittel 9|Opplæringspenger|Boolsk|boolean|
|0194881f-9433-70e9-a85b-c246150c45d1|Uføretrygd etter lovens kapittel 12|Uføre|Boolsk|boolean|
|0194881f-9433-70e9-a85b-c246150c45d2|Foreldrepenger etter lovens kapittel 14|Foreldrepenger|Boolsk|boolean|
|0194881f-9433-70e9-a85b-c246150c45d3|Svangerskapspenger etter lovens kapittel 14|Svangerskapspenger|Boolsk|boolean|
|0194881f-9433-70e9-a85b-c246150c45d4|Sykepenger dagsats|Sykepenger dagsats|Penger|Beløp|
|0194881f-9433-70e9-a85b-c246150c45d5|Pleiepenger dagsats|Pleiepenger dagsats|Penger|Beløp|
|0194881f-9433-70e9-a85b-c246150c45d6|Omsorgspenger dagsats|Omsorgspenger dagsats|Penger|Beløp|
|0194881f-9433-70e9-a85b-c246150c45d7|Opplæringspenger dagsats|Opplæringspenger dagsats|Penger|Beløp|
|0194881f-9433-70e9-a85b-c246150c45d8|Uføre dagsats|Uføre dagsats|Penger|Beløp|
|0194881f-9433-70e9-a85b-c246150c45d9|Foreldrepenger dagsats|Foreldrepenger dagsats|Penger|Beløp|
|0194881f-9433-70e9-a85b-c246150c45da|Svangerskapspenger dagsats|Svangerskapspenger dagsats|Penger|Beløp|
|0194881f-9434-79e8-a64d-1a23cc5d86e9|Sum andre ytelser|Sum andre ytelser|Penger|Beløp|
|0194881f-9434-79e8-a64d-1a23cc5d86ea|Medlem har reduserte ytelser fra folketrygden (Samordning)|Medlem har reduserte ytelser fra folketrygden (Samordning)|Boolsk|boolean|
|0194881f-9434-79e8-a64d-1a23cc5d86eb|Samordnet dagsats uten barnetillegg|Samordnet dagsats uten barnetillegg|Penger|Beløp|
|0194881f-9434-79e8-a64d-1a23cc5d86ec|Samordnet dagsats er større enn 0|Samordnet dagsats er større enn 0|Boolsk|boolean|
|0194881f-9434-79e8-a64d-1a23cc5d86ed|Antall timer arbeidstiden skal samordnes mot|Antall timer arbeidstiden skal samordnes mot|Desimaltall|double|
|0194881f-9434-79e8-a64d-1a23cc5d86ee|Samordnet fastsatt arbeidstid|Samordnet fastsatt arbeidstid|Desimaltall|double|
|0194881f-9434-79e8-a64d-1a23cc5d86ef|Utfall etter samordning|Utfall etter samordning|Boolsk|boolean|
|0194881f-9435-72a8-b1ce-9575cbc2a75d|Har tapt arbeid|HarTaptArbeid|Boolsk|boolean|
|0194881f-9435-72a8-b1ce-9575cbc2a75e|Krav på lønn fra tidligere arbeidsgiver|KravPåLønn|Boolsk|boolean|
|0194881f-9435-72a8-b1ce-9575cbc2a75f|Ønsket arbeidstid|ØnsketArbeidstid|Desimaltall|double|
|0194881f-9435-72a8-b1ce-9575cbc2a760|Ikke krav på lønn fra tidligere arbeidsgiver|Ikke krav på lønn fra tidligere arbeidsgiver|Boolsk|boolean|
|0194881f-9435-72a8-b1ce-9575cbc2a761|Krav til tap av arbeidsinntekt|Krav til tap av arbeidsinntekt|Boolsk|boolean|
|0194881f-9435-72a8-b1ce-9575cbc2a762|Krav til prosentvis tap av arbeidstid|Krav til prosentvis tap av arbeidstid|Desimaltall|double|
|0194881f-9435-72a8-b1ce-9575cbc2a763|Beregningsregel: Tapt arbeidstid|Beregningsregel: Tapt arbeidstid|Boolsk|boolean|
|0194881f-9435-72a8-b1ce-9575cbc2a764|Beregningsregel: Arbeidstid siste 6 måneder|Beregningsregel: Arbeidstid siste 6 måneder|Boolsk|boolean|
|0194881f-9435-72a8-b1ce-9575cbc2a765|Beregningsregel: Arbeidstid siste 12 måneder|Beregningsregel: Arbeidstid siste 12 måneder|Boolsk|boolean|
|0194881f-9435-72a8-b1ce-9575cbc2a766|Beregningsregel: Arbeidstid siste 36 måneder|Beregningsregel: Arbeidstid siste 36 måneder|Boolsk|boolean|
|0194881f-9435-72a8-b1ce-9575cbc2a767|Beregnet vanlig arbeidstid per uke før tap|Beregnet vanlig arbeidstid per uke før tap|Desimaltall|double|
|0194881f-9435-72a8-b1ce-9575cbc2a768|Maksimal vanlig arbeidstid|Maksimal vanlig arbeidstid|Desimaltall|double|
|0194881f-9435-72a8-b1ce-9575cbc2a769|Minimum vanlig arbeidstid|Minimum vanlig arbeidstid|Desimaltall|double|
|0194881f-9435-72a8-b1ce-9575cbc2a76a|Fastsatt arbeidstid per uke før tap|Fastsatt arbeidstid per uke før tap|Desimaltall|double|
|0194881f-9435-72a8-b1ce-9575cbc2a76b|Ny arbeidstid per uke|Ny arbeidstid per uke|Desimaltall|double|
|0194881f-9435-72a8-b1ce-9575cbc2a76c|Fastsatt vanlig arbeidstid etter ordinær eller verneplikt|Fastsatt vanlig arbeidstid etter ordinær eller verneplikt|Desimaltall|double|
|0194881f-9435-72a8-b1ce-9575cbc2a76d|Villig til å jobbe minimum arbeidstid|Villig til å jobbe minimum arbeidstid|Boolsk|boolean|
|0194881f-9435-72a8-b1ce-9575cbc2a76e|Tap av arbeidstid er minst terskel|Tap av arbeidstid er minst terskel|Boolsk|boolean|
|0194881f-9435-72a8-b1ce-9575cbc2a76f|Krav til tap av arbeidsinntekt og arbeidstid|Krav til tap av arbeidsinntekt og arbeidstid|Boolsk|boolean|
|0194881f-943d-77a7-969c-147999f15449|Antall dager som skal regnes med i hver uke|Antall dager som skal regnes med i hver uke|Heltall|int|
|0194881f-943d-77a7-969c-147999f1544a|Kort dagpengeperiode|Kort dagpengeperiode|Heltall|int|
|0194881f-943d-77a7-969c-147999f1544b|Lang dagpengeperiode|Lang dagpengeperiode|Heltall|int|
|0194881f-943d-77a7-969c-147999f1544c|Terskel for 12 måneder|Terskel for 12 måneder|Penger|Beløp|
|0194881f-943d-77a7-969c-147999f1544d|Terskel for 36 måneder|Terskel for 36 måneder|Penger|Beløp|
|0194881f-943d-77a7-969c-147999f1544e|Divisior|Divisior|Desimaltall|double|
|0194881f-943d-77a7-969c-147999f1544f|Terskelfaktor for 12 måneder|Terskelfaktor for 12 måneder|Desimaltall|double|
|0194881f-943d-77a7-969c-147999f15450|Terskelfaktor for 36 måneder|Terskelfaktor for 36 måneder|Desimaltall|double|
|0194881f-943d-77a7-969c-147999f15451|Snittinntekt siste 36 måneder|Snittinntekt siste 36 måneder|Penger|Beløp|
|0194881f-943d-77a7-969c-147999f15452|Stønadsuker ved siste 12 måneder|Stønadsuker ved siste 12 måneder|Heltall|int|
|0194881f-943d-77a7-969c-147999f15453|Stønadsuker ved siste 36 måneder|Stønadsuker ved siste 36 måneder|Heltall|int|
|0194881f-943d-77a7-969c-147999f15454|Over terskel for 12 måneder|Over terskel for 12 måneder|Boolsk|boolean|
|0194881f-943d-77a7-969c-147999f15455|Over terskel for 36 måneder|Over terskel for 36 måneder|Boolsk|boolean|
|0194881f-943d-77a7-969c-147999f15456|Antall stønadsuker|Antall stønadsuker|Heltall|int|
|0194881f-943d-77a7-969c-147999f15457|Antall gjenstående stønadsdager|Antall gjenstående stønadsdager|Heltall|int|
|0194881f-943d-77a7-969c-147999f15458|Stønadsuker når kravet til minste arbeidsinntekt ikke er oppfylt|Stønadsuker når kravet til minste arbeidsinntekt ikke er oppfylt|Heltall|int|
|0194881f-943d-77a7-969c-147999f15459|Antall stønadsuker som gis ved ordinære dagpenger|Antall stønadsuker som gis ved ordinære dagpenger|Heltall|int|
|0194881f-943f-78d9-b874-00a4944c54ef|Egenandel|Egenandel|Penger|Beløp|
|0194881f-943f-78d9-b874-00a4944c54f0|Antall dagsats for egenandel|Antall dagsats for egenandel|Desimaltall|double|
|0194881f-943f-78d9-b874-00a4944c54f1|Mottar ikke andre fulle ytelser|Mottar ikke andre fulle ytelser|Boolsk|boolean|
|0194881f-9441-7d1b-a06a-6727543a141e|Kan jobbe heltid og deltid|KanJobbeDeltid|Boolsk|boolean|
|0194881f-9441-7d1b-a06a-6727543a141f|Det er godkjent at bruker kun søker deltidsarbeid|Det er godkjent at bruker kun søker deltidsarbeid|Boolsk|boolean|
|0194881f-9442-707b-a6ee-e96c06877bd8|Oppfyller kravet til heltid- og deltidsarbeid|Oppfyller kravet til heltid- og deltidsarbeid|Boolsk|boolean|
|0194881f-9442-707b-a6ee-e96c06877bd9|Kan jobbe i hele Norge|KanJobbeHvorSomHelst|Boolsk|boolean|
|0194881f-9442-707b-a6ee-e96c06877bda|Det er godkjent at bruker kun søk arbeid lokalt|Det er godkjent at bruker kun søk arbeid lokalt|Boolsk|boolean|
|0194881f-9442-707b-a6ee-e96c06877bdb|Oppfyller kravet til mobilitet|Oppfyller kravet til mobilitet|Boolsk|boolean|
|0194881f-9442-707b-a6ee-e96c06877bdc|Kan ta alle typer arbeid|HelseTilAlleTyperJobb|Boolsk|boolean|
|0194881f-9442-707b-a6ee-e96c06877bdd|Oppfyller kravet til å være arbeidsfør|Oppfyller kravet til å være arbeidsfør|Boolsk|boolean|
|0194881f-9442-707b-a6ee-e96c06877bde|Villig til å bytte yrke|VilligTilÅBytteYrke|Boolsk|boolean|
|0194881f-9442-707b-a6ee-e96c06877bdf|Oppfyller kravet til å ta ethvert arbeid|Oppfyller kravet til å ta ethvert arbeid|Boolsk|boolean|
|0194881f-9442-707b-a6ee-e96c06877be0|Registrert som arbeidssøker|RegistrertSomArbeidssøker|Boolsk|boolean|
|0194881f-9442-707b-a6ee-e96c06877be1|Registrert som arbeidssøker på søknadstidspunktet|Registrert som arbeidssøker på søknadstidspunktet|Boolsk|boolean|
|0194881f-9442-707b-a6ee-e96c06877be2|Krav til arbeidssøker|Krav til arbeidssøker|Boolsk|boolean|
|0194881f-9443-72b4-8b30-5f6cdb24d549|Opphold i Norge|OppholdINorge|Boolsk|boolean|
|0194881f-9443-72b4-8b30-5f6cdb24d54a|Oppfyller unntak for opphold i Norge|Oppfyller unntak for opphold i Norge|Boolsk|boolean|
|0194881f-9443-72b4-8b30-5f6cdb24d54b|Oppfyller kravet til opphold i Norge eller unntak|Oppfyller kravet til opphold i Norge eller unntak|Boolsk|boolean|
|0194881f-9443-72b4-8b30-5f6cdb24d54c|Er personen medlem av folketrygden|Er personen medlem av folketrygden|Boolsk|boolean|
|0194881f-9443-72b4-8b30-5f6cdb24d54d|Oppfyller kravet til medlemskap|Oppfyller kravet til medlemskap|Boolsk|boolean|
|0194881f-9443-72b4-8b30-5f6cdb24d54e|Oppfyller kravet til opphold i Norge|Oppfyller kravet til opphold i Norge|Boolsk|boolean|
|0194881f-9444-7a73-a458-0af81c034d85|Har rett til ordinære dagpenger gjennom arbeidsforhold|Ordinær|Boolsk|boolean|
|0194881f-9444-7a73-a458-0af81c034d86|Bruker er permittert|Permittert|Boolsk|boolean|
|0194881f-9444-7a73-a458-0af81c034d87|Har rett til dagpenger etter konkurs|Lønnsgaranti|Boolsk|boolean|
|0194881f-9444-7a73-a458-0af81c034d88|Har rett til dagpenger under permittering i fiskeforedlingsindustri|PermittertFiskeforedling|Boolsk|boolean|
|0194881f-9444-7a73-a458-0af81c034d89|Har rett til ordinære dagpenger uten arbeidsforhold|Har rett til ordinære dagpenger uten arbeidsforhold|Boolsk|boolean|
|0194881f-9444-7a73-a458-0af81c034d8a|Har rett til ordinære dagpenger|Har rett til ordinære dagpenger|Boolsk|boolean|
|0194881f-9444-7a73-a458-0af81c034d8b|Rettighetstype|Rettighetstype|Boolsk|boolean|
|0194881f-9445-734c-a7ee-045edf29b522|Tar utdanning eller opplæring?|TarUtdanningEllerOpplæring|Boolsk|boolean|
|0194881f-9445-734c-a7ee-045edf29b523|Godkjent unntak for utdanning eller opplæring?|Godkjent unntak for utdanning eller opplæring?|Boolsk|boolean|
|0194881f-9445-734c-a7ee-045edf29b524|Har svart ja på spørsmål om utdanning eller opplæring|Har svart ja på spørsmål om utdanning eller opplæring|Boolsk|boolean|
|0194881f-9445-734c-a7ee-045edf29b525|Har svart nei på spørsmål om utdanning eller opplæring|Har svart nei på spørsmål om utdanning eller opplæring|Boolsk|boolean|
|0194881f-9445-734c-a7ee-045edf29b526|Oppfyller kravet på unntak for utdanning eller opplæring|Oppfyller kravet på unntak for utdanning eller opplæring|Boolsk|boolean|
|0194881f-9445-734c-a7ee-045edf29b527|Deltar i arbeidsmarkedstiltak|Deltar i arbeidsmarkedstiltak|Boolsk|boolean|
|0194881f-9445-734c-a7ee-045edf29b528|Deltar i opplæring for innvandrere|Deltar i opplæring for innvandrere|Boolsk|boolean|
|0194881f-9445-734c-a7ee-045edf29b529|Deltar i grunnskoleopplæring, videregående opplæring og opplæring i grunnleggende ferdigheter|Deltar i grunnskoleopplæring, videregående opplæring og opplæring i grunnleggende ferdigheter|Boolsk|boolean|
|0194881f-9445-734c-a7ee-045edf29b52a|Deltar i høyere yrkesfaglig utdanning|Deltar i høyere yrkesfaglig utdanning|Boolsk|boolean|
|0194881f-9445-734c-a7ee-045edf29b52b|Deltar i høyere utdanning|Deltar i høyere utdanning|Boolsk|boolean|
|0194881f-9445-734c-a7ee-045edf29b52c|Deltar på kurs mv|Deltar på kurs mv|Boolsk|boolean|
|0194881f-9445-734c-a7ee-045edf29b52d|Krav til utdanning eller opplæring|Krav til utdanning eller opplæring|Boolsk|boolean|
|0194881f-9447-7e36-a569-3e9f42bff9f6|Bruker er utestengt fra dagpenger|Bruker er utestengt fra dagpenger|Boolsk|boolean|
|0194881f-9447-7e36-a569-3e9f42bff9f7|Oppfyller krav til ikke utestengt|Oppfyller krav til ikke utestengt|Boolsk|boolean|
|0194881f-9462-78af-8977-46092bb030eb|fagsakId|fagsakId|Heltall|int|
|01948d3c-4bea-7802-9d18-5342a5e2be99|Avtjent verneplikt|Verneplikt|Boolsk|boolean|
|01948d43-e218-76f1-b29b-7e604241d98a|Oppfyller kravet til verneplikt|Oppfyller kravet til verneplikt|Boolsk|boolean|
|0194929e-2036-7ac1-ada3-5cbe05a83f08|Har helsemessige begrensninger og kan ikke ta alle typer arbeid|Har helsemessige begrensninger og kan ikke ta alle typer arbeid|Boolsk|boolean|
|0194d105-bd54-7b2b-9dc6-6e6664951894|Årsaken til permitteringen er godkjent|Årsaken til permitteringen er godkjent|Boolsk|boolean|
|0194d111-db2f-7395-bcfb-959f245fd2a6|Oppfyller kravet til permittering|Oppfyller kravet til permittering|Boolsk|boolean|
|0194d119-90b7-7416-a8b0-9e9cf3587d48|Permitteringen er midlertidig driftsinnskrenkning eller driftsstans|Permitteringen er midlertidig driftsinnskrenkning eller driftsstans|Boolsk|boolean|
|0194ff86-a035-7eb0-9c60-21899f7cc0c1|Kan kravet til reell arbeidssøker vurderes|Kan kravet til reell arbeidssøker vurderes|Boolsk|boolean|
|0195042d-918e-7fae-8fb7-7f38eed42710|Periode som gis ved permittering|Periode som gis ved permittering|Heltall|int|
|019522b0-c722-76d4-8d7f-78f556c51f72|Oppfyller kravet til permittering i fiskeindustrien|Oppfyller kravet til permittering i fiskeindustrien|Boolsk|boolean|
|019522b8-0f1b-7536-8691-fd824bca86de|Årsaken til permitteringen fra fiskeindustrien er godkjent|Årsaken til permitteringen fra fiskeindustrien er godkjent|Boolsk|boolean|
|019522b8-494f-7012-898c-d202e3b90061|Permitteringen fra fiskeindustrien er midlertidig driftsinnskrenkning eller driftsstans|Permitteringen fra fiskeindustrien er midlertidig driftsinnskrenkning eller driftsstans|Boolsk|boolean|
|019522d2-9bb1-7960-b1e2-a959566e2428|Krav til prosentvis tap av arbeidstid ved permittering fra fiskeindustrien|Krav til prosentvis tap av arbeidstid ved permittering fra fiskeindustrien|Desimaltall|double|
|019522d6-846d-7173-a892-67f10016d8d2|Ordinært krav til prosentvis tap av arbeidstid|Ordinært krav til prosentvis tap av arbeidstid|Desimaltall|double|
|0195235a-599b-7b27-97a8-bc6142066a87|Periode som gis ved permittering fra fiskeindustrien|Periode som gis ved permittering fra fiskeindustrien|Heltall|int|
|019523aa-7941-7dd2-8c43-0644d7b43f57|Tre ganger dagsats|Tre ganger dagsats|Penger|Beløp|
|019523aa-980d-7805-b6ed-d701e7827998|Ingen egenandel|Ingen egenandel|Penger|Beløp|