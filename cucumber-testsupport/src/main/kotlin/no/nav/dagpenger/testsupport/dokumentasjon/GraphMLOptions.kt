package no.nav.dagpenger.testsupport.dokumentasjon

import org.approvaltests.core.Options
import org.approvaltests.namer.ApprovalNamer
import java.io.File

fun graphmlOptions(
    katalog: String,
    filnavn: String,
): Options =
    Options()
        .forFile()
        .withExtension(".graphml")
        .forFile()
        .withNamer(
            object : ApprovalNamer {
                override fun getApprovedFile(extensionWithDot: String) = File(katalog, "$filnavn.approved$extensionWithDot")

                override fun getReceivedFile(extensionWithDot: String) = File(katalog, "$filnavn.received$extensionWithDot")
            },
        )
