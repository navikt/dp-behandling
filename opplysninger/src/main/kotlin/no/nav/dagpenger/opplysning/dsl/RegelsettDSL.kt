package no.nav.dagpenger.opplysning.dsl

import no.nav.dagpenger.opplysning.Hjemmel
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.Regelverkstype
import no.nav.dagpenger.opplysning.tomHjemmel

fun <T : Regelverkstype> vilkår(
    hjemmel: String,
    block: VilkårRegelsettBuilder<T>.() -> Unit,
) = vilkår(tomHjemmel(hjemmel), block)

fun <T : Regelverkstype> vilkår(
    hjemmel: Hjemmel,
    block: VilkårRegelsettBuilder<T>.() -> Unit,
): Regelsett<T> {
    val builder = VilkårRegelsettBuilder<T>(hjemmel)
    builder.block()
    return builder.build()
}

fun <T : Regelverkstype> fastsettelse(
    hjemmel: Hjemmel,
    block: FastsettelseRegelsettBuilder<T>.() -> Unit,
): Regelsett<T> {
    val builder = FastsettelseRegelsettBuilder<T>(hjemmel)
    builder.block()
    return builder.build()
}
