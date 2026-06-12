CREATE DATABASE IF NOT EXISTS travelmind DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE travelmind;

CREATE TABLE IF NOT EXISTS travel_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_name VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    current_itinerary_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS trip_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    raw_input TEXT NOT NULL,
    destination VARCHAR(64) NOT NULL,
    duration_days INT NOT NULL,
    start_date DATE NULL,
    people_count INT NULL,
    budget_level VARCHAR(32) NULL,
    pace_level VARCHAR(32) NULL,
    transport_mode VARCHAR(32) NULL,
    preferences_json JSON NULL,
    hotel_area VARCHAR(128) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_trip_request_session_id (session_id),
    INDEX idx_trip_request_destination (destination)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS itinerary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    request_id BIGINT NOT NULL,
    version INT NOT NULL DEFAULT 1,
    title VARCHAR(256) NOT NULL,
    itinerary_json JSON NOT NULL,
    markdown_content MEDIUMTEXT NOT NULL,
    validation_status VARCHAR(32) NOT NULL DEFAULT 'PASSED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_itinerary_session_id (session_id),
    INDEX idx_itinerary_request_id (request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS poi_cache (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    source VARCHAR(32) NOT NULL DEFAULT 'AMAP',
    source_poi_id VARCHAR(128) NOT NULL,
    name VARCHAR(256) NOT NULL,
    city VARCHAR(64) NOT NULL,
    address VARCHAR(512) NULL,
    location VARCHAR(64) NULL,
    category VARCHAR(128) NULL,
    raw_json JSON NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_source_poi_id (source, source_poi_id),
    INDEX idx_poi_city_name (city, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS llm_call_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NULL,
    provider VARCHAR(64) NOT NULL,
    model VARCHAR(128) NOT NULL,
    call_type VARCHAR(64) NOT NULL,
    prompt_tokens INT NULL,
    completion_tokens INT NULL,
    latency_ms INT NULL,
    status VARCHAR(32) NOT NULL,
    error_message TEXT NULL,
    request_json JSON NULL,
    response_json JSON NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_llm_call_log_session_id (session_id),
    INDEX idx_llm_call_log_call_type (call_type),
    INDEX idx_llm_call_log_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
