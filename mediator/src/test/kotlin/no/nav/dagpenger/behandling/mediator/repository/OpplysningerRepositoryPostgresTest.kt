package no.nav.dagpenger.behandling.mediator.repository

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.TestOpplysningstyper.barn
import no.nav.dagpenger.behandling.TestOpplysningstyper.baseOpplysningstype
import no.nav.dagpenger.behandling.TestOpplysningstyper.beløpA
import no.nav.dagpenger.behandling.TestOpplysningstyper.beløpB
import no.nav.dagpenger.behandling.TestOpplysningstyper.boolsk
import no.nav.dagpenger.behandling.TestOpplysningstyper.dato
import no.nav.dagpenger.behandling.TestOpplysningstyper.desimal
import no.nav.dagpenger.behandling.TestOpplysningstyper.heltall
import no.nav.dagpenger.behandling.TestOpplysningstyper.inntektA
import no.nav.dagpenger.behandling.TestOpplysningstyper.maksdato
import no.nav.dagpenger.behandling.TestOpplysningstyper.mindato
import no.nav.dagpenger.behandling.TestOpplysningstyper.opplysningerRepository
import no.nav.dagpenger.behandling.TestOpplysningstyper.periode
import no.nav.dagpenger.behandling.TestOpplysningstyper.tekst
import no.nav.dagpenger.behandling.TestOpplysningstyper.utledetOpplysningstype
import no.nav.dagpenger.behandling.april
import no.nav.dagpenger.behandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder
import no.nav.dagpenger.behandling.mai
import no.nav.dagpenger.behandling.objectMapper
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.LesbarOpplysninger.Filter.Egne
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.Saksbehandler
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.opplysning.ULID
import no.nav.dagpenger.opplysning.Utledning
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.alle
import no.nav.dagpenger.opplysning.regel.enAv
import no.nav.dagpenger.opplysning.regel.innhentes
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.opplysning.verdier.Barn
import no.nav.dagpenger.opplysning.verdier.BarnListe
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.Inntekt
import no.nav.dagpenger.opplysning.verdier.Periode
import no.nav.dagpenger.opplysning.verdier.Ulid
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlin.system.measureTimeMillis

class OpplysningerRepositoryPostgresTest {
    @Test
    fun `lagrer enkle opplysninger`() {
        withMigratedDb {
            val repo = opplysningerRepository()
            val heltallFaktum = Faktum(heltall, 10)
            val kildeA = Saksbehandlerkilde(UUIDv7.ny(), Saksbehandler("foo"))
            val boolskFaktum = Faktum(boolsk, true, kilde = kildeA)
            val kildeB = Saksbehandlerkilde(UUIDv7.ny(), Saksbehandler("bar"))
            val datoFaktum = Faktum(dato, LocalDate.now(), kilde = kildeB)
            val desimalltallFaktum = Faktum(desimal, 5.5, kilde = kildeB)
            val tekstFaktum = Faktum(tekst, "Dette er en tekst")
            val periode = Faktum(periode, Periode(LocalDate.now(), LocalDate.now().plusDays(1)))
            val barn =
                Faktum(
                    barn,
                    BarnListe(
                        listOf(
                            Barn(
                                fødselsdato = 1.april(2010),
                                fornavnOgMellomnavn = "fornavn",
                                etternavn = "etternavn",
                                statsborgerskap = "NOR",
                                kvalifiserer = true,
                            ),
                        ),
                    ),
                )
            val opplysninger = Opplysninger.med(heltallFaktum, boolskFaktum, datoFaktum, desimalltallFaktum, tekstFaktum, barn, periode)
            repo.lagreOpplysninger(opplysninger)

            val fraDb =
                repo.hentOpplysninger(opplysninger.id).also {
                    Regelkjøring(LocalDate.now(), it)
                }
            fraDb.somListe().size shouldBe opplysninger.somListe().size
            fraDb.finnOpplysning(heltallFaktum.opplysningstype).verdi shouldBe heltallFaktum.verdi
            fraDb.finnOpplysning(boolskFaktum.opplysningstype).verdi shouldBe boolskFaktum.verdi
            fraDb.finnOpplysning(boolskFaktum.opplysningstype).kilde?.id shouldBe kildeA.id
            fraDb.finnOpplysning(datoFaktum.opplysningstype).verdi shouldBe datoFaktum.verdi
            fraDb.finnOpplysning(datoFaktum.opplysningstype).kilde?.id shouldBe kildeB.id
            fraDb.finnOpplysning(tekstFaktum.opplysningstype).verdi shouldBe tekstFaktum.verdi
            fraDb.finnOpplysning(barn.opplysningstype).verdi shouldBe barn.verdi
            fraDb.finnOpplysning(periode.opplysningstype).verdi shouldBe periode.verdi

            fraDb.finnOpplysning(desimalltallFaktum.opplysningstype).verdi shouldBe desimalltallFaktum.verdi
        }
    }

