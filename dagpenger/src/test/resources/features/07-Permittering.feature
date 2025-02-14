#language: no
@dokumentasjon @regel-permittering
Egenskap: § 4-7 Permittering

  Scenariomal: Søker oppfyller kravet til permittering
    Gitt at søker har "<er permittert>" om dagpenger under permittering
    Og saksbehandler vurderer at søker har "<godkjent årsak>" til permittering
    Og vurderer at søker har "<midlertidig>" permittering
    Så skal søker få "<utfall>" av permittering og <periode> uker med permittering

  Eksempler:
    | er permittert | godkjent årsak | midlertidig | utfall | periode |
    | Nei           | Nei            | Nei         | Nei    | 0       |
    | Nei           | Ja             | Nei         | Nei    | 0       |
    | Nei           | Nei            | Ja          | Nei    | 0       |
    | Ja            | Nei            | Nei         | Nei    | 0       |
    | Ja            | Ja             | Nei         | Nei    | 0       |
    | Ja            | Nei            | Ja          | Nei    | 0       |
    | Ja            | Ja             | Ja          | Ja     | 26      |
