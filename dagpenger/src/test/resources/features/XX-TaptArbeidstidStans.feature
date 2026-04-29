#language: no
Egenskap: Automatisk stans ved manglende tapt arbeidstid over flere meldeperioder

  Bakgrunn:
    Gitt at bruker har løpende rett fra og med "06.01.2025"
    Og at grenseverdien er 3 påfølgende perioder uten tapt arbeidstid

  Scenario: Tre perioder på rad uten tapt arbeidstid gir stans fra første periode i rekken
    Gitt at periode "06.01.2025" til "19.01.2025" oppfylte kravet til tapt arbeidstid
    Og at periode "20.01.2025" til "02.02.2025" ikke oppfylte kravet til tapt arbeidstid
    Og at periode "03.02.2025" til "16.02.2025" ikke oppfylte kravet til tapt arbeidstid
    Og at periode "17.02.2025" til "02.03.2025" ikke oppfylte kravet til tapt arbeidstid
    Når stansregelen kjøres
    Så skal dagpengene stanses fra og med "20.01.2025"

  Scenario: To perioder på rad uten tapt arbeidstid er ikke nok til stans
    Gitt at periode "06.01.2025" til "19.01.2025" oppfylte kravet til tapt arbeidstid
    Og at periode "20.01.2025" til "02.02.2025" ikke oppfylte kravet til tapt arbeidstid
    Og at periode "03.02.2025" til "16.02.2025" ikke oppfylte kravet til tapt arbeidstid
    Når stansregelen kjøres
    Så skal dagpengene ikke stanses

  Scenario: En god periode nullstiller rekken — ny rekke på tre gir stans fra ny startdato
    Gitt at periode "06.01.2025" til "19.01.2025" ikke oppfylte kravet til tapt arbeidstid
    Og at periode "20.01.2025" til "02.02.2025" ikke oppfylte kravet til tapt arbeidstid
    Og at periode "03.02.2025" til "16.02.2025" oppfylte kravet til tapt arbeidstid
    Og at periode "17.02.2025" til "02.03.2025" ikke oppfylte kravet til tapt arbeidstid
    Og at periode "03.03.2025" til "16.03.2025" ikke oppfylte kravet til tapt arbeidstid
    Og at periode "17.03.2025" til "30.03.2025" ikke oppfylte kravet til tapt arbeidstid
    Når stansregelen kjøres
    Så skal dagpengene stanses fra og med "17.02.2025"

  Scenario: Nøyaktig på grenseverdien gir stans
    Gitt at periode "06.01.2025" til "19.01.2025" ikke oppfylte kravet til tapt arbeidstid
    Og at periode "20.01.2025" til "02.02.2025" ikke oppfylte kravet til tapt arbeidstid
    Og at periode "03.02.2025" til "16.02.2025" ikke oppfylte kravet til tapt arbeidstid
    Når stansregelen kjøres
    Så skal dagpengene stanses fra og med "06.01.2025"
