# Test av regelmotoren ved bruk av Norske Redningshunders godkjenningsprogram

En liten test av regelmotoren for å evaluerer resultatet av gjennomføringen av en praktisk lydighetsprøve.

## Eksempel regel 
>**Bli på plass med forstyrrelser**
>
>_Gjennomføring_
>
>Hundefører tar med seg hunden til et sted som dommeren anviser og dekker/setter hunden der. Deretter går hundefører frem til dommeren for tildeling av oppgaver. Oppgavene for hundefører skal omfatte samtale med annen person, henting eller flytting av utstyr. Oppgaven skal innbefatte at hundefører er ute av syne for hunden i to minutter, og dette utføres i slutten av øvelsen. Hundeførers og dommers avstand fra hunden skal være ca. 10-30 m.
>
>Tid fra hunden dekkes ned/settes til øvelsen avsluttes skal være fem minutter. Tiden starter når hundefører forlater hunden. Øvelsen er slutt når dommer gir beskjed om at tiden er ute.  Hunden kan endre posisjon mellom stå, sitt og ligg under øvelsen.
>
>_Vurdering og grunnlag for underkjenning_
>
>For å få bestått skal hunden ikke forflytte seg mer enn én hundelengde før øvelsen er slutt.

### Regeltre
````mermaid
graph RL
  -1607386754["Hundefører er ute av synet for hunden "] -->|StørreEnnEllerLik| -526931442["Tid hundefører har vært ut av synet for hunden"]
  -1607386754["Hundefører er ute av synet for hunden "] -->|StørreEnnEllerLik| -1469641588["Tid hundefører skal være ut av synet for hunden"]
  735130082["Hunden ligger hele øvelsen"] -->|StørreEnnEllerLik| -253843564["Tiden hunden ligger"]
  735130082["Hunden ligger hele øvelsen"] -->|StørreEnnEllerLik| -1808032700["Tid fra hunden dekkes ned/settes til øvelsen avsluttes"]
  850327138["Bli på plass med forstyrrelser "] -->|Alle| 1719400500["For å få bestått skal hunden ikke forflytte seg mer enn én hundelengde før øvelsen er slutt."]
  850327138["Bli på plass med forstyrrelser "] -->|Alle| -1607386754["Hundefører er ute av synet for hunden "]
  850327138["Bli på plass med forstyrrelser "] -->|Alle| 735130082["Hunden ligger hele øvelsen"]
  850327138["Bli på plass med forstyrrelser "] -->|Alle| 491843032["Oppgavene for hundefører skal omfatte samtale med annen person, henting eller flytting av utstyr"]
````