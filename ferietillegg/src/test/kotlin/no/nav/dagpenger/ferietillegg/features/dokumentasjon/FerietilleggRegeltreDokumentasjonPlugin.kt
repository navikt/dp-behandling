package no.nav.dagpenger.ferietillegg.features.dokumentasjon

import io.cucumber.java.After
import io.cucumber.java.Scenario
import no.nav.dagpenger.ferietillegg.FerietilleggBeløp
import no.nav.dagpenger.ferietillegg.KravPåFerietillegg
import no.nav.dagpenger.testsupport.dokumentasjon.RegeltreDokumentasjonOppsett
import no.nav.dagpenger.testsupport.dokumentasjon.RegeltreDokumentasjonPlugin
import no.nav.dagpenger.testsupport.dokumentasjon.dokumenterRegeltre

@After("@dokumentasjon")
fun dokumentasjon(scenario: Scenario) {
    dokumenterRegeltre(scenario, ferietilleggRegeltreDokumentasjonOppsett)
}

class FerietilleggRegeltreDokumentasjonPlugin :
    RegeltreDokumentasjonPlugin(
        ferietilleggRegeltreDokumentasjonOppsett,
    )

internal val ferietilleggRegeltreDokumentasjonOppsett =
    RegeltreDokumentasjonOppsett(
        dokumentasjonskatalog = "regler",
        regelsettPerTag =
            mapOf(
                "@regel-ferietillegg-krav" to listOf(KravPåFerietillegg.regelsett, FerietilleggBeløp.regelsett),
            ),
    )
