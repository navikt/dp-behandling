package no.nav.dagpenger.mediator.audit

interface Auditlogg {
    fun les(
        melding: String,
        ident: String,
        saksbehandler: String,
    )

    fun opprett(
        melding: String,
        ident: String,
        saksbehandler: String,
    )

    fun oppdater(
        melding: String,
        ident: String,
        saksbehandler: String,
    )

    fun slett(
        melding: String,
        ident: String,
        saksbehandler: String,
    )
}
