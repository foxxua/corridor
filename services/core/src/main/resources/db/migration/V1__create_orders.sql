create table orders (
    id          uuid          primary key,
    status      varchar(32)   not null,
    created_at  timestamptz   not null,

    constraint ck_orders_status
        check ( status in ('CREATED') )
);