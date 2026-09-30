CREATE TABLE account_transfers (
                                   id BIGINT NOT NULL AUTO_INCREMENT,

                                   transfer_reference VARCHAR(100) NOT NULL,

                                   sender_user_id BIGINT NOT NULL,

                                   receiver_user_id BIGINT NOT NULL,

                                   amount DECIMAL(19,2) NOT NULL,

                                   currency VARCHAR(3) NOT NULL,

                                   status VARCHAR(20) NOT NULL,

                                   sender_balance_after DECIMAL(19,2),

                                   receiver_balance_after DECIMAL(19,2),

                                   created_at DATETIME NOT NULL,

                                   completed_at DATETIME,

                                   CONSTRAINT pk_account_transfers
                                       PRIMARY KEY (id),

                                   CONSTRAINT uk_account_transfer_reference
                                       UNIQUE (transfer_reference),

                                   CONSTRAINT fk_transfer_sender
                                       FOREIGN KEY (sender_user_id)
                                           REFERENCES users(id),

                                   CONSTRAINT fk_transfer_receiver
                                       FOREIGN KEY (receiver_user_id)
                                           REFERENCES users(id),

                                   CONSTRAINT chk_transfer_amount
                                       CHECK (amount > 0),

                                   CONSTRAINT chk_transfer_different_users
                                       CHECK (sender_user_id <> receiver_user_id)
);

CREATE INDEX idx_account_transfer_sender
    ON account_transfers(sender_user_id);

CREATE INDEX idx_account_transfer_receiver
    ON account_transfers(receiver_user_id);

CREATE INDEX idx_account_transfer_created_at
    ON account_transfers(created_at);