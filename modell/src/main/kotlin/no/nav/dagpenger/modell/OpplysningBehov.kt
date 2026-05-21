package no.nav.dagpenger.modell

import no.nav.dagpenger.aktivitetslogg.aktivitet.Behov

data class OpplysningBehov(
    override val name: String,
) : Behov.Behovtype
