create table if not exists authors (
    id bigserial primary key,
    name varchar(200) not null,
    country varchar(100) not null,
    created_at timestamptz not null default now()
);

create table if not exists books (
    id bigserial primary key,
    author_id bigint not null references authors (id),
    title varchar(255) not null,
    genre varchar(80) not null,
    isbn varchar(20) not null unique,
    stock integer not null,
    price numeric(10, 2) not null,
    created_at timestamptz not null default now()
);

create index if not exists idx_books_author_id on books (author_id);
create index if not exists idx_books_title on books (title);

create table if not exists checkouts (
    id bigserial primary key,
    book_id bigint not null references books (id),
    customer_name varchar(120) not null,
    quantity integer not null,
    checkout_token uuid not null unique,
    created_at timestamptz not null default now()
);

create index if not exists idx_checkouts_book_id_created_at on checkouts (book_id, created_at desc);
