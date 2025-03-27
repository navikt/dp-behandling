package no.nav.dagpenger.opplysning.dsl

import no.nav.dagpenger.opplysning.Hjemmel
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.Regelverk
import no.nav.dagpenger.opplysning.tomHjemmel

fun vilkår(
    regelverk: Regelverk,
    hjemmel: String,
    block: VilkårRegelsettBuilder.() -> Unit,
) = vilkår(regelverk, tomHjemmel(hjemmel), block)

fun vilkår(
    regelverk: Regelverk,
    hjemmel: Hjemmel,
    block: VilkårRegelsettBuilder.() -> Unit,
): Regelsett {
    val builder = VilkårRegelsettBuilder(hjemmel, regelverk)
    builder.block()
    return builder.build().also {
        regelverk.registrer(it)
    }
}

fun fastsettelse(
    regelverk: Regelverk,
    hjemmel: Hjemmel,
    block: FastsettelseRegelsettBuilder.() -> Unit,
): Regelsett {
    val builder = FastsettelseRegelsettBuilder(hjemmel, regelverk)
    builder.block()
    return builder.build().also {
        regelverk.registrer(it)
    }
}
