CREATE TABLE IF NOT EXISTS compilations (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    pinned BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_compilations_pinned ON compilations(pinned);

CREATE TABLE IF NOT EXISTS compilation_events (
    compilation_id BIGINT NOT NULL REFERENCES compilations(id) ON DELETE CASCADE,
    event_id BIGINT NOT NULL,
    PRIMARY KEY (compilation_id, event_id)
);
CREATE INDEX IF NOT EXISTS idx_compilation_events_compilation_id ON compilation_events(compilation_id);
CREATE INDEX IF NOT EXISTS idx_compilation_events_event_id ON compilation_events(event_id);