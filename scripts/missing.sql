-- need to keep in sync with rssminer.sql

-- USE `rssminer`;

create table IF NOT EXISTS feed_data (
  id INT UNSIGNED PRIMARY KEY,
  summary MEDIUMTEXT
);

create table IF NOT EXISTS favicon (
     hostname VARCHAR(96) PRIMARY KEY,
     favicon BLOB,  -- max 64k
     -- SMALLINT, 0, 35536, 2 bytes storage
     code SMALLINT UNSIGNED     -- fetch result's http status code
);
