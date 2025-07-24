CREATE INDEX idx_opplysning_fjernet_true
    ON opplysning (opplysningstype_id, id, opprettet DESC)
    WHERE fjernet = TRUE;

CREATE INDEX idx_op_opplysninger_opplysning_id
    ON opplysninger_opplysning(opplysninger_id, opplysning_id);
