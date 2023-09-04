package no.nav.dagpenger.vedtak.db

import no.nav.dagpenger.vedtak.mediator.persistens.PersonRepository
import no.nav.dagpenger.vedtak.modell.Person
import no.nav.dagpenger.vedtak.modell.PersonIdentifikator

class InMemoryPersonRepository : PersonRepository {
    private val persondb = mutableMapOf<PersonIdentifikator, Person>()
    override fun hent(ident: PersonIdentifikator): Person? = persondb[ident]

    override fun lagre(person: Person) {
        persondb[person.ident()] = person
    }

    fun reset() {
        persondb.clear()
    }
}