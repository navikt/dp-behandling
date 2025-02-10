package no.nav.dagpenger.opplysning.regelsett

import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Dato
import no.nav.dagpenger.opplysning.Desimaltall
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Penger
import no.nav.dagpenger.opplysning.ULID
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.dato.førsteArbeidsdag
import no.nav.dagpenger.opplysning.regel.enAv
import no.nav.dagpenger.opplysning.regel.innhentMed
import no.nav.dagpenger.opplysning.regel.multiplikasjon
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.opplysning.regel.størreEnnEllerLik
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.uuid.UUIDv7
import java.time.LocalDate

internal object ReglerForInntektTest {
    val antallG12mndInntekt =
        Opplysningstype.desimaltall(
            Opplysningstype.Id(UUIDv7.ny(), Desimaltall),
            "Antall G for krav til 12 mnd inntekt",
        )
    val antallG36mndInntekt =
        Opplysningstype.desimaltall(
            Opplysningstype.Id(UUIDv7.ny(), Desimaltall),
            "Antall G for krav til 36 mnd inntekt",
        )
    val inntekt12 =
        Opplysningstype.beløp(
            Opplysningstype.Id(UUIDv7.ny(), Penger),
            beskrivelse = "Inntekt siste 12 mnd",
            behovId = "inntekt12mnd",
        )
    val inntekt36 =
        Opplysningstype.beløp(
            Opplysningstype.Id(UUIDv7.ny(), Penger),
            beskrivelse = "Inntekt siste 36 mnd",
            behovId = "inntekt36mnd",
        )
    val grunnbeløp = Opplysningstype.beløp(Opplysningstype.Id(UUIDv7.ny(), Penger), "Grunnbeløp")

    private val prøvingsdato = Prøvingsdato.prøvingsdato
    private val antattRapporteringsFrist = Opplysningstype.dato(Opplysningstype.Id(UUIDv7.ny(), Dato), "Antatt rapporteringsfrist")
    private val reellRapporteringsFrist = Opplysningstype.dato(Opplysningstype.Id(UUIDv7.ny(), Dato), "Reell rapporteringsfrist")
    private val sisteAvsluttendeKalenderMåned =
        Opplysningstype.dato(
            Opplysningstype.Id(UUIDv7.ny(), Dato),
            "Siste avsluttendende kalendermåned",
        )
    private val førsteAvsluttendeKalenderMåned = Opplysningstype.dato(Opplysningstype.Id(UUIDv7.ny(), Dato), "Første kalendermåned")
    private val inntektId = Opplysningstype.ulid(Opplysningstype.Id(UUIDv7.ny(), ULID), "InntektId")

    private val nedreTerskel = Opplysningstype.beløp(Opplysningstype.Id(UUIDv7.ny(), Penger), "Inntektskrav for siste 12 mnd")
    private val øvreTerskel = Opplysningstype.beløp(Opplysningstype.Id(UUIDv7.ny(), Penger), "Inntektskrav for siste 36 mnd")
    private val overNedreTerskel =
        Opplysningstype.boolsk(
            Opplysningstype.Id(UUIDv7.ny(), Boolsk),
            "Inntekt er over kravet for siste 12 mnd",
        )
    private val overØvreTerskel = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "Inntekt er over kravet for siste 36 mnd")

    val minsteinntekt = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "Minsteinntekt")

    val regelsett =
        vilkår("Minsteinntekt") {
            regel(antattRapporteringsFrist) { oppslag(prøvingsdato) { LocalDate.of(it.year, it.month, 5) } }
            regel(reellRapporteringsFrist) { førsteArbeidsdag(antattRapporteringsFrist) }
            regel(antallG12mndInntekt) { oppslag(prøvingsdato) { 1.5 } }
            regel(antallG36mndInntekt) { oppslag(prøvingsdato) { 3.0 } }
            regel(inntekt12) { innhentMed(prøvingsdato) }
            regel(inntekt36) { innhentMed(prøvingsdato) }
            regel(grunnbeløp) { oppslag(prøvingsdato) { Grunnbeløp.finnFor(it) } }
            regel(nedreTerskel) { multiplikasjon(grunnbeløp, antallG12mndInntekt) }
            regel(øvreTerskel) { multiplikasjon(grunnbeløp, antallG36mndInntekt) }
            regel(overNedreTerskel) { størreEnnEllerLik(inntekt12, nedreTerskel) }
            regel(overØvreTerskel) { størreEnnEllerLik(inntekt36, øvreTerskel) }
            regel(minsteinntekt) { enAv(overNedreTerskel, overØvreTerskel) }
        }
}

internal object Grunnbeløp {
    val TEST_GRUNNBELØP = Beløp(118620.0)
    private val grunnbeløp =
        mapOf(
            LocalDate.of(2020, 5, 1) to Beløp(99858.0),
            LocalDate.of(2021, 5, 1) to Beløp(105144.0),
            LocalDate.of(2022, 5, 1) to TEST_GRUNNBELØP,
        )

    fun finnFor(dato: LocalDate) =
        grunnbeløp
            .filterKeys { it <= dato }
            .maxByOrNull { it.key }
            ?.value
            ?: throw IllegalArgumentException("Fant ikke grunnbeløp for $dato")
}
