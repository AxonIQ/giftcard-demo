CREATE TABLE token_entry (
  processor_name VARCHAR(255) NOT NULL,
  segment        INT4         NOT NULL,
  owner          VARCHAR(255),
  timestamp      VARCHAR(255) NOT NULL,
  token          OID,
  token_type     VARCHAR(255),
  PRIMARY KEY (processor_name, segment)
);

CREATE TABLE card_summary (
  id              VARCHAR(255) NOT NULL,
  initial_value   INT4,
  issued_at       BYTEA,
  remaining_value INT4,
  PRIMARY KEY (id)
);
