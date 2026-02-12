#language: no
@dokumentasjon @regel-dagpengensStørrelse
Egenskap: § 4-12. Dagpengenes størrelse

  Scenario: Dagpengegrunnlag og søker har ikke barn
    Gitt at dagpengegrunnlag er "246131"
    Og at søker har ikke barn
    Så skal dagpengens uavrundet størrelse uten barnetillegg være "590.7144"
    Så skal dagpengens størrelse være "591"

  Scenario: Dagpengegrunnlag og søker har ikke barn
    Gitt at dagpengegrunnlag er "425625"
    Og at søker har ikke barn
    Så skal dagpengens uavrundet størrelse uten barnetillegg være "1021.5"
    Så skal dagpengens størrelse være "1022"

  Scenario: Dagpengegrunnlag og søker har 2 barn
    Gitt at dagpengegrunnlag er "246131"
    Og at søker har 2 barn
    Så skal dagpengens uavrundet størrelse uten barnetillegg være "590.7144"
    Så skal dagpengens størrelse være "663"

  Scenario: Dagpengegrunnlag og mange barn gjør at 90% regelen blir brukt
    Gitt at dagpengegrunnlag er "70001"
    Og at søker har 7 barn
    Så skal dagpengens uavrundet størrelse uten barnetillegg være "168.0024"
    Så skal dagpengens størrelse være "242"
    Så skal ukessats være "1210"
