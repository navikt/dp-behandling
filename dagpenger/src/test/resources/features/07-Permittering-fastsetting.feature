#language: no
@dokumentasjon @regel-permittering-fastsetting
Egenskap: § 4-7 Permittering

  Scenariomal: Søker oppfyller kravet til permittering
    Gitt at søker "<er permittert>"
    Og "<utfall>" av permittering
    Så skal søker få <periode> uker med permittering

  Eksempler:
    | er permittert | utfall | periode |
    | Nei           | Nei    | 0       |
    | Ja            | Nei    | 0       |
    | Ja            | Ja     | 26      |
