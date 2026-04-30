package no.nav.dagpenger.regel.brev

import no.nav.dagpenger.brev.Brevmal
import no.nav.dagpenger.brev.Maltekst
import no.nav.dagpenger.brev.PeriodeType
import no.nav.dagpenger.brev.Plassering
import no.nav.dagpenger.brev.Trigger
import no.nav.dagpenger.regel.OpplysningsTyper.DagsatsEtterSamordningMedBarnetilleggId
import no.nav.dagpenger.regel.OpplysningsTyper.EgenandelId
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
                    trigger = Trigger.OpplysningFinnes(harLøpendeRettId.uuid, periodeType = PeriodeType.ÅPEN),
                    tekst = "Du får dagpenger fra og med {{Har løpende rett på dagpenger.fraOgMed}}.",
                    plassering = Plassering.INNLEDNING,
                    rekkefølge = 1,
                ),
                Maltekst(
                    trigger = Trigger.OpplysningFinnes(harLøpendeRettId.uuid, periodeType = PeriodeType.LUKKET),
                    tekst =
                        "Du får dagpenger fra og med {{Har løpende rett på dagpenger.fraOgMed}} " +
                            "til og med {{Har løpende rett på dagpenger.tilOgMed}}.",
                    plassering = Plassering.INNLEDNING,
                    rekkefølge = 1,
                ),
                Maltekst(
                    trigger = Trigger.OpplysningFinnes(harLøpendeRettId.uuid, periodeType = PeriodeType.FLERE),
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
                // Dagsats og egenandel (vises når opplysningene finnes, typisk innvilgelse/endring)
                Maltekst(
                    trigger = Trigger.OpplysningFinnes(DagsatsEtterSamordningMedBarnetilleggId.uuid),
                    tekst = "Du får {{Dagsats med barnetillegg etter samordning og 90 % regel}} kroner dagen for fem dager i uken.",
                    plassering = Plassering.INNLEDNING,
                    rekkefølge = 3,
                ),
                Maltekst(
                    trigger = Trigger.OpplysningFinnes(EgenandelId.uuid),
                    tekst =
                        "Nav trekker en egenandel av dagpengene dine. " +
                            "Egenandelen din er {{Egenandel}} kroner.",
                    plassering = Plassering.INNLEDNING,
                    rekkefølge = 4,
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
                    trigger = Trigger.OpplysningVerdi(KravTilAlderId.uuid, "false", kunNyeOpplysninger = true),
                    tittel = "Du oppfyller ikke kravet til alder",
                    tekst =
                        "Du oppfyller ikke alderskravet for å motta dagpenger. " +
                            "Vurderingen er gjort etter folketrygdloven § 4-23.",
                    plassering = Plassering.BEGRUNNELSE,
                    rekkefølge = 1,
                ),
                Maltekst(
                    trigger = Trigger.OpplysningVerdi(KravTilMinsteinntektId.uuid, "false", kunNyeOpplysninger = true),
                    tittel = "Du oppfyller ikke kravet til minsteinntekt",
                    tekst =
                        "Du har ikke hatt tilstrekkelig arbeidsinntekt. " +
                            "Vurderingen er gjort etter folketrygdloven § 4-4.",
                    plassering = Plassering.BEGRUNNELSE,
                    rekkefølge = 2,
                ),
                Maltekst(
                    trigger = Trigger.OpplysningVerdi(KravTilArbeidssøkerId.uuid, "false", kunNyeOpplysninger = true),
                    tittel = "Du må være reell arbeidssøker",
                    tekst =
                        "For å ha rett til dagpenger, må du være villig til å ta alle typer arbeid med vanlig lønn. " +
                            "Vedtaket er gjort etter folketrygdloven § 4-5.",
                    plassering = Plassering.BEGRUNNELSE,
                    rekkefølge = 3,
                ),
                Maltekst(
                    trigger =
                        Trigger.OpplysningVerdi(
                            OppyllerKravTilRegistrertArbeidssøkerId.uuid,
                            "false",
                            kunNyeOpplysninger = true,
                        ),
                    tittel = "Du er ikke lenger registrert som arbeidssøker",
                    tekst =
                        "For å ha rett til dagpenger må du være registrert som arbeidssøker hos Nav. " +
                            "Vi har fått melding om at du ikke lenger er registrert som arbeidssøker, " +
                            "og dagpengene dine er derfor stanset. " +
                            "Vedtaket er gjort etter folketrygdloven § 4-8.",
                    plassering = Plassering.BEGRUNNELSE,
                    rekkefølge = 4,
                ),
                Maltekst(
                    trigger =
                        Trigger.OpplysningVerdi(
                            OppfyllerMeldepliktId.uuid,
                            "false",
                            kunNyeOpplysninger = true,
                        ),
                    tittel = "Du har ikke oppfylt meldeplikten",
                    tekst =
                        "For å ha rett til dagpenger må du sende meldekort innen fristen. " +
                            "Vi har ikke mottatt meldekort fra deg innen fristen, " +
                            "og dagpengene dine er derfor stanset. " +
                            "Vedtaket er gjort etter folketrygdloven § 4-8.",
                    plassering = Plassering.BEGRUNNELSE,
                    rekkefølge = 5,
                ),
                // === Vilkår (innvilgelse) ===
                Maltekst(
                    trigger = Trigger.OpplysningVerdi(KravTilAlderId.uuid, "true", kunNyeOpplysninger = true),
                    tittel = "Du oppfyller kravet til alder",
                    tekst = "Vurderingen er gjort etter folketrygdloven § 4-23.",
                    plassering = Plassering.VILKÅR,
                    rekkefølge = 1,
                ),
                Maltekst(
                    trigger = Trigger.OpplysningVerdi(KravTilMinsteinntektId.uuid, "true", kunNyeOpplysninger = true),
                    tittel = "Du oppfyller kravet til minsteinntekt",
                    tekst = "Vurderingen er gjort etter folketrygdloven § 4-4.",
                    plassering = Plassering.VILKÅR,
                    rekkefølge = 2,
                ),
                Maltekst(
                    trigger = Trigger.OpplysningVerdi(KravTilArbeidssøkerId.uuid, "true", kunNyeOpplysninger = true),
                    tittel = "Du er reell arbeidssøker",
                    tekst = "Vedtaket er gjort etter folketrygdloven § 4-5.",
                    plassering = Plassering.VILKÅR,
                    rekkefølge = 3,
                ),
                // === Fastsettelser ===
                Maltekst(
                    trigger = Trigger.OpplysningFinnes(OrdinærPeriodeId.uuid),
                    tittel = "Hvor lenge kan du få dagpenger?",
                    tekst =
                        "Arbeidsinntekten din gir deg rett til en periode på maksimalt " +
                            "{{Antall stønadsuker (stønadsperiode)}} uker med dagpenger. " +
                            "Vurderingen er gjort etter folketrygdloven § 4-15.",
                    plassering = Plassering.FASTSETTELSE,
                    rekkefølge = 1,
                ),
                Maltekst(
                    trigger = Trigger.OpplysningFinnes(DagsatsEtterSamordningMedBarnetilleggId.uuid),
                    tittel = "Slik har vi beregnet dagpengene dine",
                    tekst =
                        "Du får {{Dagsats med barnetillegg etter samordning og 90 % regel}} kroner per dag for fem dager i uken. " +
                            "Inntektsgrunnlaget ditt er beregnet til {{Dagpengegrunnlag}} kroner. " +
                            "Beregningen er gjort etter folketrygdloven § 4-11 andre ledd.",
                    plassering = Plassering.FASTSETTELSE,
                    rekkefølge = 2,
                ),
                Maltekst(
                    trigger = Trigger.OpplysningFinnes(fastsattArbeidstidPerUkeFørTapId.uuid),
                    tittel = "Arbeidstiden din",
                    tekst =
                        "Vi har kommet frem til at den vanlige arbeidstiden din er " +
                            "{{Fastsatt arbeidstid per uke før tap}} timer per uke.",
                    plassering = Plassering.FASTSETTELSE,
                    rekkefølge = 3,
                ),
                Maltekst(
                    trigger = Trigger.OpplysningFinnes(EgenandelId.uuid),
                    tittel = "Egenandel",
                    tekst =
                        "Egenandelen din er {{Egenandel}} kroner. " +
                            "Vi trekker egenandelen fra den første utbetalingen din. " +
                            "Les mer om egenandel i folketrygdloven § 4-9.",
                    plassering = Plassering.FASTSETTELSE,
                    rekkefølge = 4,
                ),
                // === Informasjon ===
                Maltekst(
                    trigger = Trigger.Avgjørelse("Innvilgelse"),
                    tittel = "Du må sende meldekort",
                    tekst =
                        "For å ha rett på dagpenger må du sende meldekort hver 14. dag. " +
                            "Du fyller ut meldekortet digitalt på nav.no/meldekort.",
                    plassering = Plassering.INFORMASJON,
                    rekkefølge = 1,
                ),
                Maltekst(
                    trigger = Trigger.Avgjørelse("Gjenopptak"),
                    tittel = "Du må sende meldekort",
                    tekst =
                        "For å ha rett på dagpenger må du sende meldekort hver 14. dag. " +
                            "Du fyller ut meldekortet digitalt på nav.no/meldekort.",
                    plassering = Plassering.INFORMASJON,
                    rekkefølge = 1,
                ),
                Maltekst(
                    trigger = Trigger.Avgjørelse("Stans"),
                    tittel = "Slik kan du få dagpenger igjen",
                    tekst =
                        "Hvis du ønsker å få dagpenger igjen, må du registrere deg som arbeidssøker på nav.no. " +
                            "Du må deretter søke om dagpenger på nytt.",
                    plassering = Plassering.INFORMASJON,
                    rekkefølge = 1,
                ),
                Maltekst(
                    trigger = Trigger.Alltid,
                    tittel = "Du må melde fra om endringer",
                    tekst =
                        "Hvis det skjer en endring i situasjonen din, kan det påvirke dagpengene dine. " +
                            "Du kan lese mer om opplysningsplikten i folketrygdloven § 21-3.",
                    plassering = Plassering.INFORMASJON,
                    rekkefølge = 2,
                ),
                // === Avslutning ===
                Maltekst(
                    trigger = Trigger.Alltid,
                    tittel = "Du har rett til innsyn",
                    tekst =
                        "Kontakt oss om du vil se dokumentene i saken din. " +
                            "Ta kontakt på nav.no/kontakt eller på telefon 55 55 33 33.",
                    plassering = Plassering.AVSLUTNING,
                    rekkefølge = 1,
                ),
                Maltekst(
                    trigger = Trigger.Alltid,
                    tittel = "Du har rett til å få hjelp fra andre",
                    tekst =
                        "Du kan be om hjelp fra andre under hele saksbehandlingen. " +
                            "Dette følger av forvaltningsloven § 12.",
                    plassering = Plassering.AVSLUTNING,
                    rekkefølge = 2,
                ),
            ),
    )
