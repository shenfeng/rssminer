--http://www.postgresql.org/docs/current/static/datatype-character.html
CREATE TABLE users
(
  id serial PRIMARY KEY,
  "name" character varying,
  email character varying UNIQUE,
  "password" character varying,
  authen_toekn character varying,
  added_ts timestamp with time zone DEFAULT now()
);
----
CREATE TABLE subscriptions
(
  id serial PRIMARY KEY,
  link character varying UNIQUE,       -- the feed link
  alternate character varying,         -- usually, the site's link
  title character varying,
  description text,
  favicon text,                 -- base64 encoded
  last_check_ts timestamp with time zone,
  last_update_ts timestamp with time zone, -- Last-Modified header?
  -- select * from A where EXTRACT(EPOCH FROM current_timestamp - last_check_ts) > check_interval;
  check_interval integer default 60 * 60 * 24, -- in seconds, default one day, will adapt
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
  group_name character varying default 'ungrouped',
  added_ts timestamp with time zone DEFAULT now(),
  PRIMARY KEY (user_id, subscription_id)
);
----
CREATE TABLE feeds
(
  id serial PRIMARY KEY,
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
----
CREATE TABLE crawl_logs
(
   id serial primary key,
   subscription_id integer NOT NULL
      REFERENCES subscriptions MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE,
   added_ts timestamp with time zone default now(),
   file character varying,      --save as string, if file is not changed, it's null
   feeds_added integer,         --how many new feeds,
   is_error boolean DEFAULT FALSE,
   error_mesg character varying -- exception message
)
