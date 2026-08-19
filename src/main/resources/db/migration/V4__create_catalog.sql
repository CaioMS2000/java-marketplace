-- Módulo catalog: sellers, categories, products (ordem respeita as FKs).
-- sellers.user_id NÃO tem FK para users: é referência por ID cross-módulo (catalog → identity).
CREATE TABLE sellers (
    id         UUID         PRIMARY KEY,
    user_id    UUID         NOT NULL UNIQUE,
    store_name VARCHAR(255) NOT NULL
);

CREATE TABLE categories (
    id        UUID         PRIMARY KEY,
    name      VARCHAR(255) NOT NULL UNIQUE,
    parent_id UUID         REFERENCES categories (id)
);

CREATE TABLE products (
    id             UUID           PRIMARY KEY,
    name           VARCHAR(255)   NOT NULL,
    price_amount   NUMERIC(19, 2) NOT NULL,
    price_currency VARCHAR(10)    NOT NULL,
    description    VARCHAR(255),
    category_id    UUID           REFERENCES categories (id),
    seller_id      UUID           NOT NULL REFERENCES sellers (id)
);
