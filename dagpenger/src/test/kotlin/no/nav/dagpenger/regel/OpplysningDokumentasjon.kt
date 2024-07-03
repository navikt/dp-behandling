package no.nav.dagpenger.regel

import com.spun.util.persistence.Loader
import no.nav.dagpenger.opplysning.dag.RegeltreBygger
import no.nav.dagpenger.opplysning.regel.Ekstern
import org.approvaltests.Approvals
import org.approvaltests.core.Options
import org.approvaltests.namer.NamerWrapper
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class OpplysningDokumentasjon {
    @Test
    fun `henter ut hvilken informasjon som må innhentes`() {
        val regler =
            listOf(
                Alderskrav.regelsett,
                Meldeplikt.regelsett,
                Minsteinntekt.regelsett,
                Opptjeningstid.regelsett,
                ReellArbeidssøker.regelsett,
                KravPåDagpenger.regelsett,
                Rettighetstype.regelsett,
                Søknadstidspunkt.regelsett,
                Verneplikt.regelsett,
            )

        val dag = RegeltreBygger(*regler.toTypedArray()).dag()
        val opplysningerUtenRegel = dag.findLeafNodes()
        val opplysningerMedEksternRegel = dag.findNodesWithEdge { it.data is Ekstern<*> }
        val behov = opplysningerUtenRegel + opplysningerMedEksternRegel

        val markdown =
            """
            ># Dokumentasjon på behov for opplysninger
            >
            >Dette er opplysninger som blir innhentet som en del av dagpengebehandlingen. De publiseres som behov på rapiden.
            >
            >|Behov|Beskrivelse|Logisk datatype|Datatype|
            >|---|---|---|---|
            ${behov.sortedBy { it.data.id }.joinToString("\n") {
                ">|${it.data.id} | ${it.data.navn} | ${it.data.datatype}|${it.data.datatype.klasse.simpleName}|"
            }}
            """.trimMargin(">")

        skriv(markdown)
    }

    private companion object {
        val path = "${Paths.get("").toAbsolutePath().toString().substringBeforeLast("/")}/docs/"
        val options = Options().forFile().withExtension(".md")
    }

    private fun skriv(behov: String) {
        Approvals.namerCreater = Loader { NamerWrapper({ "behov" }, { path }) }
        Approvals.verify(behov, options)
    }
}
