package no.nav.dagpenger.aktivitetslogg

interface IAktivitetslogg {
    fun info(melding: String, vararg params: Any?)
    fun logiskFeil(melding: String, vararg params: Any?): Nothing
    fun varsel(melding: String)
    fun varsel(kode: Varselkode)
    fun funksjonellFeil(kode: Varselkode)
    fun behov(type: Aktivitet.Behov.Behovtype, melding: String, detaljer: Map<String, Any> = emptyMap())
    fun harAktiviteter(): Boolean
    fun harFunksjonelleFeilEllerVerre(): Boolean
    fun aktivitetsteller(): Int
    fun behov(): List<Aktivitet.Behov>
    fun barn(): IAktivitetslogg
    fun kontekst(kontekst: Aktivitetskontekst)
    fun kontekst(kontekst: Subaktivitetskontekst)
    fun kontekster(): List<IAktivitetslogg>
    fun toMap(mapper: AktivitetsloggMappingPort): Map<String, List<Map<String, Any>>>
    fun accept(visitor: AktivitetsloggVisitor)
    fun registrer(observer: AktivitetsloggObserver)
}
