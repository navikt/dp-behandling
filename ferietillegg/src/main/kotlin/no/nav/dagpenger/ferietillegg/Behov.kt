package no.nav.dagpenger.ferietillegg

import kotlin.reflect.KProperty

object Behov {
    val AntallDagerForbrukt by StringConstant()
    val OpptjeningsårFerietillegg by StringConstant()
    val OpptjeningsBeløp by StringConstant()
}

private class StringConstant {
    operator fun getValue(
        thisRef: Any?,
        property: KProperty<*>,
    ) = property.name
}
