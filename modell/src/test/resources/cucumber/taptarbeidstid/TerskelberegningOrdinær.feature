# language: no

  Egenskap: VurderTerskelForTaptArbeidstid

    Bakgrunn: Ordinære dagpenger er innvileget fra 12. desember
      Gitt en ny hendelse om innvilget søknad
        | fødselsnummer | behandlingId                         | utfall      | virkningsdato | dagsats | stønadsperiode | vanligArbeidstidPerDag | dagpengerettighet |
        | 12345678901   | 7E7A891C-E8E2-4641-A213-83E3A7841A57 | Innvilgelse | 12.12.2022    | 800     | 104            | 8                      | Ordinær           |

      Scenario: Rapporterer arbeidstimer eksakt lik terskel
        Når rapporteringshendelse mottas
          | dato       | fravær | timer |
          | 12.12.2022 | false  | 4     |
          | 13.12.2022 | false  | 4     |
          | 14.12.2022 | false  | 4     |
          | 15.12.2022 | false  | 4     |
          | 16.12.2022 | false  | 4     |
          | 17.12.2022 | false  | 0     |
          | 18.12.2022 | false  | 0     |
          | 19.12.2022 | false  | 4     |
          | 20.12.2022 | false  | 4     |
          | 21.12.2022 | false  | 4     |
          | 22.12.2022 | false  | 4     |
          | 23.12.2022 | false  | 4     |
          | 24.12.2022 | false  | 0     |
          | 25.12.2022 | false  | 0     |
        Så skal forbruket være 10 dager
        Så skal bruker ha 2 vedtak


      Scenario: Rapporterer arbeidstimer over terskel
        Når rapporteringshendelse mottas
          | dato       | fravær | timer |
          | 12.12.2022 | false  | 4     |
          | 13.12.2022 | false  | 4     |
          | 14.12.2022 | false  | 4     |
          | 15.12.2022 | false  | 4     |
          | 16.12.2022 | false  | 4     |
          | 17.12.2022 | false  | 0     |
          | 18.12.2022 | false  | 0     |
          | 19.12.2022 | false  | 4     |
          | 20.12.2022 | false  | 4     |
          | 21.12.2022 | false  | 4     |
          | 22.12.2022 | false  | 4     |
          | 23.12.2022 | false  | 4.5   |
          | 24.12.2022 | false  | 0     |
          | 25.12.2022 | false  | 0     |
          Så skal forbruket være 0 dager
          Så skal bruker ha 2 vedtak


    Scenario: Rapporterer fravær en dag og arbeidstimer eksakt lik terskel øvrige arbeidsdager
      Når rapporteringshendelse mottas
        | dato       | fravær | timer |
        | 12.12.2022 | false  | 4     |
        | 13.12.2022 | false  | 4     |
        | 14.12.2022 | false  | 4     |
        | 15.12.2022 | false  | 4     |
        | 16.12.2022 | false  | 4     |
        | 17.12.2022 | false  | 0     |
        | 18.12.2022 | false  | 0     |
        | 19.12.2022 | false  | 4     |
        | 20.12.2022 | false  | 4     |
        | 21.12.2022 | false  | 4     |
        | 22.12.2022 | false  | 4     |
        | 23.12.2022 | true   | 0     |
        | 24.12.2022 | false  | 0     |
        | 25.12.2022 | false  | 0     |
      Så skal forbruket være 9 dager
      Så skal bruker ha 2 vedtak


      Scenario: Har ikke dagpengerettighet hele meldeperioden. Rapporterer arbeidstimer eksakt lik terskel etter vedtakstidspunkt.
        Når rapporteringshendelse mottas
          | dato       | fravær | timer |
          | 05.12.2022 | false  | 8     |
          | 06.12.2022 | false  | 8     |
          | 07.12.2022 | false  | 8     |
          | 08.12.2022 | false  | 8     |
          | 09.12.2022 | false  | 8     |
          | 10.12.2022 | false  | 0     |
          | 11.12.2022 | false  | 0     |
          | 12.12.2022 | false  | 4     |
          | 13.12.2022 | false  | 4     |
          | 14.12.2022 | false  | 4     |
          | 15.12.2022 | false  | 4     |
          | 16.12.2022 | false  | 4     |
          | 17.12.2022 | false  | 0     |
          | 18.12.2022 | false  | 0     |
        Så skal forbruket være 5 dager
        Så skal bruker ha 2 vedtak

    Scenario: Rapporterer arbeidstimer på samme dag som fravær. Disse arbeidstimene skal ikke telle med, så bruker har tapt arbeidstid eksakt lik terskel.
      Når rapporteringshendelse mottas
        | dato       | fravær | timer |
        | 12.12.2022 | false  | 4     |
        | 13.12.2022 | false  | 4     |
        | 14.12.2022 | false  | 4     |
        | 15.12.2022 | false  | 4     |
        | 16.12.2022 | false  | 4     |
        | 17.12.2022 | false  | 0     |
        | 18.12.2022 | false  | 0     |
        | 19.12.2022 | false  | 4     |
        | 20.12.2022 | false  | 4     |
        | 21.12.2022 | false  | 4     |
        | 22.12.2022 | false  | 4     |
        | 23.12.2022 | true   | 8     |
        | 24.12.2022 | false  | 0     |
        | 25.12.2022 | false  | 0     |
      Så skal forbruket være 9 dager
      Så skal bruker ha 2 vedtak

    Scenario: Rapporterer arbeid i helgedag, som gjør at man kommer over terskel
      Når rapporteringshendelse mottas
        | dato       | fravær | timer |
        | 12.12.2022 | false  | 4     |
        | 13.12.2022 | false  | 4     |
        | 14.12.2022 | false  | 4     |
        | 15.12.2022 | false  | 4     |
        | 16.12.2022 | false  | 4     |
        | 17.12.2022 | false  | 0.5   |
        | 18.12.2022 | false  | 0     |
        | 19.12.2022 | false  | 4     |
        | 20.12.2022 | false  | 4     |
        | 21.12.2022 | false  | 4     |
        | 22.12.2022 | false  | 4     |
        | 23.12.2022 | false  | 4     |
        | 24.12.2022 | false  | 0     |
        | 25.12.2022 | false  | 0     |
      Så skal forbruket være 0 dager
      Så skal bruker ha 2 vedtak
