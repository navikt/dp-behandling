package no.nav.dagpenger.behandling.mediator.repository

import io.github.oshai.kotlinlogging.KotlinLogging
import io.opentelemetry.instrumentation.annotations.WithSpan
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.behandling.mediator.repository.JsonSerde.Companion.serde
import no.nav.dagpenger.behandling.objectMapper
import no.nav.dagpenger.opplysning.BarnDatatype
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Datatype
import no.nav.dagpenger.opplysning.Dato
import no.nav.dagpenger.opplysning.Desimaltall
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Heltall
import no.nav.dagpenger.opplysning.Hypotese
import no.nav.dagpenger.opplysning.InntektDataType
import no.nav.dagpenger.opplysning.Kilde
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningsformål
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.alltidSynlig
import no.nav.dagpenger.opplysning.Penger
import no.nav.dagpenger.opplysning.PeriodeDataType
import no.nav.dagpenger.opplysning.Tekst
import no.nav.dagpenger.opplysning.ULID
import no.nav.dagpenger.opplysning.Utledning
import no.nav.dagpenger.opplysning.verdier.Barn
import no.nav.dagpenger.opplysning.verdier.BarnListe
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.Inntekt
import no.nav.dagpenger.opplysning.verdier.Periode
import no.nav.dagpenger.opplysning.verdier.Ulid
import org.postgresql.util.PGobject
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.dagpenger.inntekt.v1.Inntekt as InntektV1

class OpplysningerRepositoryPostgres : OpplysningerRepository {
    internal companion object {
        private val opplysningstyper by lazy {
            Opplysningstype.definerteTyper.associateBy { it.id }
        }
        private val logger = KotlinLogging.logger { }
        private val kildeRepository = KildeRepository()

        fun Session.hentOpplysninger(opplysningerId: UUID) =
            OpplysningRepository(opplysningerId, this).hentOpplysninger().let { Opplysninger.rehydrer(opplysningerId, it) }

        private val serdeBarn = objectMapper.serde<List<Barn>>()
        private val serdeInntekt = objectMapper.serde<InntektV1>()
        private val serdePeriode = objectMapper.serde<Periode>()
    }

    override fun hentOpplysninger(opplysningerId: UUID) =
        sessionOf(dataSource)
            .use { session -> return@use session.hentOpplysninger(opplysningerId) }

    override fun lagreOpplysninger(opplysninger: Opplysninger) {
        val unitOfWork = PostgresUnitOfWork.transaction()
        lagreOpplysninger(opplysninger, unitOfWork)
        unitOfWork.commit()
    }

    override fun lagreOpplysninger(
        opplysninger: Opplysninger,
        unitOfWork: UnitOfWork<*>,
    ) = lagreOpplysninger(opplysninger, unitOfWork as PostgresUnitOfWork)

    private fun lagreOpplysninger(
        opplysninger: Opplysninger,
        unitOfWork: PostgresUnitOfWork,
    ) = unitOfWork.inTransaction { tx ->
        tx.run(
            queryOf(
                //language=PostgreSQL
                """
                INSERT INTO opplysninger (opplysninger_id) VALUES (:opplysningerId) ON CONFLICT DO NOTHING
                """.trimIndent(),
                mapOf("opplysningerId" to opplysninger.id),
            ).asUpdate,
        )

        val somListe: List<Opplysning<*>> = opplysninger.somListe(LesbarOpplysninger.Filter.Egne)
        /* // TODO: Denne kontrollen er ikke tiltenkt å kjøre for alltid :) Den bør fjernes når vi er sikre på at data er bra
        val overlappende: Map<Opplysningstype<out Comparable<*>>, Boolean> =
            somListe
                .groupBy { opplysning -> opplysning.opplysningstype }
                .mapValues { (_, opplysning) -> opplysning.map { it.gyldighetsperiode } }
                .mapValues { (_, gyldighetsperioder) -> gyldighetsperioder.overlappendePerioder() }
                .filter { it.value }

        if (overlappende.isNotEmpty()) {
            throw IllegalStateException(
                """Opplysninger med id=${opplysninger.id} har overlappende gyldighetsperioder. ${
                    overlappende.filter {
                        it.value
                    }.keys
                }""",
            )
        }*/

        OpplysningRepository(opplysninger.id, tx).lagreOpplysninger(
            somListe.filter { it.skalLagres },
            opplysninger.fjernet(),
        )
    }

