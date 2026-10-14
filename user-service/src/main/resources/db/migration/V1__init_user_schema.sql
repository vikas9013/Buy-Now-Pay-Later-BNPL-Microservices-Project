-- V1: Initial Schema for User Service
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255),
    phone_number VARCHAR(255) NOT NULL UNIQUE,
    kyc_status VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS credit_accounts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    total_credit_limit NUMERIC(19, 2) NOT NULL,
    available_credit NUMERIC(19, 2) NOT NULL,
    account_status VARCHAR(50) NOT NULL,
    CONSTRAINT fk_credit_accounts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_phone ON users(phone_number);
CREATE INDEX IF NOT EXISTS idx_credit_accounts_user_id ON credit_accounts(user_id);
