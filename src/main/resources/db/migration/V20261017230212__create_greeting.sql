create table greeting (
    id         uuid         primary key,
    name       varchar(100) not null,
    created_at timestamptz  not null
);
