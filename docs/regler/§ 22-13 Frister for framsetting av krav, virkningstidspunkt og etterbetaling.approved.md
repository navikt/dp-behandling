# § 22-13 Frister for framsetting av krav, virkningstidspunkt og etterbetaling

## Regeltre

```mermaid
graph RL
```

## Akseptansetester

```gherkin
#language: no
@dokumentasjon @regel-framsattkrav
Egenskap: § 22-13 Frister for framsetting av krav, virkningstidspunkt og etterbetaling

  Scenario: Søker oppfyller kravet om å framsette krav
    Gitt at søker har søkt om dagpenger
    Og søknadstidspunktet er "26.09.2025"
    Så forelegger det plikt til å vurdere rett til ny stønadsperiode


  Scenario: Søker trekker framsatt krav
    Gitt at søker har søkt om dagpenger
    Og trekker kravet om dagpenger
    Så bortfaller plikten til å vurdere rett til ny stønadsperiode
``` 