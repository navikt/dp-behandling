DROP INDEX idx_op_opplysninger_opplysning_id;

-- Dette er uansett primary key
DROP INDEX idx_opplysning_id;

-- Indekser bort de som er fjernet
CREATE INDEX idx_opplysning_fjernet ON opplysning (fjernet) WHERE fjernet = FALSE;
