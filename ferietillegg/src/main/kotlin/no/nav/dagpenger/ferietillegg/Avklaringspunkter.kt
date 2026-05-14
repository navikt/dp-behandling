package no.nav.dagpenger.ferietillegg

import no.nav.dagpenger.opplysning.Avklaringkode

object Avklaringspunkter {
    val KontrollFerietillegg =
        Avklaringkode(
            kode = "KontrollFerietillegg",
            tittel = "Manuell kontroll ferietillegg",
            beskrivelse =
                """
                Manuell kontroll av ferietillegg.
                """.trimIndent(),
        )
}
