package no.nav.dagpenger.behandling.modell

import no.nav.dagpenger.aktivitetslogg.aktivitet.Behov

data class OpplysningBehov(
    override val name: String,
) : Behov.Behovtype
