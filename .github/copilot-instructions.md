# GitHub Copilot Agent Instruksjoner

Dette dokumentet inneholder instruksjoner som kan brukes av GitHub Copilot agenter for å generere dokumentasjon og diagrammer for denne kodebasen.

## Dokumentasjon av flyt og regler

### Instruks: Dokumenter BeregnMeldekortHendelse flyt

**Kommando:**
```
Lag et flytdiagram av hvordan BeregnMeldekortHendelse og hvilke regler som er i spill i BeregningsperiodeFabrikk og Beregningsperiode.
```

**Krav:**
- Lag diagrammer i Mermaid-format
- Skriv dokumentasjonen i Markdown
- Inkluder følgende:
  - Hovedflyt fra hendelse til beregning
  - Detaljerte regler i BeregningsperiodeFabrikk
  - Beregningslogikk i Beregningsperiode
  - Avklaringer og valideringer
  - Dataflyt og opplysninger
  - Nøkkelkonsepter og formler

**Output:**
- Fil: `docs/beregning-meldekort-flyt.md`
- Format: Markdown med Mermaid diagrammer

**Relevant kode:**
- `dagpenger/src/main/kotlin/no/nav/dagpenger/regel/hendelse/BeregnMeldekortHendelse.kt`
- `dagpenger/src/main/kotlin/no/nav/dagpenger/regel/beregning/BeregningsperiodeFabrikk.kt`
- `dagpenger/src/main/kotlin/no/nav/dagpenger/regel/beregning/Beregningsperiode.kt`
- `dagpenger/src/main/kotlin/no/nav/dagpenger/regel/beregning/Beregning.kt`
- `dagpenger/src/main/kotlin/no/nav/dagpenger/regel/beregning/Dag.kt`

---

## Fremtidige instruksjoner

Legg til flere dokumentasjonsinstruksjoner her etter samme mal:

### Instruks: [Navn på instruks]

**Kommando:**
```
[Beskriv hva som skal gjøres]
```

**Krav:**
- [Listepunkt 1]
- [Listepunkt 2]

**Output:**
- Fil: `[sti til output-fil]`
- Format: [filformat]

**Relevant kode:**
- [filsti 1]
- [filsti 2]

---

## Bruk av instruksjoner

### Manuelt via GitHub Copilot CLI
```bash
# Les instruksjonen og kjør
 copilot -p "Følg instruksen i .github/copilot-instructions.md for 'Dokumenter BeregnMeldekortHendelse flyt'" --allow-tool 'write'
```

### Via GitHub Actions
Disse instruksjonene kan integreres i GitHub Actions workflows for automatisk dokumentasjonsgenerering ved kodeendringer.

### Via Copilot Chat
Kopier relevant instruks og lim inn i Copilot Chat for å få generert dokumentasjon.

---

## Tips for nye instruksjoner

1. **Vær spesifikk**: Angi nøyaktig hvilke filer og komponenter som skal dokumenteres
2. **Definer format**: Spesifiser om det skal være Mermaid, PlantUML, tabeller, etc.
3. **Angi output-lokasjon**: Gi klar filsti for hvor dokumentasjonen skal lagres
4. **List avhengigheter**: Inkluder alle relevante kodefiler
5. **Beskriv struktur**: Hvis dokumentet skal ha spesifikke seksjoner, list dem opp
