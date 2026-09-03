package no.nav.dagpenger.mediator

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaMethodCall
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import org.junit.jupiter.api.Test

class ArkitekturTest {
    private val klasser =
        ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .importPackages("no.nav.dagpenger")

    // -- Modulgrenser --

    @Test
    fun `ingen andre pakker skal avhenge av mediator`() {
        noClasses()
            .that()
            .resideOutsideOfPackage("..dagpenger.mediator..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..dagpenger.mediator..")
            .check(klasser)
    }

    // -- Infrastruktur-isolasjon --

    @Test
    fun `kun mediator skal bruke kotliquery`() {
        noClasses()
            .that()
            .resideOutsideOfPackage("..dagpenger.mediator..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("kotliquery..")
            .check(klasser)
    }

    @Test
    fun `kun mediator skal bruke Ktor`() {
        noClasses()
            .that()
            .resideOutsideOfPackage("..dagpenger.mediator..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("io.ktor..")
            .check(klasser)
    }

    // -- Transportuavhengighet --

    @Test
    fun `domenemodell og opplysninger skal ikke avhenge av Jackson`() {
        noClasses()
            .that()
            .resideInAnyPackage("..dagpenger.modell..", "..dagpenger.opplysning..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("com.fasterxml.jackson..", "tools.jackson..")
            .check(klasser)
    }

    // -- Navnekonvensjoner --

    @Test
    fun `RepositoryPostgres-klasser skal ligge i repository-pakken`() {
        classes()
            .that()
            .haveSimpleNameEndingWith("RepositoryPostgres")
            .and()
            .doNotHaveSimpleName("SakRepositoryPostgres")
            .should()
            .resideInAPackage("..repository..")
            .check(klasser)
    }

    // -- Sykliske avhengigheter --

    @Test
    fun `ingen sykliske avhengigheter mellom moduler`() {
        slices()
            .matching("no.nav.dagpenger.(*)..")
            .should()
            .beFreeOfCycles()
            .check(klasser)
    }

    // -- Typede Kafka-hendelser --

    @Test
    fun `nye Kafka-hendelser skal publiseres med typede DTOer, ikke JsonMessage newMessage direkte`() {
        // Kjent teknisk gjeld fra før dette mønsteret ble innført. Skal ikke øke — nye Kafka-
        // hendelser skal bruke toJsonMessage(eventName, dto) (se Vedtak.kt) med en DTO generert av
        // fabrikt fra en components/schemas-oppføring i openapi/src/main/resources/behandling-api.yaml.
        val kjenteUntak =
            setOf(
                "no.nav.dagpenger.mediator.AktivitetsloggMediator",
                "no.nav.dagpenger.mediator.FlyttSøskenObserver",
                "no.nav.dagpenger.mediator.HendelseMediator",
                "no.nav.dagpenger.mediator.PersonMediator",
                "no.nav.dagpenger.mediator.meldekort.MeldekortBehandlingskø",
                "no.nav.dagpenger.mediator.mottak.ArenaOppgaveMottak",
                "no.nav.dagpenger.mediator.mottak.InnsendingFerdigstiltMessage",
                "no.nav.dagpenger.mediator.mottak.MeldekortBehandlingsresultatKontrollregningMottak",
                "no.nav.dagpenger.mediator.repository.AvklaringKafkaObservatør",
            )

        val erIkkeKjentUnntak =
            DescribedPredicate.describe<JavaClass>("ikke et kjent unntak") { klasse ->
                klasse.fullName !in kjenteUntak
            }

        val kallerJsonMessageNewMessageDirekte =
            DescribedPredicate.describe<JavaMethodCall>("kaller JsonMessage.newMessage direkte") { kall ->
                kall.targetOwner.isEquivalentTo(JsonMessage.Companion::class.java) &&
                    kall.target.name.startsWith("newMessage")
            }

        noClasses()
            .that(erIkkeKjentUnntak)
            .should()
            .callMethodWhere(kallerJsonMessageNewMessageDirekte)
            .because(
                "nye Kafka-hendelser skal bruke typede DTOer via toJsonMessage(eventName, dto) i stedet for " +
                    "rå mapOf-payloads (se SøknadBehandletObserver.kt for eksempel)",
            ).check(klasser)
    }
}
