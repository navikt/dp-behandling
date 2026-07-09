package no.nav.dagpenger.mediator.repository

import io.github.oshai.kotlinlogging.KotlinLogging
import io.opentelemetry.instrumentation.annotations.WithSpan
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.dagpenger.mediator.db.DatabaseSession
import no.nav.dagpenger.mediator.objectMapper
import no.nav.dagpenger.mediator.repository.JsonSerde.Companion.serde
import no.nav.dagpenger.mediator.tilInntektV1
import no.nav.dagpenger.mediator.tilJsonNode
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
import no.nav.dagpenger.opplysning.OpplysningstypeRegister
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

internal class OpplysningerRepositoryPostgres(
    private val dbSession: DatabaseSession,
    private val kildeRepository: KildeRepository,
    private val opplysningstypeRegister: OpplysningstypeRegister = OpplysningstypeRegister.tom,
) : OpplysningerRepository {
    internal companion object {
        private val logger = KotlinLogging.logger { }

        fun Session.hentOpplysninger(
            kildeRepository: KildeRepository,
            opplysningstypeRegister: OpplysningstypeRegister,
            opplysningerId: UUID,
        ) = hentOpplysninger(kildeRepository, opplysningstypeRegister, setOf(opplysningerId))
            .values
            .singleOrNull()
            ?: Opplysninger.rehydrer(opplysningerId, emptyList())

        fun Session.hentOpplysninger(
            kildeRepository: KildeRepository,
            opplysningstypeRegister: OpplysningstypeRegister,
            opplysningerIder: Set<UUID>,
        ) = OpplysningRepository(this, kildeRepository, opplysningstypeRegister)
            .hentOpplysninger(opplysningerIder)
            .mapValues { (opplysningerId, opplysninger) ->
                Opplysninger.rehydrer(opplysningerId, opplysninger)
            }

        private val serdeBarn = objectMapper.serde<BarnListe>()

        // Legacy serde for å kunne lese gamle verdier lagret med gammel struktur
        private val legazySerdeBarn = objectMapper.serde<List<Barn>>()
        private val serdePeriode = objectMapper.serde<Periode>()
    }

    override fun hentOpplysninger(opplysningerId: UUID) =
        dbSession.session { session -> session.hentOpplysninger(kildeRepository, opplysningstypeRegister, opplysningerId) }

    override fun lagreOpplysninger(opplysninger: Opplysninger) {
        dbSession.transaction {
            lagreOpplysninger(listOf(opplysninger), this)
        }
    }

    override fun lagreOpplysninger(
        opplysninger: List<Opplysninger>,
        unitOfWork: PostgresUnitOfWork,
    ) {
        val params =
            opplysninger.map {
                mapOf("opplysningerId" to it.id)
            }

        unitOfWork.session.batchPreparedNamedStatement(
            //language=PostgreSQL
            """
            INSERT INTO opplysninger (opplysninger_id) VALUES (:opplysningerId) ON CONFLICT DO NOTHING
            """.trimIndent(),
            params,
        )

        val opplysningerSomSkalLagres =
            opplysninger.map {
                val somListe = it.somListe(LesbarOpplysninger.Filter.Egne)
                it.id to somListe.filter { opplysning -> opplysning.skalLagres }
            }

        val fjernet = opplysninger.flatMap { it.fjernet() }.toSet()

        OpplysningRepository(unitOfWork.session, kildeRepository, opplysningstypeRegister).lagreOpplysninger(
            opplysningerSomSkalLagres,
            fjernet,
        )
    }

    override fun lagreOpplysningstyper(opplysningstyper: Collection<Opplysningstype<*>>) =
        dbSession.session { session ->
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
        private val session: Session,
        private val kildeRespository: KildeRepository,
        private val opplysningstypeRegister: OpplysningstypeRegister,
    ) {
        fun hentOpplysninger(opplysningerIder: Set<UUID>): Map<UUID, List<Opplysning<out Any>>> {
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
                                    where opplysninger_id = ANY(:opplysninger_ider)
                                    
                                    union all
                                    -- rekursive opplysninger. Kandidatene (utledet_av_id og erstatter_id) samles i én
                                    -- lateral-subquery slik at rekursjonen kun refererer opplysningskjede ett sted,
                                    -- og join'en mot opplysningstabell kan bruke indeks (id = cand.ref_id) i stedet
                                    -- for en OR-betingelse som tvinger nested loop uten indeksbruk.
                                    select o.id, o.utledet_av_id, o.erstatter_id
                                    from opplysningskjede ok
                                    cross join lateral (
                                        select unnest(ok.utledet_av_id) as ref_id
                                        union all
                                        select ok.erstatter_id
                                        where ok.erstatter_id is not null
                                    ) cand
                                    join opplysningstabell o on o.id = cand.ref_id
                                ),
                                kjede_ids as (select distinct id from opplysningskjede)
                            select o.*
                            from kjede_ids k
                            join opplysningstabell o on o.id = k.id
                            order by o.id
                            """.trimIndent(),
                            mapOf(
                                "opplysninger_ider" to
                                    session.connection.underlying.createArrayOf(
                                        "uuid",
                                        opplysningerIder.toTypedArray(),
                                    ),
                            ),
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

            // reverse-lookup map for å finne opplysningerId for hver opplysning
            val opplysningerIdForOpplysning =
                raderMedKilde
                    // bevarer bare opplysninger vi faktisk ønsker oss
                    .filter { it.opplysingerId in opplysningerIder }
                    .associate { it.id to it.opplysingerId }
            return raderMedKilde
                .somOpplysninger()
                .filter { it.id in opplysningerIdForOpplysning }
                .groupBy { opplysningerIdForOpplysning.getValue(it.id) }
        }

        private fun <T : Any> Row.somOpplysningRad(datatype: Datatype<T>): OpplysningRad<T> {
            val opplysingerId = uuid("opplysninger_id")
            val id = uuid("id")
            val typeUuid = uuid("type_uuid")

            val opplysningTypeId = Opplysningstype.Id(typeUuid, datatype)
            val opplysningstype: Opplysningstype<T> =
                opplysningstypeRegister[opplysningTypeId]
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
            val behandletVed = this.localDateOrNull("behandlet_ved")

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
                behandletVed = behandletVed,
            )
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T : Any> Datatype<T>.verdi(row: Row): T =
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
                    Inntekt(row.binaryStream("verdi_jsonb").use { objectMapper.readTree(it).tilInntektV1() })
                }

                PeriodeDataType -> {
                    serdePeriode.fromJson(row.string("verdi_jsonb"))
                }
            } as T

        fun lagreOpplysninger(
            opplysninger: List<Pair<UUID, List<Opplysning<*>>>>,
            fjernet: Set<Opplysning<*>>,
        ) {
            val kilder = opplysninger.flatMap { (_, opplysningerliste) -> opplysningerliste.mapNotNull { it.kilde } }
            kildeRespository.lagreKilder(kilder, session)
            batchOpplysninger(opplysninger)
            batchFjernet(fjernet)
            lagreUtledetAv(opplysninger.flatMap { it.second })
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
        private fun batchOpplysninger(opplysninger: List<Pair<UUID, List<Opplysning<*>>>>) {
            val defaultVerdi =
                mapOf(
                    "verdi_heltall" to null,
                    "verdi_desimaltall" to null,
                    "verdi_dato" to null,
                    "verdi_boolsk" to null,
                    "verdi_string" to null,
                )

            val params =
                opplysninger.flatMap { (opplysningerId, opplysningliste) ->
                    opplysningliste.map { opplysning ->
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
                            "fom" to gyldighetsperiode.fraOgMed.takeIf { gyldighetsperiode.harStartdato },
                            "tom" to gyldighetsperiode.tilOgMed.takeIf { gyldighetsperiode.harSluttdato },
                            "opprettet" to opplysning.opprettet,
                            "kolonne" to kolonne,
                            "opplysning_id" to opplysning.id,
                            "datatype" to datatype.navn(),
                            "erstatter_id" to opplysning.erstatter?.id,
                            "behandlet_ved" to opplysning.behandletVed,
                        ) + verdi
                    }
                }

            session
                .batchPreparedNamedStatement(
                    //language=PostgreSQL
                    """
                    WITH ins AS (SELECT opplysningstype_id FROM opplysningstype WHERE uuid = :typeUuid AND datatype = :datatype)
                    INSERT
                    INTO opplysning (opplysninger_id, id, status, opplysningstype_id, kilde_id, gyldig_fom, gyldig_tom, opprettet, datatype,
                                     verdi_heltall, verdi_desimaltall, verdi_dato, verdi_boolsk, verdi_string, verdi_jsonb, erstatter_id, behandlet_ved)
                    VALUES (:opplysningerId, :id, :status, (SELECT opplysningstype_id FROM ins), :kilde_id, :fom::timestamp,
                            :tom::timestamp, :opprettet, :datatype, :verdi_heltall, :verdi_desimaltall, :verdi_dato, :verdi_boolsk,
                            :verdi_string, :verdi_jsonb, :erstatter_id, :behandlet_ved)
                    ON CONFLICT(id) DO UPDATE SET erstatter_id=:erstatter_id, behandlet_ved=:behandlet_ved
                    """.trimIndent(),
                    params,
                ).krevAtAntallRaderErNøyaktigLik(params.size)
        }

        @WithSpan
        private fun batchFjernet(fjernet: Set<Opplysning<*>>) {
            val params =
                fjernet
                    .map { opplysning -> mapOf("id" to opplysning.id) }

            session
                .batchPreparedNamedStatement(
                    //language=PostgreSQL
                    """
                    UPDATE opplysning SET fjernet=TRUE WHERE id=:id
                    """.trimIndent(),
                    params,
                )
        }

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
                            value = it.tilJsonNode().toString()
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
private fun Collection<OpplysningRad<out Any>>.somOpplysninger(): List<Opplysning<out Any>> {
    val opplysningMap = mutableMapOf<UUID, Opplysning<out Any>>()

    // Finn opplysningen som erstattes av denne
    fun <T : Any> OpplysningRad<T>.finnErstatter() {
        this.erstatter?.let {
            require(opplysningMap.contains(it)) { "Opplysning ${this.id} trenger id $it er ikke funnet" }
            (opplysningMap[this.id] as Opplysning<T>).erstatter(opplysningMap[it] as Opplysning<T>)
        }
    }

    fun <T : Any> OpplysningRad<T>.toOpplysning(): Opplysning<out Any> {
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
        opplysning.behandletVed = behandletVed
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

private data class OpplysningRad<T : Any>(
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
    val behandletVed: LocalDate? = null,
)
