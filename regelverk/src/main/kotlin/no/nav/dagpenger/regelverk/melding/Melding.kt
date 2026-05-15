package no.nav.dagpenger.regelverk.melding

import java.util.UUID

interface Melding {
    val id: UUID

    fun lagreMelding(repository: MeldingRepository)
}
