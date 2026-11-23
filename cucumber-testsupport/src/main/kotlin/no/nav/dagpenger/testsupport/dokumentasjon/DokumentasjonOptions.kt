package no.nav.dagpenger.testsupport.dokumentasjon

import org.approvaltests.core.Options
import org.approvaltests.namer.ApprovalNamer
import java.io.File

fun markdownOptions(
    katalog: String,
    filnavn: String,
): Options =
    Options()
        .forFile()
        .withExtension(".md")
        .forFile()
        .withNamer(
            object : ApprovalNamer {
                override fun getApprovedFile(extensionWithDot: String) = File(katalog, "$filnavn.approved$extensionWithDot")

                override fun getReceivedFile(extensionWithDot: String) = File(katalog, "$filnavn.received$extensionWithDot")
            },
        )
