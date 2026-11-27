# Utforsk regeltreet i Neo4j

Grafen genereres av `DiagramTest` til `docs/regeltre-dagpenger.approved.graphml`.

```bash
cd docs/graf
docker compose up -d      # starter Neo4j og laster inn grafen
open http://localhost:7474 # Neo4j Browser, ingen innlogging
docker compose down       # stopper
```

Filen må finnes før `docker compose up` (ellers lager Docker en mappe med samme navn).

Last inn på nytt etter at grafen er endret: `docker compose run --rm importer`.

Noder: `Opplysning` (+ `Grunnlag` hvis den ikke utledes av andre), `Regelsett`, `Avklaring` og `Kvote`.
Typer som `Vilkår`/`Fastsettelse`/`Prosess` og datatype er egenskaper (`type`, `regelsettType`, `datatype`), ikke labels.

Relasjoner:
- Opplysning → opplysning: regeltypen (f.eks. `MinstAv`), fra produktet til det det utledes av.
- Regelsett → opplysning: `PRODUSERER`, `AVHENGER_AV`, `HENTER_VIA_BEHOV`, `UTFALL`, `BETINGELSE`,
  `ØNSKET_RESULTAT` (ønsket informasjon minus utfall og betingelser) og `AKTIV_OPPLYSNING`.
- Regelsett → `Avklaring` (`HAR_AVKLARING`) og `Kvote` (`HAR_KVOTE`). Kvote → opplysning:
  `TILDELINGSGRUNNLAG`, `TELLES_NÅR`, `UTLØSENDE_BETINGELSE`, `FORBRUKSTELLER`, `GJENSTÅENDE`,
  `SISTE_FORBRUK`, `SISTE_GJENSTÅENDE`.

## Eksempler

Alt som brukes til å fastsette en opplysning:

```cypher
MATCH p = (m {label: 'Fastsatt arbeidstid per uke før tap'})-[*1..]->(d)
RETURN p
```

Bare grunnlagsopplysningene bak den:

```cypher
MATCH (m {label: 'Fastsatt arbeidstid per uke før tap'})-[*1..]->(d:Grunnlag)
RETURN DISTINCT d.label, d.datatype, d.enhet ORDER BY d.label
```

Hva avhenger av en opplysning:

```cypher
MATCH p = (a)-[*1..]->(m {label: 'Prøvingsdato'}) RETURN p
```

Opplysninger per regelsett:

```cypher
MATCH (n:Opplysning) RETURN n.regelsett, count(*) ORDER BY count(*) DESC
```

Utfall og betingelser per regelsett:

```cypher
MATCH (r:Regelsett)-[k:UTFALL|BETINGELSE]->(o) RETURN r.navn, type(k), o.label
```

Avklaringer et regelsett kan gi, og kvotene:

```cypher
MATCH (r:Regelsett)-[:HAR_AVKLARING]->(a) RETURN r.navn, a.kode, a.tittel
MATCH (r:Regelsett)-[:HAR_KVOTE]->(k)-[t]->(o) RETURN r.navn, k.navn, type(t), o.label
```

Alt som ligger bak det et regelsett vil ha ut:

```cypher
MATCH (r:Regelsett {navn: 'Fastsettelse av arbeidstid'})-[:ØNSKET_RESULTAT]->(o)-[*]->(g:Grunnlag)
RETURN DISTINCT g.label
```
