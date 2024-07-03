# Dokumentasjon på behov for opplysninger

Dette er opplysninger som blir innhentet som en del av dagpengebehandlingen. De publiseres som behov på rapiden.

|Behov|Beskrivelse|Logisk datatype|Datatype|
|---|---|---|---|
|Fødselsdato | Fødselsdato | Dato|LocalDate|
|HelseTilAlleTyperJobb | Kan ta alle typer arbeid | Boolsk|boolean|
|InntektId | Inntekt | ULID|Ulid|
|InntektSiste12Mnd | Arbeidsinntekt siste 12 mnd | Desimaltall|double|
|InntektSiste36Mnd | Arbeidsinntekt siste 36 mnd | Desimaltall|double|
|KanJobbeDeltid | Kan jobbe heltid og deltid | Boolsk|boolean|
|KanJobbeHvorSomHelst | Kan jobbe i hele Norge | Boolsk|boolean|
|Lønnsgaranti | Har rett til dagpenger etter konkurs | Boolsk|boolean|
|Ordinær | Har rett til ordinære dagpenger gjennom arbeidsforhold | Boolsk|boolean|
|Permittert | Har rett til dagpenger under permittering | Boolsk|boolean|
|PermittertFiskeforedling | Har rett til dagpenger under permittering i fiskeforedlingsindustri | Boolsk|boolean|
|RegistrertSomArbeidssøker | Registrert som arbeidssøker | Boolsk|boolean|
|Søknadstidspunkt | Søknadsdato | Dato|LocalDate|
|Verneplikt | Avtjent verneplikt | Boolsk|boolean|
|VilligTilÅBytteYrke | Villig til å bytte yrke | Boolsk|boolean|
|ØnskerDagpengerFraDato | Ønsker dagpenger fra dato | Dato|LocalDate|