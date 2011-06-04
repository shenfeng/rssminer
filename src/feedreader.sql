--http://www.postgresql.org/docs/current/static/datatype-character.html
CREATE TABLE users
(
  id serial NOT NULL,
  email character varying,
  "name" character varying,
  "password" character varying,
  authen_toekn character varying,
  added_ts timestamp with time zone DEFAULT now(),
  CONSTRAINT pk_users PRIMARY KEY (id),
  CONSTRAINT uniq_users_email UNIQUE (email)
);
----
CREATE TABLE subscriptions
(
  id serial NOT NULL,
  link character varying,       -- the fee link
  alternate character varying,  -- usually, the site's link
  title character varying,
  description text,
  favicon text,                 -- base 64 encoded
  last_check_ts timestamp with time zone,
  last_update_ts timestamp with time zone,
  added_ts timestamp with time zone DEFAULT now(),
  user_id integer NOT NULL,     -- who first add it
  CONSTRAINT pk_subscriptions PRIMARY KEY (id),
  CONSTRAINT uniq_subscriptions_link UNIQUE (link)
);
----
CREATE TABLE user_subscription
(
  user_id integer NOT NULL,
  subscription_id integer NOT NULL,
  group_name character varying default 'freader_ungrouped',
  added_ts timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT pk_user_subscription PRIMARY KEY (user_id, subscription_id),
  CONSTRAINT fk_user_subscription_subscriptionid FOREIGN KEY (subscription_id)
      REFERENCES subscriptions (id) MATCH SIMPLE
      ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_user_subscription_userid FOREIGN KEY (user_id)
      REFERENCES users (id) MATCH SIMPLE
      ON UPDATE CASCADE ON DELETE CASCADE
);
----
CREATE TABLE feeds
(
  id serial NOT NULL,
  subscription_id integer,
  author character varying,
  title character varying,
  summary text,
  alternate character varying,  -- url
  updated_ts timestamp with time zone,
  published_ts timestamp with time zone,
  crawl_ts timestamp with time zone DEFAULT now(),
  CONSTRAINT pk_feeds PRIMARY KEY (id),
  CONSTRAINT fk_feeds_subscriptionid FOREIGN KEY (subscription_id)
      REFERENCES subscriptions (id) MATCH SIMPLE
      ON UPDATE CASCADE ON DELETE CASCADE
);
----
CREATE TABLE comments
(
  id serial NOT NULL,
  "content" text NOT NULL,
  user_id integer NOT NULL,
  feed_id integer NOT NULL,
  added_ts timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT pk_comments PRIMARY KEY (id),
  CONSTRAINT fk_comments_feedid FOREIGN KEY (feed_id)
      REFERENCES feeds (id) MATCH SIMPLE
      ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_comments_userid FOREIGN KEY (user_id)
      REFERENCES users (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);
---
create table feedcategory
(
    "type" character varying, -- possible val: tag, freader(system type), 
    "text" character varying, -- freader-> stared, read
    user_id integer NOT NULL,
    feed_id integer NOT NULL,
    added_ts timestamp with time zone not null default now(),
   CONSTRAINT pk_feedcategory PRIMARY KEY("type", "text", user_id, feed_id),
   CONSTRAINT fk_feedcategory_userid FOREIGN KEY(user_id)
     REFERENCES users(id) MATCH SIMPLE
     ON UPDATE CASCADE ON DELETE CASCADE,
   CONSTRAINT fk_feedcategory_feedid FOREIGN KEY(feed_id)
     REFERENCES feeds(id) MATCH SIMPLE
     ON UPDATE CASCADE ON DELETE CASCADE
);