    @Test
    fun `lagre opplysningens gyldighetsperiode`() {
        withMigratedDb {
            val repo = opplysningerRepository()
            val gyldighetsperiode1 = Gyldighetsperiode(LocalDate.now(), LocalDate.now().plusDays(14))
            val faktum1 = Faktum(heltall, 10, gyldighetsperiode1)
            val opplysninger = Opplysninger.med(faktum1)
            repo.lagreOpplysninger(opplysninger)
            val fraDb =
                repo.hentOpplysninger(opplysninger.id).also {
                    Regelkjøring(LocalDate.now(), it)
                }
            fraDb.finnOpplysning(faktum1.opplysningstype).gyldighetsperiode shouldBe gyldighetsperiode1
        }
    }

    @Test
    fun `lagrer grenseverdier for dato opplysninger`() {
        withMigratedDb {
            val repo = opplysningerRepository()
            val kilde = Saksbehandlerkilde(UUIDv7.ny(), Saksbehandler("foo"))
            val maksDatoFaktum = Faktum(maksdato, LocalDate.MAX, kilde = kilde)
            val minDatoFaktum = Faktum(mindato, LocalDate.MIN, kilde = kilde)
            val opplysninger = Opplysninger.med(maksDatoFaktum, minDatoFaktum)
            repo.lagreOpplysninger(opplysninger)

            val fraDb =
                repo.hentOpplysninger(opplysninger.id).also {
                    Regelkjøring(LocalDate.now(), it)
                }
            fraDb.finnOpplysning(maksDatoFaktum.opplysningstype).verdi shouldBe maksDatoFaktum.verdi
            fraDb.finnOpplysning(minDatoFaktum.opplysningstype).verdi shouldBe minDatoFaktum.verdi
        }
    }

