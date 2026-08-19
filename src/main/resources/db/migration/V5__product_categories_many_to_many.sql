-- Produto passa a pertencer a 0-N categorias (antes era 0-1 via products.category_id).
ALTER TABLE products
    DROP COLUMN category_id;

CREATE TABLE product_categories (
    product_id  UUID NOT NULL REFERENCES products (id),
    category_id UUID NOT NULL REFERENCES categories (id),
    PRIMARY KEY (product_id, category_id)
);
