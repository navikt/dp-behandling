update opplysning_verdi set verdi_desimaltall = verdi_heltall
where opplysning_id in (
    select opplysning_id from opplysning_verdi
                                  inner join opplysning on opplysning.id = opplysning_verdi.opplysning_id
                                  inner join opplysningstype on opplysningstype.opplysningstype_id = opplysning.opplysningstype_id
    where opplysningstype.uuid = '01948ea0-e25c-7c47-8429-a05045d80eca'
);

update opplysningstype set datatype = 'Desimaltall'
where uuid = '01948ea0-e25c-7c47-8429-a05045d80eca';
