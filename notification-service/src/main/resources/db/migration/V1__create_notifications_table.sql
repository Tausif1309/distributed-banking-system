CREATE TABLE notifications (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,

                               user_id BIGINT NOT NULL,

                               transaction_id BIGINT NOT NULL,

                               type VARCHAR(50) NOT NULL,

                               title VARCHAR(150) NOT NULL,

                               message VARCHAR(500) NOT NULL,

                               is_read BOOLEAN NOT NULL DEFAULT FALSE,

                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                               INDEX idx_notification_user_id (user_id),

                               INDEX idx_notification_created_at (created_at),

                               INDEX idx_notification_user_read (user_id, is_read)
);