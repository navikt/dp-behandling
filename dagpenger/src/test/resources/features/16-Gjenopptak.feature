#language: no

Egenskap: § 4-16. Gjenopptak av løpende stønadsperiode som er avbrutt

  Scenariomal: Søker gjenopptak
    Gitt at søkeren har hatt en løpende stønadsperiode og har hatt minst en forbruksdag på <siste forbruksdag>
    Og søker etter gjenopptak på <gjenopptaksprøvingsdato>
    Så skal gjenopptak være <gjenopptas>

    Eksempler:
    | siste forbruksdag | gjenopptaksprøvingsdato | gjenopptas |
    | 01.04.2022        | 01.05.2022              | Ja         |
    | 01.04.2020        | 01.05.2022              | Nei        |

