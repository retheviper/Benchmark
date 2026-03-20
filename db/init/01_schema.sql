create extension if not exists pgcrypto;
create extension if not exists pg_stat_statements;

create table if not exists authors (
    id bigserial primary key,
    name varchar(200) not null,
    bio text not null
);

create table if not exists books (
    id bigserial primary key,
    author_id bigint not null references authors(id),
    isbn varchar(20) not null unique,
    title varchar(255) not null,
    description text not null,
    price_cents integer not null,
    stock integer not null,
    published_at date not null
);

create index if not exists idx_books_author_id on books(author_id);
create index if not exists idx_books_title on books(title);

create table if not exists checkouts (
    id uuid primary key default gen_random_uuid(),
    book_id bigint not null references books(id),
    customer_name varchar(120) not null,
    customer_email varchar(255) not null,
    quantity integer not null check (quantity > 0),
    checked_out_at timestamptz not null default now()
);

create index if not exists idx_checkouts_book_id on checkouts(book_id);
create index if not exists idx_checkouts_checked_out_at on checkouts(checked_out_at desc);
