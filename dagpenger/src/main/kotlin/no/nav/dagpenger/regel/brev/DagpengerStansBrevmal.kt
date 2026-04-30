package no.nav.dagpenger.regel.brev

import no.nav.dagpenger.brev.Brevmal
import no.nav.dagpenger.brev.Maltekst
import no.nav.dagpenger.brev.Plassering
import no.nav.dagpenger.brev.Trigger
import no.nav.dagpenger.regel.OpplysningsTyper.OppfyllerMeldepliktId
import no.nav.dagpenger.regel.OpplysningsTyper.OppyllerKravTilRegistrertArbeidssøkerId

/**
 * Brevmal for stans av dagpenger.
 * Brukes når en løpende rett stanses (f.eks. avregistrert som arbeidssøker, meldeplikt brutt).
 */
val DagpengerStansBrevmal =
    Brevmal(
        navn = "Dagpenger - stans",
        maltekster =
            listOf(
                // === Overskrift ===
                Maltekst(
                    trigger = Trigger.Avgjørelse("Stans"),
                    tekst = "Nav har stanset dagpengene dine",
                    plassering = Plassering.OVERSKRIFT,
                    rekkefølge = 1,
                ),
                // === Innledning ===
                Maltekst(
                    trigger = Trigger.Avgjørelse("Stans"),
                    tekst = "Vi har stanset dagpengene dine.",
                    plassering = Plassering.INNLEDNING,
                    rekkefølge = 1,
                ),
                // === Begrunnelse ===
                Maltekst(
                    trigger = Trigger.Avgjørelse("Stans"),
                    tittel = "Derfor har vi stanset dagpengene dine",
                    tekst = "",
                    plassering = Plassering.BEGRUNNELSE,
                    rekkefølge = 0,
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
                    rekkefølge = 1,
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
                    rekkefølge = 2,
                ),
                // === Informasjon ===
                Maltekst(
                    trigger = Trigger.Alltid,
                    tittel = "Slik kan du få dagpenger igjen",
                    tekst =
                        "Hvis du ønsker å få dagpenger igjen, må du registrere deg som arbeidssøker på nav.no. " +
                            "Du må deretter søke om dagpenger på nytt.",
                    plassering = Plassering.INFORMASJON,
                    rekkefølge = 1,
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
