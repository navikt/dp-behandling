package no.nav.dagpenger.features.dokumentasjon

import io.cucumber.java.After
import io.cucumber.java.Scenario
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengegrunnlag
import no.nav.dagpenger.regel.regelsett.fastsetting.DagpengenesStørrelse
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengeperiode
import no.nav.dagpenger.regel.regelsett.fastsetting.Egenandel
import no.nav.dagpenger.regel.regelsett.fastsetting.PermitteringFastsetting
import no.nav.dagpenger.regel.regelsett.fastsetting.PermitteringFraFiskeindustrienFastsetting
import no.nav.dagpenger.regel.regelsett.fastsetting.SamordingUtenforFolketrygden
import no.nav.dagpenger.regel.regelsett.fastsetting.VernepliktFastsetting
import no.nav.dagpenger.regel.regelsett.prosessvilkår.OmgjøringUtenKlage
import no.nav.dagpenger.regel.regelsett.prosessvilkår.Uriktigeopplysninger
import no.nav.dagpenger.regel.regelsett.vilkår.Alderskrav
import no.nav.dagpenger.regel.regelsett.vilkår.FulleYtelser
import no.nav.dagpenger.regel.regelsett.vilkår.Gjenopptak
import no.nav.dagpenger.regel.regelsett.vilkår.Meldeplikt
import no.nav.dagpenger.regel.regelsett.vilkår.Minsteinntekt
import no.nav.dagpenger.regel.regelsett.vilkår.Opphold
import no.nav.dagpenger.regel.regelsett.vilkår.Opptjeningstid
import no.nav.dagpenger.regel.regelsett.vilkår.Permittering
import no.nav.dagpenger.regel.regelsett.vilkår.PermitteringFraFiskeindustrien
import no.nav.dagpenger.regel.regelsett.vilkår.ReellArbeidssøker
import no.nav.dagpenger.regel.regelsett.vilkår.RegistrertArbeidssøker
import no.nav.dagpenger.regel.regelsett.vilkår.Samordning
import no.nav.dagpenger.regel.regelsett.vilkår.Sanksjonsperiode
import no.nav.dagpenger.regel.regelsett.vilkår.StreikOgLockout
import no.nav.dagpenger.regel.regelsett.vilkår.TapAvArbeidsinntektOgArbeidstid
import no.nav.dagpenger.regel.regelsett.vilkår.TidsbegrensetBortfall
import no.nav.dagpenger.regel.regelsett.vilkår.Utdanning
import no.nav.dagpenger.regel.regelsett.vilkår.Utestengning
import no.nav.dagpenger.regel.regelsett.vilkår.Verneplikt
import no.nav.dagpenger.testsupport.dokumentasjon.RegeltreDokumentasjonOppsett
import no.nav.dagpenger.testsupport.dokumentasjon.RegeltreDokumentasjonPlugin
import no.nav.dagpenger.testsupport.dokumentasjon.dokumenterRegeltre

@After("@dokumentasjon")
fun dokumentasjon(scenario: Scenario) {
    dokumenterRegeltre(scenario, dagpengerRegeltreDokumentasjonOppsett)
}

class DagpengerRegeltreDokumentasjonPlugin :
    RegeltreDokumentasjonPlugin(
        dagpengerRegeltreDokumentasjonOppsett,
    )

internal val dagpengerRegeltreDokumentasjonOppsett =
    RegeltreDokumentasjonOppsett(
        regelsettPerTag =
            mapOf(
                "@regel-alder" to Alderskrav.regelsett,
                "@regel-minsteinntekt" to Minsteinntekt.regelsett,
                "@regel-opptjeningstid" to Opptjeningstid.regelsett,
                "@regel-reell-arbeidssøker" to ReellArbeidssøker.regelsett,
                "@regel-meldeplikt" to Meldeplikt.regelsett,
                "@regel-opphold" to Opphold.regelsett,
                "@regel-tap-arbeidsinntekt-og-arbeidstid" to TapAvArbeidsinntektOgArbeidstid.regelsett,
                "@regel-utdanning" to Utdanning.regelsett,
                "@regel-verneplikt" to Verneplikt.regelsett,
                "@regel-utestengning" to Utestengning.regelsett,
                "@regel-fulle-ytelser-eller-afp" to FulleYtelser.regelsett,
                "@regel-streik-og-lockout" to StreikOgLockout.regelsett,
                "@regel-dapengeperiode" to Dagpengeperiode.regelsett,
                "@regel-dagpengegrunnlag" to Dagpengegrunnlag.regelsett,
                "@regel-dagpengensStørrelse" to DagpengenesStørrelse.regelsett,
                "@regel-egenandel" to Egenandel.regelsett,
                "@regel-verneplikt-fastsetting" to VernepliktFastsetting.regelsett,
                "@regel-samordning" to Samordning.regelsett,
                "@regel-samordning-utenfor-folketrygden" to SamordingUtenforFolketrygden.regelsett,
                "@regel-permittering" to Permittering.regelsett,
                "@regel-permittering-fastsetting" to PermitteringFastsetting.regelsett,
                "@regel-registrert-arbeidssøker" to RegistrertArbeidssøker.regelsett,
                "@regel-permittering-fiskeindustrien" to PermitteringFraFiskeindustrien.regelsett,
                "@regel-permitteringFiskeindustrien-fastsetting" to PermitteringFraFiskeindustrienFastsetting.regelsett,
                "@regel-uriktig-eller-mangelfulle-opplysninger" to Uriktigeopplysninger.regelsett,
                "@regel-omgjøring-uten-klage" to OmgjøringUtenKlage.regelsett,
                "@regel-gjenopptak" to Gjenopptak.regelsett,
                "@regel-sanksjon" to Sanksjonsperiode.regelsett,
                "@regel-tidsbegrenset-bortfall" to TidsbegrensetBortfall.regelsett,
            ).map {
                it.key to listOf(it.value)
            }.toMap(),
    )
