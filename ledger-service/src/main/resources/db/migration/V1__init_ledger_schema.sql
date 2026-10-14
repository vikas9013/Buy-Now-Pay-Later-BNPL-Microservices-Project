-- V1: Initial Schema for Ledger Service
CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    merchant_name VARCHAR(255) NOT NULL,
    transaction_date TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS repayments (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    transaction_id UUID NOT NULL,
    amount_due NUMERIC(19, 2) NOT NULL,
    due_date DATE NOT NULL,
    is_paid BOOLEAN NOT NULL DEFAULT FALSE,
    late_fee_applied NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    payment_method VARCHAR(50),
    gateway_payment_id VARCHAR(255),
    gateway_order_id VARCHAR(255),
    payment_gateway VARCHAR(50),
    paid_at TIMESTAMP,
    CONSTRAINT fk_repayments_transaction FOREIGN KEY (transaction_id) REFERENCES transactions (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_transactions_user_id ON transactions (user_id);
CREATE INDEX IF NOT EXISTS idx_repayments_user_id ON repayments (user_id);
CREATE INDEX IF NOT EXISTS idx_repayments_due_date ON repayments (due_date, is_paid);
