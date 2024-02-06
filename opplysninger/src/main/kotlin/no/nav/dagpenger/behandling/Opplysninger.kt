package no.nav.dagpenger.behandling

interface LesbarOpplysninger {
    fun <T : Comparable<T>> finnOpplysning(opplysningstype: Opplysningstype<T>): Opplysning<T>

    fun har(opplysningstype: Opplysningstype<*>): Boolean

    fun finnAlle(opplysningstyper: List<Opplysningstype<*>>): List<Opplysning<*>>

    fun finnAlle(): List<Opplysning<*>>
}

class Opplysninger(
    opplysninger: List<Opplysning<*>> = emptyList(),
) : LesbarOpplysninger {
    private lateinit var regelkjøring: Regelkjøring
    private val opplysninger: MutableList<Opplysning<*>> = opplysninger.toMutableList()

    constructor() : this(mutableListOf())

    fun registrer(regelkjøring: Regelkjøring) {
        this.regelkjøring = regelkjøring
    }

    override fun <T : Comparable<T>> finnOpplysning(opplysningstype: Opplysningstype<T>): Opplysning<T> {
        return finnNullableOpplysning(opplysningstype)
            ?: throw IllegalStateException("Har ikke opplysning $opplysningstype som er gyldig for ${regelkjøring.forDato}")
    }

    fun leggTil(opplysning: Opplysning<*>) {
        require(opplysninger.none { it.sammeSom(opplysning) }) {
            "Opplysning ${opplysning.opplysningstype} finnes allerede med overlappende gyldighetsperiode"
        }
        opplysninger.add(opplysning)
        regelkjøring.evaluer()
    }

    fun erstatt(opplysning: Opplysning<*>) {
        require(
            opplysninger.any {
                it.sammeSom(opplysning)
            },
        ) { "Opplysning ${opplysning.opplysningstype} med samme opplysningstype og gyldighetsperiode finnes ikke" }

        val opplysningSomSkalErstattes = finnOpplysning(opplysning.opplysningstype)
        if (opplysning.gyldighetsperiode.inneholder(opplysningSomSkalErstattes.gyldighetsperiode.fom)) {
            println("scenario 1")
        } else if (opplysning.gyldighetsperiode.inneholder(opplysningSomSkalErstattes.gyldighetsperiode.tom)) {
            println("scenario 2")
            // scenario 2 - opplysning vi skal erstatte med har overlapp på tom dato
        } else {
            val opplysningSomSkalErstattesFør =
                opplysningSomSkalErstattes.erstatt(
                    Gyldighetsperiode(
                        opplysningSomSkalErstattes.gyldighetsperiode.fom,
                        opplysning.gyldighetsperiode.fom.minusDays(1),
                    ),
                )

            val opplysningSomSkalErstattesEtter =
                opplysningSomSkalErstattes.erstatt(
                    Gyldighetsperiode(
                        opplysning.gyldighetsperiode.tom.plusDays(1),
                        opplysningSomSkalErstattes.gyldighetsperiode.tom,
                    ),
                )
            this.opplysninger.remove(opplysningSomSkalErstattes)
            this.leggTil(opplysningSomSkalErstattesFør)
            this.leggTil(opplysningSomSkalErstattesEtter)
            this.leggTil(opplysning)

            // scenario 3 - opplysning vi skal erstatte med har overlapp på fom og tom dato
        }

        // finne opplysning
        // splitte på gyldighetsdato
        // legge til en til tre nye opplysinger
        // fjerne den gamle
    }

    private fun <T : Comparable<T>> finnNullableOpplysning(opplysningstype: Opplysningstype<T>): Opplysning<T>? =
        opplysninger.firstOrNull { it.er(opplysningstype) && it.gyldighetsperiode.inneholder(regelkjøring.forDato) } as Opplysning<T>?

    override fun har(opplysningstype: Opplysningstype<*>) = finnNullableOpplysning(opplysningstype) != null

    override fun finnAlle(opplysningstyper: List<Opplysningstype<*>>) = opplysningstyper.mapNotNull { finnNullableOpplysning(it) }

    override fun finnAlle() = opplysninger.toList()
}
