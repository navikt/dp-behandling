package no.nav.dagpenger.mediator.repository

import kotliquery.Session

data class PostgresUnitOfWork(
    val session: Session,
)
