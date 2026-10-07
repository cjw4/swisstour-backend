-- Initial schema: Create all tables

-- Shared sequence for Player, Tournament, Round (allocationSize = 1)
CREATE SEQUENCE IF NOT EXISTS primary_sequence START WITH 1 INCREMENT BY 1;

-- Sequence for Event (Hibernate 6 default naming: {table}_seq)
CREATE SEQUENCE IF NOT EXISTS events_seq START WITH 1 INCREMENT BY 50;

-- Events table
CREATE TABLE IF NOT EXISTS "events" (
    id BIGINT PRIMARY KEY,
    event_id BIGINT UNIQUE,
    name VARCHAR(255),
    display_name VARCHAR(255),
    tier VARCHAR(255),
    "year" INTEGER NOT NULL,
    city VARCHAR(255),
    country VARCHAR(255),
    number_players INTEGER,
    points INTEGER NOT NULL,
    purse DOUBLE PRECISION,
    is_championship BOOLEAN NOT NULL,
    is_swisstour BOOLEAN NOT NULL,
    has_results BOOLEAN NOT NULL DEFAULT false,
    info_link VARCHAR(500),
    registration_link VARCHAR(500),
    registration_start DATE,
    swisstour_type VARCHAR(50),
    start_date DATE,
    end_date DATE
);

-- Players table
CREATE TABLE IF NOT EXISTS "players" (
    id BIGINT PRIMARY KEY,
    firstname VARCHAR(255) NOT NULL,
    lastname VARCHAR(255) NOT NULL,
    pdga_number BIGINT UNIQUE,
    sda_number BIGINT UNIQUE,
    swisstour_license BOOLEAN NOT NULL,
    is_pro BOOLEAN NOT NULL
);

-- Tournaments table
CREATE TABLE IF NOT EXISTS "tournaments" (
    id BIGINT PRIMARY KEY,
    division VARCHAR(255) NOT NULL,
    place INTEGER NOT NULL,
    rating INTEGER,
    prize DOUBLE PRECISION,
    score INTEGER,
    points DOUBLE PRECISION,
    event_id BIGINT NOT NULL REFERENCES "events"(id),
    player_id BIGINT NOT NULL REFERENCES "players"(id)
);

-- Rounds table
CREATE TABLE IF NOT EXISTS "rounds" (
    id BIGINT PRIMARY KEY,
    round_number INTEGER,
    rating INTEGER,
    score INTEGER,
    tournament_id BIGINT NOT NULL REFERENCES "tournaments"(id)
);

-- Custom user table
CREATE TABLE IF NOT EXISTS custom_user (
    username VARCHAR(255) PRIMARY KEY,
    password VARCHAR(255)
);
