package no.nav.dagpenger.behandling.mediator.repository

import kotliquery.Session

data class PostgresUnitOfWork(
    val session: Session,
)
