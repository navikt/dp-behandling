#language: no
Egenskap: Bli på plass med forstyrrelser

  Scenariomal:
    Gitt at hunden ikke forflytte seg mer enn én hundelengde før øvelsen er slutt er "<hundenFlyttetSegIkke>"
    Gitt at minutter hundefører skal være ut av synet er "<minutterHundeførerSkalVæreUtAvSynet>"
    Gitt at hundefører er ut av synet "<hundeførerErUtAvSynet>" minutter
    Gitt at øvelsen skal vare i "<lengdePåØvelse>" minutter
    Gitt at hunden ligger i "<hundeLiggerI>" minutter
    Gitt at oppgave til hundefører er gitt: "<oppgaveGitt>"
    Så skal resultatet være "<resultat>"

    Eksempler:
      | hundenFlyttetSegIkke | minutterHundeførerSkalVæreUtAvSynet | hundeførerErUtAvSynet | lengdePåØvelse | hundeLiggerI | oppgaveGitt | resultat     |
      | true                 | 2                                   | 2                     | 5              | 5            | true        | Bestått      |
      | false                | 2                                   | 2                     | 5              | 5            | true        | Ikke bestått |
      | true                 | 2                                   | 1                     | 5              | 5            | true        | Ikke bestått |
      | true                 | 2                                   | 2                     | 5              | 3            | true        | Ikke bestått |
      | true                 | 2                                   | 2                     | 5              | 5            | false       | Ikke bestått |
