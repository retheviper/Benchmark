insert into authors (name, bio)
select
    'Author ' || gs,
    'Benchmark author biography ' || gs
from generate_series(1, 500) as gs;

insert into books (author_id, isbn, title, description, price_cents, stock, published_at)
select
    ((gs - 1) % 500) + 1,
    lpad(gs::text, 13, '0'),
    'Benchmark Book ' || gs,
    'Synthetic benchmark book description ' || gs,
    1000 + ((gs - 1) % 4000),
    5 + ((gs - 1) % 95),
    date '2020-01-01' + ((gs - 1) % 1825)
from generate_series(1, 20000) as gs;

analyze authors;
analyze books;
analyze checkouts;

