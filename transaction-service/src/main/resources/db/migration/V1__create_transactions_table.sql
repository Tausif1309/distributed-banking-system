CREATE TABLE transactions (

                              id BIGINT NOT NULL AUTO_INCREMENT,

                              sender_user_id BIGINT NOT NULL,

                              receiver_user_id BIGINT NOT NULL,

                              amount DECIMAL(19,2) NOT NULL,

                              currency VARCHAR(3) NOT NULL,

                              status VARCHAR(20) NOT NULL,

                              reference_id VARCHAR(100) NOT NULL,

                              description VARCHAR(255),

                              created_at DATETIME NOT NULL,

                              completed_at DATETIME,

                              CONSTRAINT pk_transactions
                                  PRIMARY KEY (id),

                              CONSTRAINT uk_transaction_reference
                                  UNIQUE (reference_id),

                              CONSTRAINT chk_transaction_amount
                                  CHECK (amount > 0)
);

CREATE INDEX idx_transaction_sender
    ON transactions(sender_user_id);

CREATE INDEX idx_transaction_receiver
    ON transactions(receiver_user_id);

CREATE INDEX idx_transaction_created_at
    ON transactions(created_at);