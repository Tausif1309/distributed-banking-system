ALTER TABLE transactions
    ADD COLUMN idempotency_key VARCHAR(100) NOT NULL;

ALTER TABLE transactions
    ADD CONSTRAINT uk_transaction_user_idempotency
        UNIQUE (sender_user_id, idempotency_key);