package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Lovkilde
import java.net.URI

val aOpplynsingsLoven = Lovkilde("A-opplysningsloven", "a-opplysningsloven")
val forskriftTilFolketrygden = Lovkilde("Forskrift til Folketrygdloven", "ftrl")
val folketrygden = Lovkilde("Folketrygdloven", "ftrl", URI("https://lovdata.no/nav/lov/1997-02-28-19/"))
val forvaltningsloven = Lovkilde("Forvaltningsloven", "fvl", URI("https://lovdata.no/nav/andre-rettskilder/Forvaltningsloven/kapVI/"))
