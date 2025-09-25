UPDATE opplysningstype
SET datatype = 'Penger'
WHERE uuid in ('01973a27-d8b3-7ffd-a81a-a3826963b079', '01957069-d7d5-7f7c-b359-c00686fbf1f7', '01994cfd-9a27-762e-81fa-61f550467c95');


UPDATE opplysning_verdi
SET verdi_string = CONCAT('NOK ', verdi_heltall),
    datatype     = 'Penger'
WHERE opplysning_id IN (SELECT opplysning_id
                        FROM opplysning_verdi
                                 INNER JOIN opplysning ON opplysning.id = opplysning_verdi.opplysning_id
                                 INNER JOIN opplysningstype ON opplysningstype.opplysningstype_id = opplysning.opplysningstype_id
                        WHERE opplysningstype.uuid in ('01973a27-d8b3-7ffd-a81a-a3826963b079', '01957069-d7d5-7f7c-b359-c00686fbf1f7', '01994cfd-9a27-762e-81fa-61f550467c95'));