CREATE TABLE users
(
  id serial NOT NULL,
  email character varying(100),
  "password" character varying(100),
  "name" character varying(30),
  CONSTRAINT pk_users PRIMARY KEY (id),
  CONSTRAINT uniq_users_email UNIQUE (email)
)
----
CREATE TABLE feedsources
(
  id serial NOT NULL,
  uri character varying(300),
  last_check timestamp with time zone,
  last_update timestamp with time zone,
  CONSTRAINT pk_feedsources PRIMARY KEY (id)
)
----
CREATE TABLE user_feedsource
(
  user_id integer NOT NULL,
  feedsource_id integer NOT NULL,
  CONSTRAINT pk_userfeedsource PRIMARY KEY (user_id, feedsource_id),
  CONSTRAINT fk_userfeedsource_feedsourceid FOREIGN KEY (feedsource_id)
      REFERENCES feedsource (id) MATCH SIMPLE
      ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_userfeedsource_userid FOREIGN KEY (user_id)
      REFERENCES users (id) MATCH SIMPLE
      ON UPDATE CASCADE ON DELETE CASCADE
)
