package no.nav.dagpenger.regel.brev

import no.nav.dagpenger.brev.Brevmal
import no.nav.dagpenger.brev.Maltekst
import no.nav.dagpenger.brev.PeriodeType
import no.nav.dagpenger.brev.Plassering
import no.nav.dagpenger.brev.Trigger
import no.nav.dagpenger.regel.OpplysningsTyper.AntallBarnSomGirRettTilBarnetilleggId
import no.nav.dagpenger.regel.OpplysningsTyper.DagsatsEtterSamordningMedBarnetilleggId
import no.nav.dagpenger.regel.OpplysningsTyper.EgenandelId
import no.nav.dagpenger.regel.OpplysningsTyper.GrunnlagId
import no.nav.dagpenger.regel.OpplysningsTyper.KravTilAlderId
import no.nav.dagpenger.regel.OpplysningsTyper.KravTilArbeidssøkerId
import no.nav.dagpenger.regel.OpplysningsTyper.KravTilMinsteinntektId
import no.nav.dagpenger.regel.OpplysningsTyper.OppfyllerMeldepliktId
import no.nav.dagpenger.regel.OpplysningsTyper.OppyllerKravTilRegistrertArbeidssøkerId
import no.nav.dagpenger.regel.OpplysningsTyper.OrdinærPeriodeId
import no.nav.dagpenger.regel.OpplysningsTyper.fastsattArbeidstidPerUkeFørTapId
import no.nav.dagpenger.regel.OpplysningsTyper.harLøpendeRettId

/**
 * Felles brevmal for dagpenger.
 * Dekker innvilgelse, avslag, gjenopptak, stans og endring i én og samme mal.
 * Trigger-systemet sørger for at kun relevante tekster inkluderes i hvert brev.
 */
