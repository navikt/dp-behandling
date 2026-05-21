package no.nav.dagpenger.mediator

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
}
