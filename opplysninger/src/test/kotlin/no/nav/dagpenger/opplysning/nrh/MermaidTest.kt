package no.nav.dagpenger.opplysning.nrh

import no.nav.dagpenger.opplysning.dag.RegeltreBygger
import no.nav.dagpenger.opplysning.dag.printer.MermaidPrinter

fun main() {
    val dag = RegeltreBygger(BliPåPlassVilkår.regelsett).dag()
    val mermaidPrinter = MermaidPrinter(dag)
    println(mermaidPrinter.toPrint())
}
