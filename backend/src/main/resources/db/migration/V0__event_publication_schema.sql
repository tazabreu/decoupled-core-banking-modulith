create table public.event_publication
(
    id               uuid                     not null
        primary key,
    listener_id      text                     not null,
    event_type       text                     not null,
    serialized_event text                     not null,
    publication_date timestamp with time zone not null,
    completion_date  timestamp with time zone
);

alter table public.event_publication
    owner to corebanking_app;

create index event_publication_serialized_event_hash_idx
    on public.event_publication using hash (serialized_event);

create index event_publication_by_completion_date_idx
    on public.event_publication (completion_date);

