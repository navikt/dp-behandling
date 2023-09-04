package no.nav.dagpenger.vedtak.modell.visitor

import no.nav.dagpenger.aktivitetslogg.AktivitetsloggVisitor
import no.nav.dagpenger.vedtak.modell.PersonIdentifikator

interface PersonVisitor : VedtakHistorikkVisitor, RapporteringsperiodeVisitor, AktivitetsloggVisitor, SakVisitor {

    fun visitPerson(personIdentifikator: PersonIdentifikator) {}
}