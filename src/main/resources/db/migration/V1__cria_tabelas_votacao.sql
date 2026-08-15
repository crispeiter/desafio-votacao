create table pauta (
    id         bigserial     primary key,
    titulo     varchar(200)  not null,
    descricao  varchar(2000),
    criada_em  timestamptz   not null
);

create table sessao_votacao (
    id          bigserial   primary key,
    pauta_id    bigint      not null references pauta (id),
    aberta_em   timestamptz not null,
    encerra_em  timestamptz not null,
    constraint uk_sessao_pauta unique (pauta_id)
);

create table voto (
    id             bigserial   primary key,
    sessao_id      bigint      not null references sessao_votacao (id),
    cpf_associado  varchar(11) not null,
    opcao          varchar(10) not null,
    registrado_em  timestamptz not null,
    constraint uk_voto_sessao_associado unique (sessao_id, cpf_associado)
);

create index idx_voto_sessao_opcao on voto (sessao_id, opcao);
