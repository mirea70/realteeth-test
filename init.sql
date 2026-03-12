CREATE DATABASE IF NOT EXISTS realteeth;
USE realteeth;

CREATE TABLE image_job (
    image_job_id BIGINT NOT NULL,
    source_image_url VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    worker_job_id VARCHAR(255),
    result_image_url VARCHAR(255),
    result_at DATETIME(6),
    failure_code INT,
    failure_message VARCHAR(255),
    failed_at DATETIME(6),
    dispatch_attempt_count INT NOT NULL,
    poll_attempt_count INT NOT NULL,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (image_job_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE outbox_event (
    id BIGINT NOT NULL,
    domain_type VARCHAR(255) NOT NULL,
    domain_id BIGINT NOT NULL,
    type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    published_at DATETIME(6),
    available_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_image_job_status_updated_at ON image_job (status, updated_at);
CREATE INDEX idx_outbox_event_status_available_at ON outbox_event (status, available_at);
