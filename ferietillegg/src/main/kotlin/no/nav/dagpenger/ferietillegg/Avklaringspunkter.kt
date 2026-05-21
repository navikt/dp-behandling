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

    val KontrollFerietilleggRevurdering =
        Avklaringkode(
            kode = "FerietilleggRevurdert",
            tittel = "Ferietillegg er revurdert",
            beskrivelse =
                "Ferietillegg er revurdert",
            kanAvbrytes = false,
            kanKvitteres = true,
        )
}
