# Etablering

## Regeltre

```mermaid
graph RL
```

## Akseptansetester

```gherkin
#language: no
@dokumentasjon @regel-etablering
Egenskap: Etablering

  Scenariomal: Etablering skal vurderes og saksbehandler har vurdert utfallet
    Gitt at etablering skal vurderes
    Og saksbehandler vurderer at etablering "<påvirker>" utfallet
    Så skal vilkåret om etablering "<påvirke>" resultatet

    Eksempler:
      | påvirker | påvirke |
      | Ja       | Ja      |
      | Nei      | Nei      |

  Scenario: Etablering skal vurderes uten at saksbehandler har vurdert utfallet
    Gitt at etablering skal vurderes
    Så skal vilkåret om etablering "Ja" resultatet

  Scenario: Etablering skal ikke vurderes
    Gitt at etablering ikke skal vurderes
    Så skal vilkåret om etablering "Ja" resultatet
``` 