#language: no
@dokumentasjon @regel-reell-arbeidssøker
Egenskap: § 4-5. Reelle arbeidssøkere

  Scenario: Søker fyller kravene til å være reell arbeidssøker
    Gitt at personen søkte "11.05.2022"
    Og ønsker arbeidstid på 37,5 timer
    Og kan jobbe både heltid og deltid
    Og kan jobbe i hele Norge
    Og kan ta alle typer arbeid
    Og er villig til å bytte yrke eller gå ned i lønn
    Så skal kravet til reell arbeidssøker være oppfylt

  Scenario: Søker fyller ikke kravene til å være reell arbeidssøker
    Gitt at personen søkte "11.05.2022"
    Og ønsker arbeidstid på 37,5 timer
    Og kan ikke jobbe både heltid og deltid
    Og kan ikke jobbe i hele Norge
    Og kan ikke ta alle typer arbeid
    Og er ikke villig til å bytte yrke eller gå ned i lønn
    Så skal kravet til reell arbeidssøker ikke være oppfylt

  Scenario: Søker fyller vilkårene til å kun søke arbeid lokalt
    Gitt at personen søkte "11.05.2022"
    Og ønsker arbeidstid på 37,5 timer
    Og kan jobbe både heltid og deltid
    Og kan ikke jobbe i hele Norge
    Men oppfyller kravet å kun søke lokalt arbeid
    Og kan ta alle typer arbeid
    Og er villig til å bytte yrke eller gå ned i lønn
    Så skal kravet til reell arbeidssøker være oppfylt

  Scenario: Søker fyller vilkårene til å kun søke deltidssarbeid
    Gitt at personen søkte "11.05.2022"
    Og ønsker arbeidstid på 37,5 timer
    Og kan ikke jobbe både heltid og deltid
    Men oppfyller kravet til å kun søke deltidssarbeid
    Og kan jobbe i hele Norge
    Og kan ta alle typer arbeid
    Og er villig til å bytte yrke eller gå ned i lønn
    Så skal kravet til reell arbeidssøker være oppfylt

  Scenario: Søker ønsker ikke nok arbeid til å være reell arbeidssøker
    Gitt at personen søkte "11.05.2022"
    Og ønsker arbeidstid på 12 timer
    Og kan jobbe både heltid og deltid
    Og kan jobbe i hele Norge
    Og kan ta alle typer arbeid
    Og er villig til å bytte yrke eller gå ned i lønn
    Så skal kravet til reell arbeidssøker ikke være oppfylt


