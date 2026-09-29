CREATE TABLE users (
                       id BIGINT NOT NULL AUTO_INCREMENT,

                       full_name VARCHAR(150) NOT NULL,

                       email VARCHAR(150) NOT NULL,

                       phone VARCHAR(20) NOT NULL,

                       address VARCHAR(255) NOT NULL,

                       status VARCHAR(20) NOT NULL,

                       created_at DATETIME NOT NULL,

                       updated_at DATETIME NOT NULL,

                       CONSTRAINT pk_users
                           PRIMARY KEY (id),

                       CONSTRAINT uk_user_email
                           UNIQUE (email),

                       CONSTRAINT uk_user_phone
                           UNIQUE (phone)
);


CREATE TABLE accounts (
                          id BIGINT NOT NULL AUTO_INCREMENT,

                          user_id BIGINT NOT NULL,

                          balance DECIMAL(19,2) NOT NULL,

                          currency VARCHAR(3) NOT NULL,

                          status VARCHAR(20) NOT NULL,

                          created_at DATETIME NOT NULL,

                          updated_at DATETIME NOT NULL,

                          CONSTRAINT pk_accounts
                              PRIMARY KEY (id),

                          CONSTRAINT uk_account_user_id
                              UNIQUE (user_id),

                          CONSTRAINT fk_account_user
                              FOREIGN KEY (user_id)
                                  REFERENCES users(id)
);