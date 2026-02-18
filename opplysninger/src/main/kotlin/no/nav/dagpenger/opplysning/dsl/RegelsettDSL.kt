package no.nav.dagpenger.opplysning.dsl

import no.nav.dagpenger.opplysning.Hjemmel
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.tomHjemmel

fun vilkår(
    hjemmel: String,
    block: VilkårRegelsettBuilder.() -> Unit,
) = vilkår(tomHjemmel(hjemmel), block)

fun vilkår(
    hjemmel: Hjemmel,
    block: VilkårRegelsettBuilder.() -> Unit,
): Regelsett {
    val builder = VilkårRegelsettBuilder(hjemmel)
    builder.block()
    return builder.build()
}

fun fastsettelse(
    hjemmel: Hjemmel,
    block: FastsettelseRegelsettBuilder.() -> Unit,
): Regelsett {
    val builder = FastsettelseRegelsettBuilder(hjemmel)
    builder.block()
    return builder.build()
}

/**
 * Oppretter et prosess-regelsett.
 * Opplysninger produsert av prosess-regelsett arves IKKE til neste behandling i kjeden.
 */
fun prosess(
    hjemmel: Hjemmel,
    block: ProsessRegelsettBuilder.() -> Unit,
): Regelsett {
    val builder = ProsessRegelsettBuilder(hjemmel)
    builder.block()
    return builder.build()
}