    override fun lagreOpplysningstyper(opplysningstyper: Collection<Opplysningstype<*>>) =
        sessionOf(dataSource).use { session ->
            BatchStatement(
                //language=PostgreSQL
                """
                INSERT INTO opplysningstype (behov_id, navn, datatype, formål, uuid)
                VALUES (:behovId, :navn, :datatype, :formaal, :uuid)
                ON CONFLICT (uuid, datatype) DO UPDATE SET formål = :formaal, navn = :navn, behov_id = :behovId
                """.trimIndent(),
                opplysningstyper.map {
                    mapOf(
                        "behovId" to it.behovId,
                        "navn" to it.navn,
                        "datatype" to it.datatype.navn(),
                        "formaal" to it.formål.name,
                        "uuid" to it.id.uuid,
                    )
                },
            ).run(session)
        }

    private class OpplysningRepository(
        private val opplysningerId: UUID,
        private val session: Session,
        private val kildeRespository: KildeRepository = kildeRepository,
    ) {
        fun hentOpplysninger(): List<Opplysning<*>> {
            val rader: MutableSet<OpplysningRad<*>> =
                session
                    .run(
                        queryOf(
                            //language=PostgreSQL
                            "SELECT * FROM opplysningstabell WHERE opplysninger_id = :id ORDER BY id",
                            mapOf("id" to opplysningerId),
                        ).map { row ->
                            val datatype = Datatype.fromString(row.string("datatype"))
                            row.somOpplysningRad(datatype)
                        }.asList,
                    ).toMutableSet()

            // Hent alle opplysninger fra tidligere opplysninger som erstattes av opplysninger i denne
            val erstatter = ArrayDeque(rader.mapNotNull { it.erstatter })
            while (erstatter.isNotEmpty()) {
                val uuid = erstatter.removeFirst()
                if (rader.none { it.id == uuid }) {
                    val opplysning = hentOpplysning(uuid)!!
                    rader.add(opplysning)
                    opplysning.erstatter?.let { erstatter.add(it) }
                }
            }

            // Hent utledetAv-opplysninger fra tidligere opplysninger
            val utledetAv = ArrayDeque(rader.mapNotNull { it.utledetAv?.opplysninger }.flatten().distinct())
            while (utledetAv.isNotEmpty()) {
                val uuid = utledetAv.removeFirst()
                if (rader.none { it.id == uuid }) {
                    val opplysning =
                        hentOpplysning(uuid)
                            ?: throw IllegalStateException("Opplysning (id=$uuid) som har vært brukt som utledning finnes ikke")
                    rader.add(opplysning)
                    opplysning.utledetAv?.opplysninger?.let { utledetAv.addAll(it) }
                }
            }

            // Hent inn kilde for alle opplysninger vi trenger
            val kilder = kildeRespository.hentKilder(rader.mapNotNull { it.kildeId }, session)
            val raderMedKilde =
                rader.map {
                    if (it.kildeId == null) return@map it
                    val kilde = kilder[it.kildeId] ?: throw IllegalStateException("Mangler kilde")
                    it.copy(kilde = kilde)
                }

            val brutto = raderMedKilde
            val raderFraTidligereOpplysninger = brutto.filterNot { it.opplysingerId == opplysningerId }.map { it.id }
            return brutto.somOpplysninger().filterNot { it.id in raderFraTidligereOpplysninger }
        }

        private fun hentOpplysning(id: UUID) =
            session.run(
                queryOf(
                    //language=PostgreSQL
                    "SELECT * FROM opplysningstabell WHERE id = :id LIMIT 1",
                    mapOf("id" to id),
                ).map { row ->
                    val datatype = Datatype.fromString(row.string("datatype"))
                    row.somOpplysningRad(datatype)
                }.asSingle,
            )

        private fun <T : Comparable<T>> Row.somOpplysningRad(datatype: Datatype<T>): OpplysningRad<T> {
            val opplysingerId = uuid("opplysninger_id")
            val id = uuid("id")
            val typeUuid = uuid("type_uuid")

            val opplysningTypeId = Opplysningstype.Id(typeUuid, datatype)
            val opplysningstype: Opplysningstype<T> =
                opplysningstyper[opplysningTypeId]
                    ?.let {
                        if (datatype != it.datatype) {
                            logger.warn {
                                """
                                Lastet opplysningstype med feil 
                                datatype: ${opplysningTypeId.datatype} - Id ${opplysningTypeId.uuid} - Har navn: ${string("type_id")}
                                database: $datatype, 
                                kode: ${it.datatype}
                                """.trimIndent()
                            }
                            return@let null
                        }
                        @Suppress("UNCHECKED_CAST")
                        it as Opplysningstype<T>
                    } ?: Opplysningstype(
                    // Fallback når opplysningstype ikke er definert i kode lengre
                    id = opplysningTypeId,
                    navn = string("type_navn"),
                    behovId = string("type_behov_id"),
                    formål = Opplysningsformål.valueOf(string("type_formål")),
                    synlig = alltidSynlig,
                )

            val gyldighetsperiode =
                Gyldighetsperiode(
                    localDateOrNull("gyldig_fom") ?: LocalDate.MIN,
                    localDateOrNull("gyldig_tom") ?: LocalDate.MAX,
                )
            val status = this.string("status")
            val verdi = datatype.verdi(this)
            val opprettet = this.localDateTime("opprettet")
            val utledetAvId = this.arrayOrNull<UUID>("utledet_av_id")?.toList() ?: emptyList()
            val utledetAv = this.stringOrNull("utledet_av")?.let { UtledningRad(it, utledetAvId) }
            val erstatterId = this.uuidOrNull("erstatter_id")

            val kildeId = this.uuidOrNull("kilde_id")

            return OpplysningRad(
                opplysingerId = opplysingerId,
                id = id,
                opplysningstype = opplysningstype,
                verdi = verdi,
                status = status,
                gyldighetsperiode = gyldighetsperiode,
                utledetAv = utledetAv,
                kildeId = kildeId,
                kilde = null,
                opprettet = opprettet,
                erstatter = erstatterId,
            )
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T : Comparable<T>> Datatype<T>.verdi(row: Row): T =
            when (this) {
                Boolsk -> row.boolean("verdi_boolsk")
                Dato ->
                    when (row.string("verdi_dato")) {
                        "-infinity" -> LocalDate.MIN
                        "infinity" -> LocalDate.MAX
                        else -> row.localDate("verdi_dato")
                    }

                Desimaltall -> row.double("verdi_desimaltall")
                Heltall -> row.int("verdi_heltall")
                ULID -> Ulid(row.string("verdi_string"))
                Penger -> Beløp(row.string("verdi_string"))
                Tekst -> row.string("verdi_string")
                BarnDatatype -> BarnListe(serdeBarn.fromJson(row.string("verdi_jsonb")))
                InntektDataType -> Inntekt(row.binaryStream("verdi_jsonb").use { serdeInntekt.fromJson(it) })
                PeriodeDataType -> serdePeriode.fromJson(row.string("verdi_jsonb"))
            } as T

        fun lagreOpplysninger(
            opplysninger: List<Opplysning<*>>,
            fjernet: Set<Opplysning<*>>,
        ) {
            kildeRespository.lagreKilder(opplysninger.mapNotNull { it.kilde }, session)
            batchOpplysninger(opplysninger).run(session)
            batchFjernet(fjernet).run(session)
            lagreErstatter(opplysninger).run(session)
            batchVerdi(opplysninger).run(session)
            batchOpplysningLink(opplysninger).run(session)
            lagreUtledetAv(opplysninger)
        }

        @WithSpan
        private fun lagreUtledetAv(opplysninger: List<Opplysning<*>>) {
            val utlededeOpplysninger = opplysninger.filterNot { it.utledetAv == null }
            batchUtledning(utlededeOpplysninger).run(session)
            batchUtledetAv(utlededeOpplysninger).run(session)
        }

        @WithSpan
        private fun batchUtledning(opplysninger: List<Opplysning<*>>) =
            BatchStatement(
                // language=PostgreSQL
                """
                INSERT INTO opplysning_utledning (opplysning_id, regel) 
                VALUES (:opplysningId, :regel)
                ON CONFLICT DO NOTHING
                """.trimIndent(),
                opplysninger.mapNotNull { opplysningSomBleUtledet ->
                    opplysningSomBleUtledet.utledetAv?.let { utledning ->
                        mapOf(
                            "opplysningId" to opplysningSomBleUtledet.id,
                            "regel" to utledning.regel,
                        )
                    }
                },
            )

        @WithSpan
        private fun batchUtledetAv(opplysninger: List<Opplysning<*>>) =
            BatchStatement(
                // language=PostgreSQL
                """
                INSERT INTO opplysning_utledet_av (opplysning_id, utledet_av) 
                VALUES (:opplysningId, :utledetAv)
                ON CONFLICT DO NOTHING
                """.trimIndent(),
                opplysninger.flatMap { opplysning ->
                    opplysning.utledetAv!!.opplysninger.map {
                        mapOf(
                            "opplysningId" to opplysning.id,
                            "utledetAv" to it.id,
                        )
                    }
                },
            )

        @WithSpan
        private fun batchOpplysningLink(opplysninger: List<Opplysning<*>>) =
            BatchStatement(
                //language=PostgreSQL
                """
                INSERT INTO opplysninger_opplysning (opplysninger_id, opplysning_id) 
                VALUES (:opplysningerId, :opplysningId)
                ON CONFLICT DO NOTHING
                """.trimIndent(),
                opplysninger.map {
                    mapOf(
                        "opplysningerId" to opplysningerId,
                        "opplysningId" to it.id,
                    )
                },
            )

        @WithSpan
        private fun batchOpplysninger(opplysninger: List<Opplysning<*>>) =
            BatchStatement(
                //language=PostgreSQL
                """
                WITH ins AS (
                    SELECT opplysningstype_id FROM opplysningstype WHERE uuid = :typeUuid AND datatype = :datatype 
                )
                INSERT INTO opplysning (id, status, opplysningstype_id, kilde_id, gyldig_fom, gyldig_tom, opprettet)
                VALUES (:id, :status, (SELECT opplysningstype_id FROM ins), :kilde_id, :fom::timestamp, :tom::timestamp, :opprettet)
                ON CONFLICT(id) DO NOTHING
                """.trimIndent(),
                opplysninger.map { opplysning ->
                    val gyldighetsperiode: Gyldighetsperiode = opplysning.gyldighetsperiode
                    mapOf(
                        "id" to opplysning.id,
                        "status" to opplysning.javaClass.simpleName,
                        "typeUuid" to opplysning.opplysningstype.id.uuid,
                        "datatype" to opplysning.opplysningstype.datatype.navn(),
                        "kilde_id" to opplysning.kilde?.id,
                        "fom" to gyldighetsperiode.fom.takeUnless { it.isEqual(LocalDate.MIN) },
                        "tom" to gyldighetsperiode.tom.takeUnless { it.isEqual(LocalDate.MAX) },
                        "opprettet" to opplysning.opprettet,
                    )
                },
            )

        @WithSpan
        private fun batchFjernet(fjernet: Set<Opplysning<*>>) =
            BatchStatement(
                //language=PostgreSQL
                """
                UPDATE opplysning SET fjernet=TRUE WHERE id=:id
                """.trimIndent(),
                fjernet.map { opplysning ->
                    mapOf("id" to opplysning.id)
                },
            )

        @WithSpan
        private fun lagreErstatter(opplysninger: List<Opplysning<*>>) =
            BatchStatement(
                //language=PostgreSQL
                """
                INSERT INTO opplysning_erstatter (opplysning_id, erstatter_id) 
                VALUES (:opplysning_id, :erstatter_id)
                ON CONFLICT DO NOTHING
                """.trimIndent(),
                opplysninger.mapNotNull { opplysning ->
                    if (opplysning.erstatter == null) return@mapNotNull null
                    mapOf(
                        "opplysning_id" to opplysning.id,
                        "erstatter_id" to opplysning.erstatter!!.id,
                    )
                },
            )

        @WithSpan
        private fun batchVerdi(opplysninger: List<Opplysning<*>>): BatchStatement {
            val defaultVerdi =
                mapOf(
                    "verdi_heltall" to null,
                    "verdi_desimaltall" to null,
                    "verdi_dato" to null,
                    "verdi_boolsk" to null,
                    "verdi_string" to null,
                )
            return BatchStatement(
                //language=PostgreSQL
                """
                INSERT INTO opplysning_verdi (opplysning_id, datatype, verdi_heltall, verdi_desimaltall, verdi_dato, verdi_boolsk, verdi_string, verdi_jsonb) 
                VALUES (:opplysning_id, :datatype, :verdi_heltall, :verdi_desimaltall, :verdi_dato, :verdi_boolsk, :verdi_string, :verdi_jsonb)
                ON CONFLICT DO NOTHING
                """.trimIndent(),
                opplysninger.map {
                    val datatype = it.opplysningstype.datatype
                    val (kolonne, data) = verdiKolonne(datatype, it.verdi)
                    val verdi = defaultVerdi + mapOf(kolonne to data)
                    mapOf(
                        "kolonne" to kolonne,
                        "opplysning_id" to it.id,
                        "datatype" to datatype.navn(),
                    ) + verdi
                },
            )
        }

        private fun verdiKolonne(
            datatype: Datatype<*>,
            verdi: Any,
        ) = when (datatype) {
            Boolsk -> Pair("verdi_boolsk", verdi)
            Dato -> Pair("verdi_dato", tilPostgresqlTimestamp(verdi as LocalDate))
            Desimaltall -> Pair("verdi_desimaltall", verdi)
            Heltall -> Pair("verdi_heltall", verdi)
            ULID -> Pair("verdi_string", (verdi as Ulid).verdi)
            Penger -> Pair("verdi_string", (verdi as Beløp).toString())
            Tekst -> Pair("verdi_string", verdi)
            BarnDatatype ->
                Pair(
                    "verdi_jsonb",
                    (verdi as BarnListe).let {
                        PGobject().apply {
                            type = "jsonb"
                            value = serdeBarn.toJson(it)
                        }
                    },
                )

            InntektDataType ->
                Pair(
                    "verdi_jsonb",
                    (verdi as Inntekt).verdi.let {
                        PGobject().apply {
                            type = "jsonb"
                            value = serdeInntekt.toJson(it)
                        }
                    },
                )

            PeriodeDataType ->
                Pair(
                    "verdi_jsonb",
                    (verdi as Periode).let {
                        PGobject().apply {
                            type = "jsonb"
                            value = serdePeriode.toJson(it)
                        }
                    },
                )
        }

        private fun tilPostgresqlTimestamp(verdi: LocalDate) =
            when {
                verdi.isEqual(LocalDate.MIN) ->
                    PGobject().apply {
                        type = "timestamp"
                        value = "-infinity"
                    }

                verdi.isEqual(LocalDate.MAX) ->
                    PGobject().apply {
                        type = "timestamp"
                        value = "infinity"
                    }

                else -> verdi
            }
    }
}

@Suppress("UNCHECKED_CAST")
private fun Collection<OpplysningRad<*>>.somOpplysninger(): List<Opplysning<*>> {
    val opplysningMap = mutableMapOf<UUID, Opplysning<*>>()

    // Finn opplysningen som erstattes av denne
    fun <T : Comparable<T>> OpplysningRad<T>.finnErstatter() {
        this.erstatter?.let {
            require(opplysningMap.contains(it)) { "Opplysning ${this.id} trenger id $it er ikke funnet" }
            (opplysningMap[this.id] as Opplysning<T>).erstatter(opplysningMap[it] as Opplysning<T>)
        }
    }

    fun <T : Comparable<T>> OpplysningRad<T>.toOpplysning(): Opplysning<*> {
        // If the Opplysning instance has already been created, return it
        opplysningMap[id]?.let { return it }

        // Create the Utledning instance if necessary
        val utledetAv =
            utledetAv?.let { utledetAv ->
                Utledning(
                    utledetAv.regel,
                    utledetAv.opplysninger.mapNotNull { opplysningId ->
                        opplysningMap[opplysningId] ?: this@somOpplysninger.find { it.id == opplysningId }?.toOpplysning()
                    },
                )
            }

        // Create the Opplysning instance
        val opplysning =
            when (status) {
                "Hypotese" ->
                    Hypotese(
                        id = id,
                        opplysningstype = opplysningstype,
                        verdi = verdi,
                        gyldighetsperiode = gyldighetsperiode,
                        utledetAv = utledetAv,
                        kilde = kilde,
                        opprettet = opprettet,
                        skalLagres = false,
                    )

                "Faktum" ->
                    Faktum(
                        id = id,
                        opplysningstype = opplysningstype,
                        verdi = verdi,
                        gyldighetsperiode = gyldighetsperiode,
                        utledetAv = utledetAv,
                        kilde = kilde,
                        opprettet = opprettet,
                        skalLagres = false,
                    )

                else -> throw IllegalStateException("Ukjent opplysningstype")
            }

        // Add the Opplysning instance to the map and return it
        opplysningMap[id] = opplysning
        return opplysning
    }

    // Convert all OpplysningRad instances to Opplysning instances
    val alleOpplysninger = this.map { it.toOpplysning() }
    this.forEach {
        it.finnErstatter()
    }
    return alleOpplysninger
}

private data class UtledningRad(
    val regel: String,
    val opplysninger: List<UUID>,
)

private data class OpplysningRad<T : Comparable<T>>(
    val opplysingerId: UUID,
    val id: UUID,
    val opplysningstype: Opplysningstype<T>,
    val verdi: T,
    val status: String,
    val gyldighetsperiode: Gyldighetsperiode,
    val utledetAv: UtledningRad? = null,
    val kildeId: UUID? = null,
    val kilde: Kilde? = null,
    val opprettet: LocalDateTime,
    val erstatter: UUID? = null,
)
