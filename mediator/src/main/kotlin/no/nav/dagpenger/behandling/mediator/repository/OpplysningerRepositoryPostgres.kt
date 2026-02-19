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

        private val serdeBarn = objectMapper.serde<BarnListe>()

        // Legacy serde for å kunne lese gamle verdier lagret med gammel struktur
        private val legazySerdeBarn = objectMapper.serde<List<Barn>>()
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
            val rader: Set<OpplysningRad<*>> =
                session
                    .run(
                        queryOf(
                            //language=PostgreSQL
                            """
                            with recursive 
                                opplysningskjede as (
                                    -- ankeropplysninger
                                    select id, utledet_av_id, erstatter_id
                                    from opplysningstabell
                                    where opplysninger_id = :id
                                    
                                    union
                                    -- rekursive opplysninger
                                    select o.id, o.utledet_av_id, o.erstatter_id
                                    from opplysningstabell o
                                    join opplysningskjede ok on o.id = any(ok.utledet_av_id) or o.id = ok.erstatter_id
                                )
                            select o.*
                            from opplysningstabell o where o.id in (select id from opplysningskjede)
                            order by o.id
                            """.trimIndent(),
                            mapOf("id" to opplysningerId),
                        ).map { row ->
                            val datatype = Datatype.fromString(row.string("datatype"))
                            row.somOpplysningRad(datatype)
                        }.asList,
                    ).toSet()

            // Hent inn kilde for alle opplysninger vi trenger
            val kilder = kildeRespository.hentKilder(rader.mapNotNull { it.kildeId }, session)
            val raderMedKilde =
                rader.map {
                    if (it.kildeId == null) return@map it
                    val kilde = kilder[it.kildeId] ?: throw IllegalStateException("Mangler kilde")
                    it.copy(kilde = kilde)
                }

            val raderFraTidligereOpplysninger = raderMedKilde.filterNot { it.opplysingerId == opplysningerId }.map { it.id }
            return raderMedKilde.somOpplysninger().filterNot { it.id in raderFraTidligereOpplysninger }
        }

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
                    fraOgMed = localDateOrNull("gyldig_fom") ?: LocalDate.MIN,
                    tilOgMed = localDateOrNull("gyldig_tom") ?: LocalDate.MAX,
                )
            val status = this.string("status")
            val verdi = datatype.verdi(this)
            val opprettet = this.localDateTime("opprettet")
            val utledetAvId = this.arrayOrNull<UUID>("utledet_av_id")?.toList() ?: emptyList()
            val utledetAv = this.stringOrNull("utledet_av")?.let { UtledningRad(it, utledetAvId, this.stringOrNull("utledet_versjon")) }
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
                Boolsk -> {
                    row.boolean("verdi_boolsk")
                }

                Dato -> {
                    when (row.string("verdi_dato")) {
                        "-infinity" -> LocalDate.MIN
                        "infinity" -> LocalDate.MAX
                        else -> row.localDate("verdi_dato")
                    }
                }

                Desimaltall -> {
                    row.double("verdi_desimaltall")
                }

                Heltall -> {
                    row.int("verdi_heltall")
                }

                ULID -> {
                    Ulid(row.string("verdi_string"))
                }

                Penger -> {
                    Beløp(row.string("verdi_string"))
                }

                Tekst -> {
                    row.string("verdi_string")
                }

                BarnDatatype -> {
                    val barneJsonNode = objectMapper.readTree(row.binaryStream("verdi_jsonb"))
                    runCatching {
                        serdeBarn.fromJson(
                            barneJsonNode,
                        )
                    }.getOrElse { BarnListe(barn = legazySerdeBarn.fromJson(barneJsonNode)) }
                }

                InntektDataType -> {
                    Inntekt(row.binaryStream("verdi_jsonb").use { serdeInntekt.fromJson(it) })
                }

                PeriodeDataType -> {
                    serdePeriode.fromJson(row.string("verdi_jsonb"))
                }
            } as T

        fun lagreOpplysninger(
            opplysninger: List<Opplysning<*>>,
            fjernet: Set<Opplysning<*>>,
        ) {
            kildeRespository.lagreKilder(opplysninger.mapNotNull { it.kilde }, session)
            batchOpplysninger(opplysninger).run(session)
            batchFjernet(fjernet).run(session)
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
                INSERT INTO opplysning_utledning (opplysning_id, regel, versjon) 
                VALUES (:opplysningId, :regel, :versjon)
                ON CONFLICT DO NOTHING
                """.trimIndent(),
                opplysninger.mapNotNull { opplysningSomBleUtledet ->
                    opplysningSomBleUtledet.utledetAv?.let { utledning ->
                        mapOf(
                            "opplysningId" to opplysningSomBleUtledet.id,
                            "regel" to utledning.regel,
                            "versjon" to utledning.versjon,
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
        private fun batchOpplysninger(opplysninger: List<Opplysning<*>>): BatchStatement {
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
                WITH ins AS (SELECT opplysningstype_id FROM opplysningstype WHERE uuid = :typeUuid AND datatype = :datatype)
                INSERT
                INTO opplysning (opplysninger_id, id, status, opplysningstype_id, kilde_id, gyldig_fom, gyldig_tom, opprettet, datatype,
                                 verdi_heltall, verdi_desimaltall, verdi_dato, verdi_boolsk, verdi_string, verdi_jsonb, erstatter_id)
                VALUES (:opplysningerId, :id, :status, (SELECT opplysningstype_id FROM ins), :kilde_id, :fom::timestamp,
                        :tom::timestamp, :opprettet, :datatype, :verdi_heltall, :verdi_desimaltall, :verdi_dato, :verdi_boolsk,
                        :verdi_string, :verdi_jsonb, :erstatter_id)
                ON CONFLICT(id) DO UPDATE SET erstatter_id=:erstatter_id
                """.trimIndent(),
                opplysninger.map { opplysning ->
                    val gyldighetsperiode: Gyldighetsperiode = opplysning.gyldighetsperiode

                    val datatype = opplysning.opplysningstype.datatype
                    val (kolonne, data) = verdiKolonne(datatype, opplysning.verdi)
                    val verdi = defaultVerdi + mapOf(kolonne to data)

                    mapOf(
                        "opplysningerId" to opplysningerId,
                        "id" to opplysning.id,
                        "status" to opplysning.javaClass.simpleName,
                        "typeUuid" to opplysning.opplysningstype.id.uuid,
                        "datatype" to opplysning.opplysningstype.datatype.navn(),
                        "kilde_id" to opplysning.kilde?.id,
                        "fom" to gyldighetsperiode.fraOgMed.takeUnless { it.isEqual(LocalDate.MIN) },
                        "tom" to gyldighetsperiode.tilOgMed.takeUnless { it.isEqual(LocalDate.MAX) },
                        "opprettet" to opplysning.opprettet,
                        "kolonne" to kolonne,
                        "opplysning_id" to opplysning.id,
                        "datatype" to datatype.navn(),
                        "erstatter_id" to opplysning.erstatter?.id,
                    ) + verdi
                },
            )
        }

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

        private fun verdiKolonne(
            datatype: Datatype<*>,
            verdi: Any,
        ) = when (datatype) {
            Boolsk -> {
                Pair("verdi_boolsk", verdi)
            }

            Dato -> {
                Pair("verdi_dato", tilPostgresqlTimestamp(verdi as LocalDate))
            }

            Desimaltall -> {
                Pair("verdi_desimaltall", verdi)
            }

            Heltall -> {
                Pair("verdi_heltall", verdi)
            }

            ULID -> {
                Pair("verdi_string", (verdi as Ulid).verdi)
            }

            Penger -> {
                Pair("verdi_string", (verdi as Beløp).toString())
            }

            Tekst -> {
                Pair("verdi_string", verdi)
            }

            BarnDatatype -> {
                Pair(
                    "verdi_jsonb",
                    (verdi as BarnListe).let {
                        PGobject().apply {
                            type = "jsonb"
                            value = serdeBarn.toJson(it)
                        }
                    },
                )
            }

            InntektDataType -> {
                Pair(
                    "verdi_jsonb",
                    (verdi as Inntekt).verdi.let {
                        PGobject().apply {
                            type = "jsonb"
                            value = serdeInntekt.toJson(it)
                        }
                    },
                )
            }

            PeriodeDataType -> {
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
        }

        private fun tilPostgresqlTimestamp(verdi: LocalDate) =
            when {
                verdi.isEqual(LocalDate.MIN) -> {
                    PGobject().apply {
                        type = "timestamp"
                        value = "-infinity"
                    }
                }

                verdi.isEqual(LocalDate.MAX) -> {
                    PGobject().apply {
                        type = "timestamp"
                        value = "infinity"
                    }
                }

                else -> {
                    verdi
                }
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
                    utledetAv.versjon,
                )
            }

        // Create the Opplysning instance
        val opplysning =
            when (status) {
                "Hypotese" -> {
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
                }

                "Faktum" -> {
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
                }

                else -> {
                    throw IllegalStateException("Ukjent opplysningstype")
                }
            }

        // Add the Opplysning instance to the map and return it
        opplysningMap[id] = opplysning
        return opplysning
    }

    // Convert all OpplysningRad instances to Opplysning instances
    val alleOpplysninger = this.map { it.toOpplysning() }
    this.forEach { it.finnErstatter() }

    return alleOpplysninger
}

private data class UtledningRad(
    val regel: String,
    val opplysninger: List<UUID>,
    val versjon: String? = null,
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
