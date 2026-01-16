#language: no
@dokumentasjon @regel-uriktig-eller-mangelfulle-opplysninger
Egenskap: § 21-7. Manglende og/eller uriktige opplysninger

  Scenariomal: Test av uriktige opplysninger
    Gitt at søker har søkt om dagpenger
    Og at søker har <uriktige> uriktige opplysninger
    Og at søker har <holdertilbake> holder tilbake opplysninger
    Og at søker har <unnlater> unnlater å etterkomme pålegg
    Så skal vilkåret for uriktige opplysninger være <utfall>
    Eksempler:
      | uriktige | holdertilbake | unnlater | utfall |
      | Ja       | Nei           | Nei      | Nei    |
      | Ja       | Nei           | Ja       | Nei    |
      | Ja       | Ja            | Nei      | Nei    |
      | Ja       | Ja            | Ja       | Nei    |
      | Nei      | Ja            | Nei      | Nei    |
      | Nei      | ja            | Ja       | Nei    |
      | Nei      | Nei           | Ja       | Nei    |
      | Nei      | Nei           | Nei      | Ja     |