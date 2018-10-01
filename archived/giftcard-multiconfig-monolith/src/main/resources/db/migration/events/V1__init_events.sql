CREATE SEQUENCE hibernate_sequence
  START 1
  INCREMENT 1;

CREATE TABLE domain_event_entry (
  global_index         INT8         NOT NULL,
  event_identifier     VARCHAR(255) NOT NULL,
  meta_data            OID,
  payload              OID          NOT NULL,
  payload_revision     VARCHAR(255),
  payload_type         VARCHAR(255) NOT NULL,
  time_stamp           VARCHAR(255) NOT NULL,
  aggregate_identifier VARCHAR(255) NOT NULL,
  sequence_number      INT8         NOT NULL,
  type                 VARCHAR(255),
  PRIMARY KEY (global_index)
);

CREATE TABLE snapshot_event_entry (
  aggregate_identifier VARCHAR(255) NOT NULL,
  sequence_number      INT8         NOT NULL,
  type                 VARCHAR(255) NOT NULL,
  event_identifier     VARCHAR(255) NOT NULL,
  meta_data            OID,
  payload              OID          NOT NULL,
  payload_revision     VARCHAR(255),
  payload_type         VARCHAR(255) NOT NULL,
  time_stamp           VARCHAR(255) NOT NULL,
  PRIMARY KEY (aggregate_identifier, sequence_number, type)
);

ALTER TABLE domain_event_entry
  ADD CONSTRAINT UK8s1f994p4la2ipb13me2xqm1w UNIQUE (aggregate_identifier, sequence_number);

ALTER TABLE domain_event_entry
  ADD CONSTRAINT UK_fwe6lsa8bfo6hyas6ud3m8c7x UNIQUE (event_identifier);

ALTER TABLE snapshot_event_entry
  ADD CONSTRAINT UK_e1uucjseo68gopmnd0vgdl44h UNIQUE (event_identifier);
