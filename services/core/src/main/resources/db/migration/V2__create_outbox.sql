create table outbox (
    id             bigint        generated always as identity primary key,
    topic          varchar(128)  not null,
    partition_key  varchar(128)  not null,
    payload        jsonb         not null,
    created_at     timestamptz   not null default now(),
    published_at   timestamptz
);

create index ix_outbox_unpublished
    on outbox (id)
    where published_at is null;