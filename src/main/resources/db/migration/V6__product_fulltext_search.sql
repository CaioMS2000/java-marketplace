-- Busca full-text em products (name + description). Postgres não tem "FULLTEXT INDEX"
-- como o MySQL; o equivalente é indexar uma coluna tsvector com GIN.
ALTER TABLE products
    ADD COLUMN search_vector tsvector
        GENERATED ALWAYS AS (
            to_tsvector('portuguese', coalesce(name, '') || ' ' || coalesce(description, ''))
        ) STORED;

CREATE INDEX products_search_vector_idx ON products USING GIN (search_vector);
