UPDATE behandling
SET tilstand = 'Avbrutt'
WHERE behandling_id = '019816fc-a739-7819-accc-e9d8ae053792'
  AND EXISTS (SELECT 1 FROM behandling WHERE behandling_id = '019816fc-a739-7819-accc-e9d8ae053792')
  AND tilstand != 'Avbrutt';