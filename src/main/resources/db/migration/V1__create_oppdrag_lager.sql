create table oppdrag_lager
(
    id                   uuid,
    utgaaende_oppdrag    text                                not null,
    status               varchar(150) default 'LAGT_PAA_KOE':: character varying not null,
    opprettet_tidspunkt  timestamp(6) default LOCALTIMESTAMP not null,
    person_ident         varchar(50)                         not null,
    fagsak_id            varchar(50)                         not null,
    behandling_id        varchar(50)                         not null,
    fagsystem            varchar(10)                         not null,
    avstemming_tidspunkt timestamp(6)                        not null,
    utbetalingsoppdrag   json                                not null,
    kvitteringsmelding   json,
    versjon              bigint       default 0              not null,

    primary key (person_ident, behandling_id, fagsystem, versjon)
);

create index status_index
    on oppdrag_lager (status);

create index oppdragid_idx
    on oppdrag_lager (behandling_id, person_ident);
