package no.nav.dagpenger.opplysning

enum class OverlappStrategi {
    /** En nyere opplysning avslutter den eldre fra sin fraOgMed; den eldre gjenopptas ikke etterpå. */
    ERSTATT_FRA_FOM,

    /** En nyere, kortere opplysning bryter opp den eldre; resten til høyre består. */
    BEVAR_HØYRE,

    /** Kun den nyeste egne opplysningen er synlig, uavhengig av gyldighetsperiode. */
    KUN_SISTE,
}
