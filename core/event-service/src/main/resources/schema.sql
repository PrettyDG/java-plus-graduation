CREATE TABLE IF NOT EXISTS locations (
    id SERIAL PRIMARY KEY,
    lat DOUBLE PRECISION NOT NULL,
    lon DOUBLE PRECISION NOT NULL,
    UNIQUE (lat, lon)
);
CREATE INDEX IF NOT EXISTS idx_locations_lat_lon ON locations(lat, lon);

CREATE TABLE IF NOT EXISTS events (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    annotation TEXT NOT NULL,
    category_id BIGINT NOT NULL REFERENCES categories(id),
    paid BOOLEAN NOT NULL,
    event_date TIMESTAMP NOT NULL,
    initiator_id BIGINT NOT NULL REFERENCES users(id),
    description TEXT NOT NULL,
    participant_limit INT NOT NULL DEFAULT 0,
    state VARCHAR(255),
    created_on TIMESTAMP NOT NULL DEFAULT now(),
    location_id INT NOT NULL REFERENCES locations(id),
    request_moderation BOOLEAN NOT NULL DEFAULT TRUE,
    published_on TIMESTAMP WITHOUT TIME ZONE,
    confirmed_requests INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_events_category_id ON events(category_id);
CREATE INDEX IF NOT EXISTS idx_events_event_date ON events(event_date);
CREATE INDEX IF NOT EXISTS idx_events_initiator_id ON events(initiator_id);
CREATE INDEX IF NOT EXISTS idx_events_state ON events(state);
CREATE INDEX IF NOT EXISTS idx_events_location_id ON events(location_id);
CREATE INDEX IF NOT EXISTS idx_events_paid ON events(paid);