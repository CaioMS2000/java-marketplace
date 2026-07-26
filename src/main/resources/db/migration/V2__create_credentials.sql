-- Costura conta/credencial: a senha sai de users e vira uma credencial do tipo PASSWORD.
ALTER TABLE users
    DROP COLUMN password_hash;

CREATE TABLE credentials (
    id      UUID         PRIMARY KEY,
    user_id UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type    VARCHAR(20)  NOT NULL,
    subject VARCHAR(255) NOT NULL,
    UNIQUE (user_id, type)
);
