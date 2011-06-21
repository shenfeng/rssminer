--http://www.postgresql.org/docs/current/static/datatype-character.html
CREATE TABLE users
(
  id serial NOT NULL PRIMARY KEY,
  email character varying UNIQUE,
  "name" character varying,
  "password" character varying,
  authen_toekn character varying,
  added_ts timestamp with time zone DEFAULT now()
);
----
CREATE TABLE subscriptions
(
  id serial NOT NULL PRIMARY KEY,
  link character varying UNIQUE,       -- the feed link
  alternate character varying,  -- usually, the site's link
  title character varying,
  description text,
  favicon text,                 -- base 64 encoded
  last_check_ts timestamp with time zone,
  last_update_ts timestamp with time zone,
  added_ts timestamp with time zone DEFAULT now(),
  user_id integer               -- who first add it
      REFERENCES users MATCH SIMPLE ON UPDATE CASCADE ON DELETE SET NULL
);
----
CREATE TABLE user_subscription
(
  user_id integer NOT NULL
       REFERENCES users MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE,
  subscription_id integer NOT NULL
       REFERENCES subscriptions MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE,
  title character varying, --user defined title, default is subscription's title
  group_name character varying default 'freader_ungrouped',
  added_ts timestamp with time zone NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, subscription_id)
);
----
CREATE TABLE feeds
(
  id serial NOT NULL PRIMARY KEY,
  subscription_id integer NOT NULL
                   REFERENCES subscriptions MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE,
  author character varying,
  title character varying,
  summary text,
  alternate character varying,  -- url
  updated_ts timestamp with time zone,
  published_ts timestamp with time zone,
  crawl_ts timestamp with time zone DEFAULT now()
);
----
CREATE TABLE comments
(
  id serial NOT NULL PRIMARY KEY,
  "content" text NOT NULL,
  user_id integer NOT NULL
          REFERENCES users MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE,
  feed_id integer NOT NULL
          REFERENCES feeds MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE,
  added_ts timestamp with time zone NOT NULL DEFAULT now()
);
---
CREATE TABLE feedcategory
(
    "type" character varying, -- possible val: tag, freader(system type),
    "text" character varying, -- freader-> stared, read
    user_id integer NOT NULL
            REFERENCES users MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE,
    feed_id integer NOT NULL
            REFERENCES feeds MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE,
    added_ts timestamp with time zone NOT NULL DEFAULT now(),
    PRIMARY KEY("type", "text", user_id, feed_id)
);