    @Test
    @Disabled("Modellen støtter ikke å bruke opplysninger med samme navn og ulik type")
    fun `lagrer opplysninger med samme navn og ulik type`() {
        withMigratedDb {
            val repo = opplysningerRepository()
            val opplysningstype = Opplysningstype.ulid(Opplysningstype.Id(UUIDv7.ny(), ULID), "Ulid")
            val opplysningstype1 = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "Ulid")

            val ulidFaktum = Faktum(opplysningstype, Ulid("01E5Z6Z1Z1Z1Z1Z1Z1Z1Z1Z1Z1"))
            val ulidBoolskFaktum = Faktum(opplysningstype1, false)

            val opplysninger = Opplysninger.med(ulidFaktum, ulidBoolskFaktum)
            repo.lagreOpplysninger(opplysninger)

            val fraDb =
                repo.hentOpplysninger(opplysninger.id).also {
                    Regelkjøring(LocalDate.now(), it)
                }
            fraDb.finnOpplysning(opplysningstype).verdi shouldBe ulidFaktum.verdi
            fraDb.finnOpplysning(opplysningstype1).verdi shouldBe ulidBoolskFaktum.verdi
        }
    }

    @Test
    fun `lagrer opplysninger med utledning`() {
        withMigratedDb {
            val repo = opplysningerRepository()

            val baseOpplysning = Faktum(baseOpplysningstype, LocalDate.now())

            val regelsett =
                vilkår("vilkår") {
                    regel(baseOpplysningstype) {
                        innhentes
                    }
                    regel(utledetOpplysningstype) {
                        oppslag(baseOpplysningstype) { 5 }
                    }
                }
            val opplysninger = Opplysninger()
            val regelkjøring = Regelkjøring(LocalDate.now(), opplysninger, regelsett)
            opplysninger.leggTil(baseOpplysning as Opplysning<*>).also { regelkjøring.evaluer() }

            repo.lagreOpplysninger(opplysninger)

            val fraDb = repo.hentOpplysninger(opplysninger.id).also { Regelkjøring(LocalDate.now(), it) }
            fraDb.somListe().size shouldBe opplysninger.somListe().size

            with(fraDb.finnOpplysning(utledetOpplysningstype)) {
                verdi shouldBe 5
                utledetAv.shouldNotBeNull()
                utledetAv!!.regel shouldBe "Oppslag"
                utledetAv!!.opplysninger shouldContainExactly listOf(baseOpplysning)
            }
            with(fraDb.finnOpplysning(baseOpplysning.id)) {
                id shouldBe baseOpplysning.id
                verdi shouldBe baseOpplysning.verdi
                gyldighetsperiode shouldBe baseOpplysning.gyldighetsperiode
                opplysningstype shouldBe baseOpplysning.opplysningstype
                utledetAv.shouldBeNull()
            }
        }
    }

    @Test
    fun `Klarer å lagre store mengder opplysninger effektivt`() {
        withMigratedDb {
            val repo = opplysningerRepository()
            val fakta =
                (1..50000).map {
                    val fomTom = LocalDate.now().minusDays(it.toLong())
                    Faktum(desimal, it.toDouble(), Gyldighetsperiode(fomTom, fomTom))
                }
            val opplysninger = Opplysninger.med(fakta)

            val tidBrukt = measureTimeMillis { repo.lagreOpplysninger(opplysninger) }
            tidBrukt shouldBeLessThan 5555

            val fraDb = repo.hentOpplysninger(opplysninger.id)
            fraDb.somListe().size shouldBe fakta.size
        }
    }

    @Test
    fun `skriver over erstattet opplysning i samme Opplysninger`() {
        withMigratedDb {
            val repo = opplysningerRepository()
            val opplysning = Faktum(heltall, 10)
            val opplysningErstattet = Faktum(heltall, 20)
            val opplysninger = Opplysninger.med(opplysning)
            val regelkjøring = Regelkjøring(LocalDate.now(), opplysninger)

            repo.lagreOpplysninger(opplysninger)
            opplysninger.leggTil(opplysningErstattet as Opplysning<*>).also { regelkjøring.evaluer() }
            repo.lagreOpplysninger(opplysninger)
            val fraDb =
                repo.hentOpplysninger(opplysninger.id).also {
                    Regelkjøring(LocalDate.now(), it)
                }
            fraDb.somListe(LesbarOpplysninger.Filter.Egne) shouldContainExactly listOf(opplysningErstattet)
            fraDb.finnOpplysning(heltall).verdi shouldBe opplysningErstattet.verdi
        }
    }

    @Test
    fun `kan erstatte opplysning i tidligere Opplysninger`() {
        withMigratedDb {
            val repo = opplysningerRepository()

            // Lag opplysninger med opprinnelig opplysning
            val opplysning = Faktum(heltall, 10)
            val opprinneligOpplysninger = Opplysninger.med(opplysning)
            repo.lagreOpplysninger(opprinneligOpplysninger)

            // Lag ny opplysninger med erstattet opplysning
            val opplysningerSomErstatter = Opplysninger.basertPå(opprinneligOpplysninger)
            val opplysningErstattet = Faktum(heltall, 20)
            opplysningerSomErstatter.leggTil(opplysningErstattet as Opplysning<*>)
            repo.lagreOpplysninger(opplysningerSomErstatter)

            // Verifiser
            opprinneligOpplysninger.somListe(LesbarOpplysninger.Filter.Egne) shouldContainExactly listOf(opplysning)
            val opprinneligFraDb = repo.hentOpplysninger(opprinneligOpplysninger.id)
            opprinneligFraDb.somListe(LesbarOpplysninger.Filter.Egne) shouldContainExactly
                opprinneligOpplysninger.somListe(
                    LesbarOpplysninger.Filter.Egne,
                )

            val fraDb: Opplysninger =
                // Simulerer hvordan Behandling setter opp Opplysninger
                repo.hentOpplysninger(opplysningerSomErstatter.id).baserPå(opprinneligFraDb)

            fraDb.somListe(LesbarOpplysninger.Filter.Egne) shouldContainExactly
                opplysningerSomErstatter.somListe(LesbarOpplysninger.Filter.Egne)
            fraDb
                .forDato(10.mai)
                .finnOpplysning(heltall)
                .verdi shouldBe opplysningErstattet.verdi

            // TODO: Noe muffens oppstod i arbeidet rundt erstatning
            fraDb
                .forDato(10.mai)
                .finnOpplysning(heltall)
                .erstatter shouldBe opplysning
        }
    }

    @Test
    fun `lagrer opplysninger med utledning fra tidligere opplysninger`() {
        withMigratedDb {
            val repo = opplysningerRepository()

            val baseOpplysning = Faktum(baseOpplysningstype, LocalDate.now())

            val regelsett =
                vilkår("vilkår") {
                    regel(baseOpplysningstype) { innhentes }
                    regel(utledetOpplysningstype) { oppslag(baseOpplysningstype) { 5 } }
                }
            val tidligereOpplysninger = Opplysninger()
            val regelkjøring = Regelkjøring(LocalDate.now(), tidligereOpplysninger, regelsett)

            tidligereOpplysninger.leggTil(baseOpplysning as Opplysning<*>).also { regelkjøring.evaluer() }

            repo.lagreOpplysninger(tidligereOpplysninger)

            val nyeOpplysninger = Opplysninger.basertPå(tidligereOpplysninger)
            val nyRegelkjøring = Regelkjøring(LocalDate.now(), nyeOpplysninger, regelsett)
            val endretBaseOpplysningstype = Faktum(baseOpplysningstype, LocalDate.now().plusDays(1))
            nyeOpplysninger.leggTil(endretBaseOpplysningstype as Opplysning<*>).also { nyRegelkjøring.evaluer() }
            repo.lagreOpplysninger(nyeOpplysninger)

            val fraDb = repo.hentOpplysninger(nyeOpplysninger.id)
            fraDb.somListe().size shouldBe 2

            with(fraDb.finnOpplysning(utledetOpplysningstype)) {
                verdi shouldBe 5
                utledetAv.shouldNotBeNull()
                utledetAv!!.regel shouldBe "Oppslag"
                utledetAv!!.opplysninger shouldContainExactly listOf(endretBaseOpplysningstype)
            }
            with(fraDb.finnOpplysning(endretBaseOpplysningstype.id)) {
                id shouldBe endretBaseOpplysningstype.id
                verdi shouldBe endretBaseOpplysningstype.verdi
                gyldighetsperiode shouldBe endretBaseOpplysningstype.gyldighetsperiode
                opplysningstype shouldBe endretBaseOpplysningstype.opplysningstype
                utledetAv.shouldBeNull()
            }

            val tidligereOpplysningerFraDb = repo.hentOpplysninger(tidligereOpplysninger.id)
            // tidligereOpplysningerFraDb.finnAlle().utenErstattet().size shouldBe 0
            tidligereOpplysningerFraDb.somListe(Egne).size shouldBe 2
            with(tidligereOpplysninger.somListe(Egne).find { it.id == baseOpplysning.id }) {
                this.shouldNotBeNull()
                this.verdi shouldBe baseOpplysning.verdi
                this.gyldighetsperiode shouldBe baseOpplysning.gyldighetsperiode
                this.opplysningstype shouldBe baseOpplysning.opplysningstype
                this.utledetAv.shouldBeNull()
            }
        }
    }

    @Test
    fun `lagrer penger som BigDecimal med riktig presisjon`() {
        withMigratedDb {
            val repo = opplysningerRepository()

            val verdi = "10.00000000000000000006"
            val verdi1 = BigDecimal(verdi)
            val beløpFaktumA = Faktum(beløpA, Beløp(verdi1))
            val beløpFaktumB = Faktum(beløpB, Beløp("EUR 20"))

            val opplysninger = Opplysninger.med(beløpFaktumA, beløpFaktumB)
            repo.lagreOpplysninger(opplysninger)

            val fraDb =
                repo.hentOpplysninger(opplysninger.id).also {
                    Regelkjøring(LocalDate.now(), it)
                }

            fraDb.somListe().size shouldBe opplysninger.somListe().size
            val beløpAFraDB = fraDb.finnOpplysning(beløpFaktumA.opplysningstype)
            beløpAFraDB.verdi shouldBe beløpFaktumA.verdi
            beløpAFraDB.verdi.toString() shouldBe "NOK $verdi"

            val beløpBFraDB = fraDb.finnOpplysning(beløpFaktumB.opplysningstype)
            beløpBFraDB.verdi shouldBe beløpFaktumB.verdi
            beløpBFraDB.verdi.toString() shouldBe "EUR 20"
        }
    }

    @Test
    fun `kan lagre inntekt`() {
        withMigratedDb {
            val repo = opplysningerRepository()
            val inntektV1: no.nav.dagpenger.inntekt.v1.Inntekt =
                objectMapper.readValue(
                    this.javaClass.getResourceAsStream("/test-data/inntekt.json"),
                    no.nav.dagpenger.inntekt.v1.Inntekt::class.java,
                )
            val inntektFaktum =
                Faktum(
                    inntektA,
                    Inntekt(
                        inntektV1,
                    ),
                )
            val opplysninger = Opplysninger.med(inntektFaktum)
            repo.lagreOpplysninger(opplysninger)
            val fraDb = repo.hentOpplysninger(opplysninger.id)

            fraDb.finnOpplysning(inntektA).verdi.id shouldBe inntektFaktum.verdi.id
            fraDb
                .finnOpplysning(inntektA)
                .verdi.verdi.inntektsListe shouldBe inntektFaktum.verdi.verdi.inntektsListe
        }
    }

    @Test
    fun `kan fjerne opplysninger`() {
        withMigratedDb {
            val repo = opplysningerRepository()
            val vaktmesterRepo = VaktmesterPostgresRepo()
            val heltallFaktum = Faktum(heltall, 10)
            val heltallFaktum2 = Faktum(heltall, 20)
            val opplysninger = Opplysninger.med(heltallFaktum)
            // TODO: ???? heltallFaktum.erstattesAv(heltallFaktum2)
            repo.lagreOpplysninger(opplysninger)
            opplysninger.fjernHvis { it == heltallFaktum }
            repo.lagreOpplysninger(opplysninger)
            val fraDb = repo.hentOpplysninger(opplysninger.id)
            fraDb.somListe().shouldBeEmpty()
            vaktmesterRepo.slettOpplysninger() shouldContainExactly listOf(heltallFaktum.id)
        }
    }

    @Test
    fun `ikke slette mer enn vi skal fra tidligere opplysninger `() {
        withMigratedDb {
            val repo = opplysningerRepository()
            val vaktmesterRepo = VaktmesterPostgresRepo()

            // Gammel behandling
            val opprinneligDato = LocalDate.now()
            val baseOpplysning = Faktum(baseOpplysningstype, opprinneligDato)

            val regelsett =
                vilkår("vilkår") {
                    regel(baseOpplysningstype) { innhentes }
                    regel(utledetOpplysningstype) { oppslag(baseOpplysningstype) { 5 } }
                }
            val tidligereOpplysninger = Opplysninger()
            val regelkjøring = Regelkjøring(LocalDate.now(), tidligereOpplysninger, regelsett)

            tidligereOpplysninger.leggTil(baseOpplysning as Opplysning<*>).also { regelkjøring.evaluer() }
            repo.lagreOpplysninger(tidligereOpplysninger)

            // Ny behandling som baseres på den gamle
            val nyeOpplysninger = Opplysninger.basertPå(tidligereOpplysninger)
            val endretBaseOpplysningstype = Faktum(baseOpplysningstype, LocalDate.now().plusDays(1))
            nyeOpplysninger.leggTil(endretBaseOpplysningstype as Opplysning<*>).also {
                Regelkjøring(LocalDate.now(), nyeOpplysninger, regelsett).evaluer()
            }
            repo.lagreOpplysninger(nyeOpplysninger)

            // Hent lagrede opplysninger fra ny behandling
            val fraDb = repo.hentOpplysninger(nyeOpplysninger.id)
            fraDb.somListe().size shouldBe 2
            vaktmesterRepo.slettOpplysninger().shouldBeEmpty()
            val utledet = fraDb.finnOpplysning(utledetOpplysningstype)

            // Legg til endret opplysning i ny behandling
            val endretDato = LocalDate.now().plusDays(2)
            fraDb.leggTil(Faktum(baseOpplysningstype, endretDato, Gyldighetsperiode(LocalDate.now().minusDays(1)))).also {
                Regelkjøring(LocalDate.now(), fraDb, regelsett).evaluer()
            }

            repo.lagreOpplysninger(fraDb)

            // Slett opplysninger som er fjernet kun fra ny behandling
            vaktmesterRepo.slettOpplysninger().shouldContainExactly(utledet.id, endretBaseOpplysningstype.id)

            with(repo.hentOpplysninger(nyeOpplysninger.id)) {
                somListe().size shouldBe 2
                finnOpplysning(baseOpplysningstype).verdi shouldBe endretDato
            }

            with(repo.hentOpplysninger(tidligereOpplysninger.id)) {
                somListe().size shouldBe 2
                finnOpplysning(baseOpplysningstype).verdi shouldBe opprinneligDato
            }
        }
    }

    @Test
    fun `skal slette fjernede opplysninger som er utledet av i flere nivåer`() {
        withMigratedDb {
            val a = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "A")
            val b = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "B")
            val c = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "C")
            val d = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "D")
            val e = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "E")
            val f = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "F")
            val repo = opplysningerRepository()
            val vaktmesterRepo = VaktmesterPostgresRepo()

            val regelsett1 =
                vilkår("vilkår en") {
                    regel(a) { innhentes }
                    regel(d) { innhentes }
                    regel(b) { enAv(a) }
                    regel(c) { enAv(b, d) }
                }
            val regelsett2 =
                vilkår("vilkår to") {
                    regel(e) { enAv(c) }
                    regel(f) { alle(e, b, a) }
                }
            val opplysninger = Opplysninger()
            val regelkjøring = Regelkjøring(LocalDate.now(), opplysninger, regelsett1, regelsett2)

            val aFaktum = Faktum(a, true)
            val dFaktum = Faktum(d, false)
            opplysninger.leggTil(aFaktum)
            opplysninger.leggTil(dFaktum)
            regelkjøring.evaluer()

            repo.lagreOpplysninger(opplysninger)

            vaktmesterRepo.slettOpplysninger().shouldBeEmpty()

            val rehydrerteOpplysninger = repo.hentOpplysninger(opplysninger.id)
            // Endre opplysning a slik at b og c blir endret (og det originale blir fjernet)
            val endretAFaktum = Faktum(a, false)
            val regelkjøring2 = Regelkjøring(LocalDate.now(), rehydrerteOpplysninger, regelsett1, regelsett2)
            rehydrerteOpplysninger.leggTil(endretAFaktum).also { regelkjøring2.evaluer() }
            val forventetfjernet: Set<UUID> = rehydrerteOpplysninger.fjernet().map { it.id }.toSet()
            repo.lagreOpplysninger(rehydrerteOpplysninger)

            val slettedeOpplysninger: Set<UUID> = vaktmesterRepo.slettOpplysninger().toSet()
            slettedeOpplysninger.size shouldBe forventetfjernet.size

            slettedeOpplysninger shouldContainAll forventetfjernet
        }
    }

    @Test
    fun `Sletter flere sett med opplysninger`() {
        withMigratedDb {
            val vaktmesterRepo = VaktmesterPostgresRepo()

            val a = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "A")
            val b = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "B")
            val c = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "C")
            val d = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "D")

            val repo = opplysningerRepository()

            val regelsett =
                vilkår("vilkår") {
                    regel(a) { innhentes }
                    regel(d) { innhentes }
                    regel(b) { enAv(a) }
                    regel(c) { enAv(b, d) }
                }
            val opplysninger = Opplysninger()
            val regelkjøring = Regelkjøring(LocalDate.now(), opplysninger, regelsett)

            val aFaktum = Faktum(a, true)
            val dFaktum = Faktum(d, false)
            opplysninger.leggTil(aFaktum)
            opplysninger.leggTil(dFaktum)
            regelkjøring.evaluer()

            repo.lagreOpplysninger(opplysninger)

            // ----
            val opplysninger2 = Opplysninger()
            val regelkjøring2 = Regelkjøring(LocalDate.now(), opplysninger2, regelsett)

            val qFaktum = Faktum(a, true)
            val wFaktum = Faktum(d, false)
            opplysninger2.leggTil(qFaktum)
            opplysninger2.leggTil(wFaktum)
            regelkjøring2.evaluer()

            repo.lagreOpplysninger(opplysninger2)

            // -----

            val endretAFaktum = Faktum(a, false)
            opplysninger.leggTil(endretAFaktum).also { regelkjøring.evaluer() }
            repo.lagreOpplysninger(opplysninger)

            // ------

            val endretQFaktum = Faktum(a, false)
            opplysninger2.leggTil(endretQFaktum).also { regelkjøring2.evaluer() }
            repo.lagreOpplysninger(opplysninger2)

            vaktmesterRepo.slettOpplysninger(antallBehandlinger = 10).size shouldBe 6
        }
    }

    @Test
    @Disabled("Fungerer ikke etter repository er refaktorert")
    fun `migrering reduserer normalisering`() {
        withMigratedDb("76") {
            val repo = opplysningerRepository()
            val beløp = Faktum(beløpA, Beløp("NOK 100.00"))
            val fakta =
                listOf(
                    Faktum(boolsk, true),
                    Faktum(desimal, 0.2),
                    beløp,
                    Faktum(beløpB, Beløp("NOK 100.00"), utledetAv = Utledning("regel", listOf(beløp), "versjon 1")),
                )
            val opplysninger = Opplysninger.med(fakta)
            repo.lagreOpplysninger(opplysninger)

            fun verifiserTilstand() {
                repo.hentOpplysninger(opplysninger.id).also { opplysninger ->
                    fakta.forEach {
                        opplysninger.finnOpplysning(it.opplysningstype).verdi shouldBe it.verdi
                    }

                    opplysninger.finnOpplysning(beløpB).utledetAv?.regel shouldBe "regel"
                }

                // Opplysninger med tilfeldig UUID gir uansett et sett opplysninger som er tomt
                repo.hentOpplysninger(UUIDv7.ny()).somListe().shouldBeEmpty()
            }

            verifiserTilstand()

            // Kjør migrering uten link-tabellen opplysninger_opplysning
            PostgresDataSourceBuilder.runMigrationTo("77")

            verifiserTilstand()
        }
    }
}
