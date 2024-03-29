# language: no
Egenskap: Utbetaling

  Bakgrunn: Ordinære dagpenger er innvilget fra 12. desember
    Gitt en ny hendelse om innvilget søknad
      | fødselsnummer | behandlingId                         | utfall      | virkningsdato | dagsats | stønadsperiode | vanligArbeidstidPerDag | dagpengerettighet |
      | 12345678901   | 7E7A891C-E8E2-4641-A213-83E3A7841A57 | Innvilgelse | 12.12.2022    | 800     | 52             | 8                      | Ordinær           |

  Scenario: Rapporterer ingen arbeidstimer
    Når rapporteringshendelse mottas
      | dato       | fravær | timer |
      | 12.12.2022 | false  | 0     |
      | 13.12.2022 | false  | 0     |
      | 14.12.2022 | false  | 0     |
      | 15.12.2022 | false  | 0     |
      | 16.12.2022 | false  | 0     |
      | 17.12.2022 | false  | 0     |
      | 18.12.2022 | false  | 0     |
      | 19.12.2022 | false  | 0     |
      | 20.12.2022 | false  | 0     |
      | 21.12.2022 | false  | 0     |
      | 22.12.2022 | false  | 0     |
      | 23.12.2022 | false  | 0     |
      | 24.12.2022 | false  | 0     |
      | 25.12.2022 | false  | 0     |
    Så skal forbruket være 10 dager
    Så skal utbetalingen være 8000
    Så skal beregnet utbetaling være 8000 kr for "25.12.2022"
    Så skal bruker ha 2 vedtak

  Scenario: Rapporterer arbeid i helg tilsvarende to hele arbeidsdager
    Når rapporteringshendelse mottas
      | dato       | fravær | timer |
      | 12.12.2022 | false  | 0     |
      | 13.12.2022 | false  | 0     |
      | 14.12.2022 | false  | 0     |
      | 15.12.2022 | false  | 0     |
      | 16.12.2022 | false  | 0     |
      | 17.12.2022 | false  | 0     |
      | 18.12.2022 | false  | 0     |
      | 19.12.2022 | false  | 0     |
      | 20.12.2022 | false  | 0     |
      | 21.12.2022 | false  | 0     |
      | 22.12.2022 | false  | 0     |
      | 23.12.2022 | false  | 0     |
      | 24.12.2022 | false  | 8     |
      | 25.12.2022 | false  | 8     |
    Så skal forbruket være 10 dager
    Så skal utbetalingen være 6400
    Så skal beregnet utbetaling være 6400 kr for "25.12.2022"
    Så skal bruker ha 2 vedtak

  Scenario: Rapporterer fravær og arbeid
    Når rapporteringshendelse mottas
      | dato       | fravær | timer |
      | 12.12.2022 | false  | 8     |
      | 13.12.2022 | true   | 0     |
      | 14.12.2022 | true   | 0     |
      | 15.12.2022 | false  | 8     |
      | 16.12.2022 | false  | 8     |
      | 17.12.2022 | false  | 0     |
      | 18.12.2022 | false  | 0     |
      | 19.12.2022 | false  | 0     |
      | 20.12.2022 | false  | 0     |
      | 21.12.2022 | false  | 0     |
      | 22.12.2022 | false  | 0     |
      | 23.12.2022 | false  | 0     |
      | 24.12.2022 | false  | 0     |
      | 25.12.2022 | false  | 0     |
    Så skal forbruket være 8 dager
    Så skal utbetalingen være 4000
    Så skal beregnet utbetaling være 4000 kr for "25.12.2022"
    Så skal bruker ha 2 vedtak


  Scenario: Rapporterer ikke hele arbeidstimer
    Når rapporteringshendelse mottas
      | dato       | fravær | timer |
      | 12.12.2022 | false  | 0     |
      | 13.12.2022 | false  | 0     |
      | 14.12.2022 | false  | 0     |
      | 15.12.2022 | false  | 4     |
      | 16.12.2022 | false  | 4     |
      | 17.12.2022 | false  | 0     |
      | 18.12.2022 | false  | 0     |
      | 19.12.2022 | false  | 8     |
      | 20.12.2022 | false  | 8     |
      | 21.12.2022 | false  | 8     |
      | 22.12.2022 | false  | 8     |
      | 23.12.2022 | false  | 0     |
      | 24.12.2022 | false  | 0     |
      | 25.12.2022 | false  | 0     |
    Så skal forbruket være 10 dager
    Så skal utbetalingen være 4000
    Så skal beregnet utbetaling være 4000 kr for "25.12.2022"
    Så skal bruker ha 2 vedtak

