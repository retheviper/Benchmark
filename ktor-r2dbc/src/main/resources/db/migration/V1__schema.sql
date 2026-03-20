create table if not exists authors (
    id uuid primary key,
    name text not null,
    country_code varchar(2) not null,
    created_at_epoch_millis bigint not null
);

create index if not exists idx_authors_name on authors (name);

create table if not exists books (
    id uuid primary key,
    author_id uuid not null references authors (id) on delete cascade,
    title text not null,
    genre text not null,
    published_at_epoch_millis bigint not null,
    created_at_epoch_millis bigint not null
);

create index if not exists idx_books_author_id on books (author_id);
create index if not exists idx_books_title on books (title);

create table if not exists checkouts (
    id uuid primary key,
    book_id uuid not null references books (id) on delete cascade,
    borrower_name text not null,
    checked_out_at_epoch_millis bigint not null,
    returned_at_epoch_millis bigint null
);

create index if not exists idx_checkouts_book_id on checkouts (book_id);