val DagpengerBrevmal =
    Brevmal(
        navn = "Dagpenger",
        krevInnholdI = setOf(Plassering.BEGRUNNELSE, Plassering.VILKÅR, Plassering.FASTSETTELSE),
        maltekster =
            listOf(
                // === Overskrift ===
                Maltekst(
                    trigger = Trigger.Avgjørelse("Innvilgelse"),
                    tekst = "Nav har innvilget søknaden din om dagpenger",
                    plassering = Plassering.OVERSKRIFT,
                    rekkefølge = 1,
                ),
                Maltekst(
                    trigger = Trigger.Avgjørelse("Avslag"),
                    tekst = "Nav har avslått søknaden din om dagpenger",
                    plassering = Plassering.OVERSKRIFT,
                    rekkefølge = 1,
                ),
                Maltekst(
                    trigger = Trigger.Avgjørelse("Gjenopptak"),
                    tekst = "Nav har gjenopptatt dagpengene dine",
                    plassering = Plassering.OVERSKRIFT,
                    rekkefølge = 1,
                ),
                Maltekst(
                    trigger = Trigger.Avgjørelse("Stans"),
                    tekst = "Nav har stanset dagpengene dine",
                    plassering = Plassering.OVERSKRIFT,
                    rekkefølge = 1,
                ),
                Maltekst(
                    trigger = Trigger.Avgjørelse("Endring"),
                    tekst = "Nav har endret dagpengene dine",
                    plassering = Plassering.OVERSKRIFT,
                    rekkefølge = 1,
                ),
                // === Innledning ===
                // Periodebasert innledning (innvilgelse/gjenopptak)
                Maltekst(
                    trigger =
                        Trigger.OpplysningVerdi(
                            harLøpendeRettId.uuid,
                            forventetVerdi = "true",
                            periodeType = PeriodeType.ÅPEN,
                        ),
                    tekst = "Du får dagpenger fra og med {{Har løpende rett på dagpenger.fraOgMed}}.",
                    plassering = Plassering.INNLEDNING,
                    rekkefølge = 1,
                ),
                Maltekst(
                    trigger =
                        Trigger.OpplysningVerdi(
                            harLøpendeRettId.uuid,
                            forventetVerdi = "true",
                            periodeType = PeriodeType.LUKKET,
                        ),
                    tekst =
                        "Du får dagpenger fra og med {{Har løpende rett på dagpenger.fraOgMed}} " +
                            "til og med {{Har løpende rett på dagpenger.tilOgMed}}.",
                    plassering = Plassering.INNLEDNING,
                    rekkefølge = 1,
                ),
                Maltekst(
                    trigger =
                        Trigger.OpplysningVerdi(
                            harLøpendeRettId.uuid,
                            forventetVerdi = "true",
                            periodeType = PeriodeType.FLERE,
                        ),
                    tekst =
                        "[Saksbehandler: Rettighetsperioden har flere perioder som " +
                            "ikke kan beskrives maskinelt. Vennligst beskriv periodene manuelt.]",
                    plassering = Plassering.INNLEDNING,
                    rekkefølge = 1,
                ),
                // Avslag innledning
                Maltekst(
                    trigger = Trigger.Avgjørelse("Avslag"),
                    tekst = "Vi har avslått søknaden din om dagpenger.",
                    plassering = Plassering.INNLEDNING,
                    rekkefølge = 2,
                ),
                // Stans innledning
                Maltekst(
                    trigger = Trigger.Avgjørelse("Stans"),
                    tekst = "Vi har stanset dagpengene dine.",
                    plassering = Plassering.INNLEDNING,
                    rekkefølge = 2,
                ),
                // Dagsats (vises når opplysningen finnes, typisk innvilgelse/endring)
                Maltekst(
                    trigger =
                        Trigger.OpplysningFinnes(
                            DagsatsEtterSamordningMedBarnetilleggId.uuid,
                        ),
                    tekst =
                        "Du får {{Dagsats med barnetillegg etter samordning og 90 % regel}} " +
                            "kroner dagen for fem dager i uken.",
                    plassering = Plassering.INNLEDNING,
                    rekkefølge = 3,
                ),
                // Egenandel i innledning
                Maltekst(
                    trigger = Trigger.OpplysningFinnes(EgenandelId.uuid),
                    tekst =
                        "Nav trekker en egenandel av dagpengene dine. " +
                            "Egenandelen din er {{Egenandel}} kroner, " +
                            "som er tre ganger dagsatsen din. " +
                            "Egenandelen blir trukket før du får første utbetaling.",
                    plassering = Plassering.INNLEDNING,
                    rekkefølge = 4,
                ),
                // Henvisning til mer info lenger ned
                Maltekst(
                    trigger = Trigger.Avgjørelse("Innvilgelse", "Gjenopptak"),
                    tekst =
                        "Du kan lese mer om beregning, utbetaling og egenandel lenger ned i brevet.",
                    plassering = Plassering.INNLEDNING,
                    rekkefølge = 5,
                ),
                // "Derfor får du dagpenger fra..." (bold)
                Maltekst(
                    trigger =
                        Trigger.OpplysningVerdi(
                            harLøpendeRettId.uuid,
                            forventetVerdi = "true",
                            periodeType = PeriodeType.ÅPEN,
                        ),
                    tekst =
                        "**Derfor får du dagpenger fra {{Har løpende rett på dagpenger.fraOgMed}}**",
                    plassering = Plassering.INNLEDNING,
                    rekkefølge = 6,
                ),
                Maltekst(
                    trigger =
                        Trigger.OpplysningVerdi(
                            harLøpendeRettId.uuid,
                            forventetVerdi = "true",
                            periodeType = PeriodeType.LUKKET,
                        ),
                    tekst =
                        "**Derfor får du dagpenger fra {{Har løpende rett på dagpenger.fraOgMed}}**",
                    plassering = Plassering.INNLEDNING,
                    rekkefølge = 6,
                ),
                // === Begrunnelse ===
                // Avslag-begrunnelse
                Maltekst(
                    trigger = Trigger.Avgjørelse("Avslag"),
                    tittel = "Derfor får du avslag",
                    tekst = "",
                    plassering = Plassering.BEGRUNNELSE,
                    rekkefølge = 0,
                ),
                // Stans-begrunnelse
                Maltekst(
                    trigger = Trigger.Avgjørelse("Stans"),
                    tittel = "Derfor har vi stanset dagpengene dine",
                    tekst = "",
                    plassering = Plassering.BEGRUNNELSE,
                    rekkefølge = 0,
                ),
                // Vilkår som ikke er oppfylt (avslag og stans)
                Maltekst(
                    trigger = Trigger.OpplysningVerdi(KravTilAlderId.uuid, "false"),
                    tittel = "Du oppfyller ikke kravet til alder",
                    tekst =
                        "Du oppfyller ikke alderskravet for å motta dagpenger. " +
                            "Vurderingen er gjort etter [folketrygdloven § 4-23](https://lovdata.no/lov/1997-02-28-19/%C2%A74-23).",
                    plassering = Plassering.BEGRUNNELSE,
                    rekkefølge = 1,
                ),
                Maltekst(
                    trigger = Trigger.OpplysningVerdi(KravTilMinsteinntektId.uuid, "false"),
                    tittel = "Du har hatt for lav inntekt",
                    tekst =
                        """
                        For å få dagpenger må du ha hatt arbeidsinntekt på {{Inntektskrav for siste 12 måneder}} kroner de siste 12 månedene, eller {{Inntektskrav for siste 36 måneder}} kroner de siste 36 månedene.

                        - Den registrerte arbeidsinntekten din de siste 12 månedene til og med {{Siste avsluttende kalendermåned}} er {{Arbeidsinntekt siste 12 måneder}} kroner.
                        - Den registrerte arbeidsinntekten din de siste 36 månedene til og med {{Siste avsluttende kalendermåned}} er {{Arbeidsinntekt siste 36 måneder}} kroner.
                        
                        Du kan se hvilke inntekter som gir rett til dagpenger på [nav.no/dagpenger](https://nav.no/dagpenger). Vi har hentet arbeidsinntektene dine fra Skatteetaten. Du kan sjekke dem på [skatteetaten.no/mineinntekter](https://skatteetaten.no/mineinntekter).

                        Hvis opplysningene ikke stemmer, må du:

                        - Kontakte arbeidsgiveren din, slik at de kan rette inntektsopplysningene dine.
                        - Ta kontakt med Nav og dokumentere endringene.

                        Vedtaket er gjort etter [folketrygdloven § 4-4](https://lovdata.no/lov/1997-02-28-19/%C2%A74-4).
                        """.trimIndent(),
                    plassering = Plassering.BEGRUNNELSE,
                    rekkefølge = 2,
                ),
                Maltekst(
                    trigger = Trigger.OpplysningVerdi(KravTilArbeidssøkerId.uuid, "false"),
                    tittel = "Du må være reell arbeidssøker",
                    tekst =
                        "For å ha rett til dagpenger, må du være villig til å ta alle typer arbeid med vanlig lønn. " +
                            "Vedtaket er gjort etter [folketrygdloven § 4-5](https://lovdata.no/lov/1997-02-28-19/%C2%A74-5).",
                    plassering = Plassering.BEGRUNNELSE,
                    rekkefølge = 3,
                ),
                Maltekst(
                    trigger =
                        Trigger.OpplysningVerdi(
                            OppyllerKravTilRegistrertArbeidssøkerId.uuid,
                            "false",
                        ),
                    tittel = "Du er ikke lenger registrert som arbeidssøker",
                    tekst =
                        "For å ha rett til dagpenger må du være registrert som arbeidssøker hos Nav. " +
                            "Vi har fått melding om at du ikke lenger er registrert som arbeidssøker, " +
                            "og dagpengene dine er derfor stanset. " +
                            "Vedtaket er gjort etter [folketrygdloven § 4-8](https://lovdata.no/lov/1997-02-28-19/%C2%A74-8).",
                    plassering = Plassering.BEGRUNNELSE,
                    rekkefølge = 4,
                ),
                Maltekst(
                    trigger =
                        Trigger.OpplysningVerdi(
                            OppfyllerMeldepliktId.uuid,
                            "false",
                        ),
                    tittel = "Du har ikke oppfylt meldeplikten",
                    tekst =
                        "For å ha rett til dagpenger må du sende meldekort innen fristen. " +
                            "Vi har ikke mottatt meldekort fra deg innen fristen, " +
                            "og dagpengene dine er derfor stanset. " +
                            "Vedtaket er gjort etter [folketrygdloven § 4-8](https://lovdata.no/lov/1997-02-28-19/%C2%A74-8).",
                    plassering = Plassering.BEGRUNNELSE,
                    rekkefølge = 5,
                ),
                // === Vilkår (vises ikke ved innvilgelse — implisitt oppfylt) ===
                // === Fastsettelser ===
                // Periode
                Maltekst(
                    trigger = Trigger.OpplysningFinnes(OrdinærPeriodeId.uuid),
                    tittel = "Hvor lenge kan du få dagpenger?",
                    tekst =
                        "Du er innvilget dagpenger til og med " +
                            "{{Har løpende rett på dagpenger.tilOgMed}}, " +
                            "fordi du ikke lenger har rett til dagpenger etter denne datoen.\n\n" +
                            "Arbeidsinntekten din gir deg rett til en periode på maksimalt " +
                            "{{Antall stønadsuker (stønadsperiode)}} uker med dagpenger. " +
                            "Vurderingen er gjort etter [folketrygdloven § 4-15](https://lovdata.no/lov/1997-02-28-19/%C2%A74-15).\n\n" +
                            "Hvis dagpengene dine stanser før perioden er over, kan du søke på nytt. " +
                            "Du kan lese mer om grunner til stans lenger ned i brevet.",
                    plassering = Plassering.FASTSETTELSE,
                    rekkefølge = 1,
                ),
                // Sats og grunnlag
                Maltekst(
                    trigger =
                        Trigger.OpplysningFinnes(
                            DagsatsEtterSamordningMedBarnetilleggId.uuid,
                        ),
                    tittel = "Slik har vi beregnet dagpengene dine",
                    tekst =
                        "Du får {{Dagsats med barnetillegg etter samordning og 90 % regel}} " +
                            "kroner per dag for fem dager i uken.",
                    plassering = Plassering.FASTSETTELSE,
                    rekkefølge = 2,
                ),
                // Barnetillegg (vises kun når det finnes barn som gir tillegg)
                Maltekst(
                    trigger =
                        Trigger.OpplysningFinnes(
                            AntallBarnSomGirRettTilBarnetilleggId.uuid,
                        ),
                    tekst =
                        "Dette inkluderer barnetillegg for " +
                            "{{Antall barn som gir rett til barnetillegg}} barn, " +
                            "som er {{Barnetilleggets størrelse i kroner per dag for hvert barn}} " +
                            "kroner per dag.",
                    plassering = Plassering.FASTSETTELSE,
                    rekkefølge = 3,
                ),
                // Beregningsforklaring med inntekter
                Maltekst(
                    trigger =
                        Trigger.OpplysningFinnes(
                            GrunnlagId.uuid,
                        ),
                    tekst =
                        """
                        Vi beregner hvor mye du kan få i dagpenger basert på hva du har hatt i inntekt de siste 12 månedene, eller i gjennomsnitt de siste 36 månedene. Vi velger det alternativet som er best for deg. For deg har vi valgt {{Brukt beregningsregel}}.

                        Vi har registrert disse inntektene:

                        - {{Siste avsluttende kalendermåned | månedÅr(-11)}} - {{Siste avsluttende kalendermåned | månedÅr(0)}}: {{Inntektperiode 1}} kroner
                        - {{Siste avsluttende kalendermåned | månedÅr(-23)}} - {{Siste avsluttende kalendermåned | månedÅr(-12)}}: {{Inntektperiode 2}} kroner
                        - {{Siste avsluttende kalendermåned | månedÅr(-35)}} - {{Siste avsluttende kalendermåned | månedÅr(-24)}}: {{Inntektperiode 3}} kroner

                        Når vi beregner dagpengene dine, setter vi opp inntekten din tilsvarende årlig justering av grunnbeløpet (G). Inntekt over {{6 ganger grunnbeløp}} kroner ({{Faktor for maksimalt mulig grunnlag}} G) per tolvmånedersperiode vil ikke bli regnet med. Inntektsgrunnlaget ditt er beregnet til {{Dagpengegrunnlag}} kroner.
                        """.trimIndent(),
                    plassering = Plassering.FASTSETTELSE,
                    rekkefølge = 4,
                ),
                // Info om inntektssjekk
                Maltekst(
                    trigger =
                        Trigger.OpplysningFinnes(
                            GrunnlagId.uuid,
                        ),
                    tekst =
                        "Du kan se hva som gir rett til dagpenger på [nav.no/dagpenger](https://nav.no/dagpenger). " +
                            "Vi har hentet inntektene dine fra Skatteetaten. " +
                            "Du kan sjekke inntekten din på [skatteetaten.no/mineinntekter](https://skatteetaten.no/mineinntekter).\n\n" +
                            "Hvis opplysningene ikke stemmer, må du:\n\n" +
                            "- Kontakte arbeidsgiveren din, slik at de kan rette " +
                            "inntektsopplysningene dine.\n" +
                            "- Ta kontakt med Nav og dokumentere endringene.\n\n" +
                            "Beregningen er gjort etter [folketrygdloven § 4-11 andre ledd](https://lovdata.no/lov/1997-02-28-19/%C2%A74-11).",
                    plassering = Plassering.FASTSETTELSE,
                    rekkefølge = 5,
                ),
                // Arbeidstid
                Maltekst(
                    trigger =
                        Trigger.OpplysningFinnes(
                            fastsattArbeidstidPerUkeFørTapId.uuid,
                        ),
                    tittel = "Arbeidstiden din",
                    tekst =
                        "Vi har kommet frem til at den vanlige arbeidstiden din er " +
                            "{{Fastsatt arbeidstid per uke før tap}} timer per uke.",
                    plassering = Plassering.FASTSETTELSE,
                    rekkefølge = 6,
                ),
                // Egenandel
                Maltekst(
                    trigger = Trigger.OpplysningFinnes(EgenandelId.uuid),
                    tittel = "Egenandel",
                    tekst =
                        "Når du får dagpenger, trekker Nav en egenandel fra den første " +
                            "utbetalingen din. Den tilsvarer tre ganger dagsatsen med " +
                            "eventuelt barnetillegg.\n\n" +
                            "Egenandelen din er {{Egenandel}} kroner.\n\n" +
                            "Vi trekker egenandelen fra den første utbetalingen din. " +
                            "Får vi ikke trukket hele egenandelen fra den første utbetalingen, " +
                            "trekker vi resten fra den neste utbetalingen din.\n\n" +
                            "Egenandelen trekkes automatisk. I utbetalingsoversikten vil " +
                            "dagsatsen din se lavere ut i perioder hvor egenandel er trukket.\n\n" +
                            "Les mer om egenandel i [folketrygdloven § 4-9](https://lovdata.no/lov/1997-02-28-19/%C2%A74-9).",
                    plassering = Plassering.FASTSETTELSE,
                    rekkefølge = 7,
                ),
                // === Informasjon ===
                Maltekst(
                    trigger = Trigger.Avgjørelse("Innvilgelse", "Gjenopptak"),
                    tittel = "Du må sende meldekort",
                    tekst =
                        "For å ha rett på dagpenger må du sende meldekort hver 14. dag. " +
                            "Du fyller ut meldekortet digitalt på [nav.no/meldekort](https://nav.no/meldekort). " +
                            "Logg inn på [nav.no](https://nav.no) for å se når du skal sende neste meldekort.\n\n" +
                            "Hvis du sender meldekortet etter fristen, får du trekk i utbetalingen for neste meldekort. " +
                            "Hvor mye du blir trukket avhenger av hvor mange dager for sent meldekortet ble sendt. " +
                            "Du kan klage på trekk i utbetalingen. Klagefristen er seks uker.\n\n" +
                            "Har det gått mer enn 20 dager siden siste gang du sendte et meldekort, " +
                            "blir du tatt ut av arbeidssøkerregisteret hos Nav, og dagpengene dine stanser. " +
                            "Da må du registrere deg som arbeidssøker på nytt og sende inn ny søknad om dagpenger.\n\n" +
                            "Les mer om meldekort og hva som skal føres på [nav.no/send-meldekort-dagpenger](https://nav.no/send-meldekort-dagpenger).",
                    plassering = Plassering.INFORMASJON,
                    rekkefølge = 1,
                ),
                Maltekst(
                    trigger = Trigger.Avgjørelse("Innvilgelse", "Gjenopptak"),
                    tittel = "Utbetaling",
                    tekst =
                        "Har du registrert en bankkonto hos Nav eller Skatteetaten, vil du få utbetalingen på den kontoen. " +
                            "Pengene utbetales vanligvis innen to til tre dager etter at meldekortet ditt er registrert hos Nav. " +
                            "Du kan se alle utbetalingene du har fått ved å logge inn på [nav.no](https://nav.no).",
                    plassering = Plassering.INFORMASJON,
                    rekkefølge = 2,
                ),
                Maltekst(
                    trigger = Trigger.Avgjørelse("Innvilgelse", "Gjenopptak"),
                    tittel = "Husk å sjekke skattekortet ditt",
                    tekst =
                        "Du må betale skatt av dagpengene du får fra Nav. " +
                            "Det er lurt å endre skattekortet ditt når du får mindre utbetalt i måneden. " +
                            "Hvis du både jobber og mottar dagpenger, og trekkes etter tabellkort på dagpengene, " +
                            "kan det føre til at vi trekker for lite skatt. " +
                            "Du bør derfor informere Nav om at du ønsker prosenttrekk av dagpengene. " +
                            "Du kan endre skattekortet ditt på [skatteetaten.no](https://skatteetaten.no).\n\n" +
                            "Les mer om skattetrekk på [nav.no/skattetrekk](https://nav.no/skattetrekk).",
                    plassering = Plassering.INFORMASJON,
                    rekkefølge = 3,
                ),
                Maltekst(
                    trigger = Trigger.Avgjørelse("Innvilgelse", "Gjenopptak"),
                    tittel = "Vi stanser dagpengene dine automatisk når du:",
                    tekst =
                        "- ikke sender meldekort etter at du registrerte deg som arbeidsledig\n" +
                            "- har jobbet mer enn 50 prosent av den vanlige arbeidstiden din på tre meldekort " +
                            "(60 prosent om du er permittert fra fiskeindustrien)\n" +
                            "- slutter å sende meldekort og det er mer enn 20 dager siden du sist sendte meldekort\n" +
                            "- svarer nei på spørsmålet på meldekortet om du fortsatt ønsker å være registrert som arbeidssøker\n" +
                            "- er ferdig med perioden du får dagpenger\n" +
                            "- er ferdig med perioden du får dagpenger som permittert\n" +
                            "- har fylt 67 år",
                    plassering = Plassering.INFORMASJON,
                    rekkefølge = 4,
                ),
                Maltekst(
                    trigger = Trigger.Avgjørelse("Innvilgelse", "Gjenopptak", "Stans", "Endring"),
                    tittel = "Du må melde fra om endringer",
                    tekst =
                        "Hvis det skjer en endring i situasjonen din, kan det påvirke dagpengene dine. " +
                            "Derfor er det din plikt å gi beskjed til Nav om endringen, " +
                            "slik at du ikke får for mye eller for lite i dagpenger. " +
                            "Ta kontakt med Nav på [nav.no/send-beskjed](https://nav.no/send-beskjed), eller på telefon 55 55 33 33.\n\n" +
                            "Du må gi beskjed til oss hvis du:\n\n" +
                            "- begynner eller slutter i arbeid, helt eller delvis\n" +
                            "- er permittert og har arbeidet for permitterende arbeidsgiver i mer enn seks uker\n" +
                            "- blir oppsagt mens du er permittert\n" +
                            "- blir sykmeldt, får endret sykmelding eller blir friskmeldt\n" +
                            "- begynner eller slutter på tiltak\n" +
                            "- begynner eller slutter på kurs eller annen utdanning\n" +
                            "- skal avvikle ferie eller permisjon\n" +
                            "- sitter i varetekt, soner straff, har omvendt voldsalarm eller er under forvaring\n" +
                            "- endrer adresse - dette gjør du på " +
                            "[skatteetaten.no/folkeregisteret](https://skatteetaten.no/folkeregisteret)\n" +
                            "- blir innlagt på sykehus eller institusjon\n" +
                            "- skal reise eller flytte til utlandet\n" +
                            "- mottar pensjon eller annen stønad\n" +
                            "- mottar barnetillegg og barnet skal oppholde seg utenfor EØS, Sveits eller Storbritannia\n" +
                            "- mottar barnetillegg og du får ansvar for flere/færre barn\n" +
                            "- ikke ønsker arbeidstilbud i en periode\n" +
                            "- har andre opplysninger som kan bety noe for retten til ytelser\n\n" +
                            "Du kan lese mer om opplysningsplikten i [folketrygdloven § 21-3](https://lovdata.no/lov/1997-02-28-19/%C2%A721-3).",
                    plassering = Plassering.INFORMASJON,
                    rekkefølge = 5,
                ),
                Maltekst(
                    trigger = Trigger.Avgjørelse("Innvilgelse", "Gjenopptak", "Stans", "Endring"),
                    tittel = "Du må gi oss riktige opplysninger",
                    tekst =
                        "Hvis du gir oss opplysninger som ikke er riktige eller mangelfulle, " +
                            "kan du få et krav om å betale tilbake dagpengene dine. " +
                            "Du kan også miste retten til dagpenger i inntil 26 uker.",
                    plassering = Plassering.INFORMASJON,
                    rekkefølge = 6,
                ),
                Maltekst(
                    trigger = Trigger.Avgjørelse("Stans"),
                    tittel = "Slik kan du få dagpenger igjen",
                    tekst =
                        "Hvis du ønsker å få dagpenger igjen, må du registrere deg som arbeidssøker på [nav.no](https://nav.no). " +
                            "Du må deretter søke om dagpenger på nytt.",
                    plassering = Plassering.INFORMASJON,
                    rekkefølge = 7,
                ),
                // === Avslutning ===
                Maltekst(
                    trigger = Trigger.Alltid,
                    tittel = "Du har rett til innsyn",
                    tekst =
                        "Kontakt oss om du vil se dokumentene i saken din. " +
                            "Ta kontakt på [nav.no/kontakt](https://nav.no/kontakt) eller på telefon 55 55 33 33. " +
                            "Du kan lese mer om innsynsretten på [nav.no/personvernerklaering](https://nav.no/personvernerklaering).",
                    plassering = Plassering.AVSLUTNING,
                    rekkefølge = 1,
                ),
                Maltekst(
                    trigger = Trigger.Alltid,
                    tittel = "Du har rettigheter knyttet til personopplysningene dine",
                    tekst =
                        "Du finner informasjon om hvordan Nav behandler personopplysningene dine, " +
                            "og hvilke rettigheter du har, på [nav.no/personvernerklaering](https://nav.no/personvernerklaering).\n\n" +
                            "Nav kan veilede deg på telefon 55 55 33 33 om hvordan Nav behandler personopplysninger.",
                    plassering = Plassering.AVSLUTNING,
                    rekkefølge = 2,
                ),
                Maltekst(
                    trigger = Trigger.Alltid,
                    tittel = "Du har rett til å få hjelp fra andre",
                    tekst =
                        "Du kan be om hjelp fra andre under hele saksbehandlingen, " +
                            "for eksempel fra en advokat, rettshjelper, en organisasjon du er medlem av, " +
                            "eller en myndig person over 18 år. " +
                            "Dette følger av [forvaltningsloven § 12](https://lovdata.no/lov/1967-02-10/%C2%A712). " +
                            "Hvis den som hjelper deg ikke er advokat, må du gi denne personen skriftlig fullmakt. " +
                            "Bruk skjemaet du finner på [nav.no/fullmakt](https://nav.no/fullmakt). " +
                            "Ta kontakt på telefon 55 55 33 33 hvis du ikke kan bruke det digitale skjemaet.",
                    plassering = Plassering.AVSLUTNING,
                    rekkefølge = 3,
                ),
                Maltekst(
                    trigger = Trigger.Alltid,
                    tittel = "Du har rett til å få veiledning fra Nav",
                    tekst =
                        "Vi har plikt til å veilede deg om dine rettigheter og plikter i saken din, " +
                            "både før, under og etter saksbehandlingen. " +
                            "Dette følger av [forvaltningsloven § 11](https://lovdata.no/lov/1967-02-10/%C2%A711). " +
                            "Ta kontakt på telefon 55 55 33 33 eller på " +
                            "[nav.no/kontaktoss](https://nav.no/kontaktoss) hvis du har spørsmål.",
                    plassering = Plassering.AVSLUTNING,
                    rekkefølge = 4,
                ),
                Maltekst(
                    trigger = Trigger.Alltid,
                    tittel = "Du har rett til å klage",
                    tekst =
                        "Hvis du mener vedtaket er feil, kan du klage innen seks uker fra den datoen vedtaket har kommet fram til deg. " +
                            "Klagen må være skriftlig. Du finner skjema og informasjon på [nav.no/klage](https://nav.no/klage).\n\n" +
                            "Nav kan veilede deg på telefon om hvordan du sender en klage. " +
                            "Nav-kontoret ditt kan også hjelpe deg med å skrive en klage. " +
                            "Kontakt oss på telefon 55 55 33 33 hvis du trenger hjelp.\n\n" +
                            "Hvis du får medhold i klagen, kan du få dekket vesentlige utgifter som har vært nødvendige " +
                            "for å få endret vedtaket, for eksempel hjelp fra advokat. " +
                            "Du kan ha krav på fri rettshjelp etter [rettshjelploven](https://lovdata.no/lov/2024-11-22-79). " +
                            "Du kan få mer informasjon om denne ordningen hos advokater, statsforvalteren eller Nav.\n\n" +
                            "Du kan lese om saksomkostninger i [forvaltningsloven § 36](https://lovdata.no/lov/1967-02-10/%C2%A736).\n\n" +
                            "Hvis du sender klage i posten, må du signere klagen.\n\n" +
                            "Mer informasjon om klagerettigheter finner du på " +
                            "[nav.no/klagerettigheter](https://nav.no/klagerettigheter).\n\n" +
                            "Hvis du får medhold i klagen din, har du kun rett til dagpenger for de periodene du har vært registrert som arbeidssøker.",
                    plassering = Plassering.AVSLUTNING,
                    rekkefølge = 5,
                ),
                Maltekst(
                    trigger = Trigger.Alltid,
                    tittel = "Har du spørsmål?",
                    tekst =
                        "Du finner mer informasjon på [nav.no/dagpenger](https://nav.no/dagpenger). " +
                            "På [nav.no/kontakt](https://nav.no/kontakt) kan du chatte eller skrive til oss. " +
                            "Hvis du ikke finner svar på [nav.no](https://nav.no) kan du ringe oss på telefon 55 55 33 33, hverdager 09.00-15.00.",
                    plassering = Plassering.AVSLUTNING,
                    rekkefølge = 6,
                ),
            ),
    )
