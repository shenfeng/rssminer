CREATE TABLE users (
  id INTEGER PRIMARY KEY auto_increment,
  email VARCHAR(64) unique,
  name VARCHAR(64),
  conf VARCHAR(1024),           -- json string
  `password` VARCHAR(32),
  provider VARCHAR(10),         -- openid provider, eg: google
  authen_token VARCHAR(32),
  added_ts INTEGER              -- interger is easier to serialize
);

----
CREATE TABLE rss_links (
  id INTEGER PRIMARY KEY AUTO_INCREMENT,
  url VARCHAR(220) unique,
  title VARCHAR(1024),
  description VARCHAR(1024),
  alternate VARCHAR(220), -- usually, the site's link
  added_ts TIMESTAMP default now(),
  next_check_ts INTEGER default 1,
  last_status INTEGER,
  check_interval INTEGER default 14400, -- seconds, in 4 h, min 1.5h
  last_modified VARCHAR(64),            -- from http response header
  user_id INTEGER,      -- who first add it, REFERENCES users(no index)
  INDEX idx_rss_check_ts (next_check_ts)
);

----
CREATE TABLE feeds (
  id INTEGER PRIMARY KEY auto_increment,
  author VARCHAR(24),
  link VARCHAR(220),
  title VARCHAR(256),
  -- saved in lucene index, updated by orginal if fetched. not saved in db
  -- summary CLOB,
  -- original CLOB,
  final_link VARCHAR(256),
  tags VARCHAR(128),
  updated_ts INTEGER,
  published_ts INTEGER,
  fetched_ts INTEGER,
  rss_link_id INTEGER,
             -- REFERENCES rss_links ON UPDATE CASCADE ON DELETE CASCADE,
  UNIQUE(rss_link_id, link)
);

----
CREATE TABLE user_subscription (
  id INTEGER PRIMARY KEY auto_increment,
  user_id INTEGER NOT NULL
       REFERENCES users ON UPDATE CASCADE ON DELETE CASCADE,
  rss_link_id INTEGER NOT NULL
      REFERENCES rss_links  ON UPDATE CASCADE ON DELETE CASCADE,
  title VARCHAR(256), -- user defined title, default is subscription's title
  group_name VARCHAR(64),
   -- keep track of position
  sort_index INTEGER default 0,
  added_ts TIMESTAMP DEFAULT now(),
  UNIQUE (user_id, rss_link_id)
);

----
create table user_feed (
    user_id INTEGER NOT NULL
            REFERENCES users ON UPDATE CASCADE ON DELETE CASCADE,
    feed_id INTEGER NOT NULL
            REFERENCES feeds ON UPDATE CASCADE ON DELETE CASCADE,
    vote INTEGER default 0,       -- like 1, dislike -1, no pref 0
    vote_sys DOUBLE default 0,    -- learn by program
    read_date INTEGER default -1, -- the reading date, -1, unread
    PRIMARY KEY(user_id, feed_id)
)

----
create table favicon (
     hostname VARCHAR(96) primary key,
     favicon BINARY,
     code INTEGER               -- fetch result's http status code
)

----
-- delimiter //

CREATE PROCEDURE get_unvoted_feedids (user_id INT, published_ts INT)
BEGIN
SELECT p.*
FROM (
SELECT f.id
FROM feeds f
JOIN user_subscription us ON us.rss_link_id = f.rss_link_id
WHERE us.user_id = user_id AND f.published_ts > published_ts) p
LEFT JOIN (
SELECT feed_id AS id
FROM user_feed
WHERE user_id = user_id AND vote != 0) q ON p.id = q.id
WHERE q.id IS NULL;
END

-- delimiter ;

----
