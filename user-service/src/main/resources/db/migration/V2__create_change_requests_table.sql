CREATE TABLE change_requests (
                                 id BIGINT NOT NULL AUTO_INCREMENT,

                                 user_id BIGINT NOT NULL,

                                 field_name VARCHAR(50) NOT NULL,

                                 old_value VARCHAR(255),

                                 new_value VARCHAR(255) NOT NULL,

                                 status VARCHAR(20) NOT NULL,

                                 reviewed_by BIGINT,

                                 created_at DATETIME NOT NULL,

                                 reviewed_at DATETIME,

                                 CONSTRAINT pk_change_requests
                                     PRIMARY KEY (id),

                                 CONSTRAINT fk_change_request_user
                                     FOREIGN KEY (user_id)
                                         REFERENCES users(id)
);