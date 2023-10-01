DROP TABLE cardsummary;
CREATE TABLE IF NOT EXISTS cardsummary
(
    id             VARCHAR(132),
    initialValue   INTEGER,
    remainingValue INTEGER,
    issued         TIMESTAMP WITH TIME ZONE,
    lastUpdated    TIMESTAMP WITH TIME ZONE
);