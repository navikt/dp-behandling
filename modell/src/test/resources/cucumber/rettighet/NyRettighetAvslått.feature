# language: no
Egenskap: Ny rettighet avslått

  Scenario:
    Gitt en ny hendelse om avslått søknad
      | fødselsnummer | behandlingId                         | utfall  | virkningsdato | dagpengerettighet |
      | 12345678901   | 06DF4319-85E7-4AD3-8470-2AC28384A802 | Avslag  | 14.12.2022    | Ordinær           |
    Så skal bruker ha 1 vedtak
    Og vedtaket har virkningsdato "14.12.2022"