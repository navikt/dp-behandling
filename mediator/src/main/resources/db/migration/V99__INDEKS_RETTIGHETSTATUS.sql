-- rettighetstatus manglet indeks helt (heller ikke på ident), til tross for at REFERENCES person(ident)
-- kun gir en constraint, ikke en indeks på den refererende siden. Alle spørringer på ident har derfor
-- gjort full seq scan. Denne dekker både oppslag på ident alene og DISTINCT ON (ident) ... ORDER BY gjelder_fra DESC.
CREATE INDEX idx_rettighetstatus_ident_gjelder_fra ON rettighetstatus (ident, gjelder_fra DESC);
