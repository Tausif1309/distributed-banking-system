ALTER TABLE notifications
    ADD CONSTRAINT uk_notification_transaction_user_type
        UNIQUE (transaction_id, user_id, type);