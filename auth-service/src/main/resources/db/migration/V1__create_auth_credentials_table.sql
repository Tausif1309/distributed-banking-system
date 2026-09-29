CREATE TABLE auth_credentials (
                                  id BIGINT NOT NULL AUTO_INCREMENT,

                                  username VARCHAR(100) NOT NULL,

                                  password_hash VARCHAR(255) NOT NULL,

                                  role VARCHAR(20) NOT NULL,

                                  status VARCHAR(20) NOT NULL,

                                  created_at DATETIME NOT NULL,

                                  updated_at DATETIME NOT NULL,

                                  CONSTRAINT pk_auth_credentials
                                      PRIMARY KEY (id),

                                  CONSTRAINT uk_auth_username
                                      UNIQUE (username)
);