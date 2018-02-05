DROP DATABASE IF EXISTS giftcard_primary;
DROP DATABASE IF EXISTS giftcard_events;
DROP ROLE IF EXISTS giftcard;

CREATE ROLE giftcard LOGIN
  PASSWORD 'secret';

CREATE DATABASE giftcard_primary
  WITH OWNER = giftcard;

CREATE DATABASE giftcard_events
  WITH OWNER = giftcard;

