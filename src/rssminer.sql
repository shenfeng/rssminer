CREATE TABLE users (
  id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  email VARCHAR(64) UNIQUE,
  name VARCHAR(64),
  conf VARCHAR(1024),           -- json string
  `password` VARCHAR(32),
  provider VARCHAR(10),         -- openid provider, eg: google
  authen_token VARCHAR(32),
  added_ts TIMESTAMP DEFAULT now() -- interger is easier to serialize
);

CREATE TABLE rss_links (
  id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  url VARCHAR(220) UNIQUE,
  title VARCHAR(1024),
  description VARCHAR(1024),
  alternate VARCHAR(220), -- usually, the site's link
  added_ts TIMESTAMP DEFAULT now(),
  next_check_ts INT UNSIGNED DEFAULT 1,
  last_status SMALLINT UNSIGNED,
  check_interval SMALLINT DEFAULT 14400, -- seconds, in 4 h, min 1.5h
  last_modified VARCHAR(64),            -- from http response header
  user_id INT UNSIGNED,      -- who first add it, REFERENCES users(no index)
  INDEX idx_rss_check_ts (next_check_ts)
);

CREATE TABLE feeds (
  id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  author VARCHAR(24),
  link VARCHAR(220),
  title VARCHAR(256),
  -- saved in lucene index, updated by orginal if fetched. not saved in db
  -- summary CLOB,
  -- original CLOB,
  final_link VARCHAR(256),
  tags VARCHAR(128),
  updated_ts INT UNSIGNED,
  published_ts INT UNSIGNED,
  fetched_ts INT UNSIGNED,
  rss_link_id INT UNSIGNED,
             -- REFERENCES rss_links ON UPDATE CASCADE ON DELETE CASCADE,
  UNIQUE rss_link_id_link (rss_link_id, link)
);

CREATE TABLE user_subscription (
  id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- id INTEGER PRIMARY KEY auto_increment,
  user_id INT UNSIGNED,
    -- REFERENCES users ON UPDATE CASCADE ON DELETE CASCADE,
  rss_link_id INT UNSIGNED,
   -- REFERENCES rss_links  ON UPDATE CASCADE ON DELETE CASCADE,
  title VARCHAR(256), -- user defined title, default is subscription's title
  group_name VARCHAR(64),
  -- keep track of position, smallint is just ok
  sort_index SMALLINT UNSIGNED default 0,
  added_ts TIMESTAMP DEFAULT now(),
  UNIQUE (user_id, rss_link_id)
);

create table user_feed (
    user_id INT UNSIGNED,
    feed_id INT UNSIGNED,
    -- tiny int require 1 bytes, -128 127
    -- like 1, dislike -1, no pref 0
    vote_user TINYINT default 0,
    vote_sys FLOAT default 0,    -- learn by program
    read_date INT default -1,    -- the reading date, -1, unread
    INDEX user_feed_id (user_id, feed_id)
    -- REFERENCES users ON UPDATE CASCADE ON DELETE CASCADE,
    -- REFERENCES feeds ON UPDATE CASCADE ON DELETE CASCADE,
    -- FOREIGN KEY (user_id) REFERENCES
    -- PRIMARY KEY(user_id, feed_id)
);

create table favicon (
     hostname VARCHAR(96) PRIMARY KEY,
     favicon VARBINARY(65536),  -- 64k
     -- SMALLINT, 0, 35536, 2 bytes storage
     code SMALLINT UNSIGNED     -- fetch result's http status code
);


delimiter //

CREATE PROCEDURE get_unvoted_feedids (user_id INT, published_ts INT)
BEGIN
SELECT p.*
FROM   (SELECT f.id
        FROM   feeds f
               JOIN user_subscription us
                 ON us.rss_link_id = f.rss_link_id
        WHERE  us.user_id = user_id
               AND f.published_ts > published_ts) p
       LEFT JOIN (SELECT feed_id AS id
                  FROM   user_feed
                  WHERE  user_id = user_id
                         AND vote_user != 0) q
         ON p.id = q.id
WHERE  q.id IS NULL;
END //


CREATE PROCEDURE get_user_subs (user_id_p INT, mark_as_read_time_p INT, like_s_p DOUBLE, neutral_s_p DOUBLE)
BEGIN
SELECT us.rss_link_id                                   AS id,
       us.group_name,
       l.url,
       us.sort_index,
       us.title,
       l.title                                          AS o_title,
       (SELECT COUNT(*)
        FROM   feeds
               LEFT JOIN user_feed
                 ON feeds.id = user_feed.feed_id
        WHERE  rss_link_id = us.rss_link_id
               AND published_ts > mark_as_read_time_p
               AND ( user_feed.read_date < 1
                      OR user_feed.read_date IS NULL )) AS total_c,
       (SELECT COUNT(*)
        FROM   feeds
               LEFT JOIN user_feed
                 ON feeds.id = user_feed.feed_id
        WHERE  rss_link_id = us.rss_link_id
               AND published_ts > mark_as_read_time_p
               AND user_feed.read_date < 1
               AND user_feed.vote_sys > like_s_p)       AS like_c,
       (SELECT COUNT(*)
        FROM   feeds
               LEFT JOIN user_feed
                 ON feeds.id = user_feed.feed_id
        WHERE  rss_link_id = us.rss_link_id
               AND published_ts > mark_as_read_time_p
               AND user_feed.read_date < 1
               AND user_feed.vote_sys < neutral_s_p)    AS dislike_c
FROM   user_subscription us
       JOIN rss_links l
         ON l.id = us.rss_link_id
WHERE  us.user_id = user_id_p;
END //

delimiter ;
