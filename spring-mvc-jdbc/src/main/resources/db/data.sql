insert into authors (name, country)
select format('Author %s', s), case (s % 5)
    when 0 then 'KR'
    when 1 then 'JP'
    when 2 then 'US'
    when 3 then 'DE'
    else 'FR'
end
from generate_series(1, 20) as s
on conflict do nothing;

insert into books (author_id, title, genre, isbn, stock, price)
select
    ((s - 1) % 20) + 1,
    format('Benchmark Book %s', s),
    case (s % 6)
        when 0 then 'fiction'
        when 1 then 'history'
        when 2 then 'science'
        when 3 then 'business'
        when 4 then 'technology'
        else 'travel'
    end,
    concat('978', lpad(s::text, 10, '0')),
    1000 - (s % 100),
    (10 + (s % 30))::numeric(10, 2)
from generate_series(1, 5000) as s
on conflict do nothing;
