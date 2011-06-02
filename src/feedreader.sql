CREATE TABLE users
(
  id serial NOT NULL,
  email character varying(100),
  "password" character varying(100),
  "name" character varying(30),
  CONSTRAINT pk_users PRIMARY KEY (id),
  CONSTRAINT uniq_users_email UNIQUE (email)
);
----
CREATE TABLE feedsources
(
  id serial NOT NULL,
  link character varying(300),
  title character varying(300),
  description text,
  last_check timestamp with time zone,
  last_update timestamp with time zone,
  favicon text,
  CONSTRAINT uniq_feedsources_link UNIQUE (link),
  CONSTRAINT pk_feedsources PRIMARY KEY (id)
);
----
CREATE TABLE user_feedsource
(
  user_id integer NOT NULL,
  feedsource_id integer NOT NULL,
  CONSTRAINT pk_userfeedsource PRIMARY KEY (user_id, feedsource_id),
  CONSTRAINT fk_userfeedsource_feedsourceid FOREIGN KEY (feedsource_id)
      REFERENCES feedsources (id) MATCH SIMPLE
      ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_userfeedsource_userid FOREIGN KEY (user_id)
      REFERENCES users (id) MATCH SIMPLE
      ON UPDATE CASCADE ON DELETE CASCADE
);
----
CREATE TABLE feeds
(
  id serial NOT NULL,
  feedsource_id integer,
  guid character varying(200), --uniqe string per item
  author character varying(50),
  title character varying(200),
  link character varying(200),
  updated timestamp with time zone,
  pub_date timestamp with time zone,
  description text,
  CONSTRAINT pk_feeds PRIMARY KEY (id),
  CONSTRAINT fk_feeds_feedsourceid FOREIGN KEY (feedsource_id)
      REFERENCES feedsources (id) MATCH SIMPLE
      ON UPDATE CASCADE ON DELETE CASCADE
);
----
CREATE TABLE user_feed
(
  user_id integer NOT NULL,
  feed_id integer NOT NULL,
  CONSTRAINT pk_userfeed PRIMARY KEY (user_id, feed_id),
  CONSTRAINT fk_userfeed_feedid FOREIGN KEY (feed_id)
      REFERENCES feeds (id) MATCH SIMPLE
      ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_userfeed_userid FOREIGN KEY (user_id)
      REFERENCES users (id) MATCH SIMPLE
      ON UPDATE CASCADE ON DELETE CASCADE
)
