package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningsformål.Bruker
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.aldriSynlig
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.alle
import no.nav.dagpenger.opplysning.regel.enAv
import no.nav.dagpenger.opplysning.regel.erSann
import no.nav.dagpenger.opplysning.regel.erUsann
import no.nav.dagpenger.opplysning.regel.innhentMed
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.regel.Behov.TarUtdanningEllerOpplæring
import no.nav.dagpenger.regel.OpplysningsTyper.deltarIArbeidsmarkedstiltakId
import no.nav.dagpenger.regel.OpplysningsTyper.deltarIHøyereUtdanningId
import no.nav.dagpenger.regel.OpplysningsTyper.deltarIHøyereYrkesfagligUtdanningId
import no.nav.dagpenger.regel.OpplysningsTyper.deltarIOpplæringForInnvandrereId
import no.nav.dagpenger.regel.OpplysningsTyper.deltarPåKursMvId
import no.nav.dagpenger.regel.OpplysningsTyper.godkjentUnntakForUtdanningId
import no.nav.dagpenger.regel.OpplysningsTyper.grunnskoleopplæringId
import no.nav.dagpenger.regel.OpplysningsTyper.kravTilUtdanningEllerOpplæringId
import no.nav.dagpenger.regel.OpplysningsTyper.oppfyllerKravetPåUnntakForUtdanningId
import no.nav.dagpenger.regel.OpplysningsTyper.svartJaPåUtdanningId
import no.nav.dagpenger.regel.OpplysningsTyper.svartNeiPåUtdanningId
import no.nav.dagpenger.regel.OpplysningsTyper.tarUtdanningEllerOpplæringId
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.Søknadstidspunkt.søknadIdOpplysningstype

object Utdanning {
    private val hvisTarUtdanning: (LesbarOpplysninger) -> Boolean = { it.erSann(tarUtdanning) }

    // § 4-6 første og andre avsnitt, Utdanning
    val tarUtdanning =
        Opplysningstype.boolsk(
            tarUtdanningEllerOpplæringId,
            "Tar utdanning eller opplæring?",
            Bruker,
            behovId = TarUtdanningEllerOpplæring,
        )
    internal val godkjentUnntakForUtdanning =
        Opplysningstype.boolsk(
            godkjentUnntakForUtdanningId,
            "Godkjent unntak for utdanning eller opplæring?",
        )
    private val svartJaPåUtdanning =
        Opplysningstype.boolsk(
            svartJaPåUtdanningId,
            "Har svart ja på spørsmål om utdanning eller opplæring",
            synlig = aldriSynlig,
        )
    private val svartNeiPåUtdanning =
        Opplysningstype.boolsk(
            svartNeiPåUtdanningId,
            "Har svart nei på spørsmål om utdanning eller opplæring",
            synlig = aldriSynlig,
        )

    private val oppfyllerKravetPåUnntak =
        Opplysningstype.boolsk(
            oppfyllerKravetPåUnntakForUtdanningId,
            "Oppfyller kravet på unntak for utdanning eller opplæring",
            synlig = hvisTarUtdanning,
        )

    // Dagpengeforskriften § 4-3. Utdanning og opplæring
    //  § 4-3.(deltakelse i arbeidsmarkedstiltak)
    val deltakelseIArbeidsmarkedstiltak =
        Opplysningstype.boolsk(
            deltarIArbeidsmarkedstiltakId,
            "Deltar i arbeidsmarkedstiltak",
            synlig = hvisTarUtdanning,
        )

    // § 4-3a.(opplæring for innvandrere)
    val opplæringForInnvandrere =
        Opplysningstype.boolsk(
            deltarIOpplæringForInnvandrereId,
            "Deltar i opplæring for innvandrere",
            synlig = hvisTarUtdanning,
        )

    // § 4-3b.(grunnskoleopplæring, videregående opplæring og opplæring i grunnleggende ferdigheter)
    val grunnskoleopplæring =
        Opplysningstype.boolsk(
            grunnskoleopplæringId,
            "Deltar i grunnskoleopplæring, videregående opplæring og opplæring i grunnleggende ferdigheter",
            synlig = hvisTarUtdanning,
        )

    // § 4-3c.(høyere yrkesfaglig utdanning)
    val høyereYrkesfagligUtdanning =
        Opplysningstype.boolsk(
            deltarIHøyereYrkesfagligUtdanningId,
            "Deltar i høyere yrkesfaglig utdanning",
            synlig = hvisTarUtdanning,
        )

    // § 4-3d.(høyere utdanning)
    val høyereUtdanning = Opplysningstype.boolsk(deltarIHøyereUtdanningId, "Deltar i høyere utdanning", synlig = hvisTarUtdanning)

    // § 4-3e.(deltakelse på kurs mv
    val deltakelsePåKurs = Opplysningstype.boolsk(deltarPåKursMvId, "Deltar på kurs mv", synlig = hvisTarUtdanning)

    val kravTilUtdanning = Opplysningstype.boolsk(kravTilUtdanningEllerOpplæringId, "Krav til utdanning eller opplæring")

    val regelsett =
        vilkår(
            folketrygden.hjemmel(
                kapittel = 4,
                paragraf = 6,
                tittel = "Dagpenger under utdanning, opplæring, etablering av egen virksomhet m.v",
                kortnavn = "Utdanning",
            ),
        ) {
            skalVurderes { oppfyllerKravetTilMinsteinntektEllerVerneplikt(it) }

            regel(tarUtdanning) { innhentMed(søknadIdOpplysningstype) }

            // TODO: Legg til regler for å om kravet til utdanning skal vurderes
            regel(deltakelseIArbeidsmarkedstiltak) { oppslag(prøvingsdato) { false } }
            regel(opplæringForInnvandrere) { oppslag(prøvingsdato) { false } }
            regel(grunnskoleopplæring) { oppslag(prøvingsdato) { false } }
            regel(høyereYrkesfagligUtdanning) { oppslag(prøvingsdato) { false } }
            regel(høyereUtdanning) { oppslag(prøvingsdato) { false } }
            regel(deltakelsePåKurs) { oppslag(prøvingsdato) { false } }

            regel(godkjentUnntakForUtdanning) {
                enAv(
                    deltakelseIArbeidsmarkedstiltak,
                    opplæringForInnvandrere,
                    grunnskoleopplæring,
                    høyereYrkesfagligUtdanning,
                    høyereUtdanning,
                    deltakelsePåKurs,
                )
            }
            regel(svartJaPåUtdanning) { erSann(tarUtdanning) }
            regel(svartNeiPåUtdanning) { erUsann(tarUtdanning) }
            regel(oppfyllerKravetPåUnntak) { alle(svartJaPåUtdanning, godkjentUnntakForUtdanning) }

            utfall(kravTilUtdanning) { enAv(oppfyllerKravetPåUnntak, svartNeiPåUtdanning) }

            påvirkerResultat { oppfyllerKravetTilMinsteinntektEllerVerneplikt(it) }
        }
}
