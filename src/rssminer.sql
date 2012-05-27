CREATE TABLE users (
  id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  email VARCHAR(64) UNIQUE,
  name VARCHAR(64),
  conf VARCHAR(1024),           -- json string
-- alter table users add scores varchar(32) after conf;
  scores VARCHAR(32),            -- like score and
  `password` VARCHAR(32),
  provider VARCHAR(10),         -- openid provider, eg: google
  authen_token VARCHAR(32),
  added_ts TIMESTAMP DEFAULT now() -- timestamp is easier to read
);

CREATE TABLE rss_links (
  id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  url VARCHAR(220) UNIQUE,
  title VARCHAR(1024),
  description VARCHAR(1024),
  alternate VARCHAR(220),       -- usually, the site's link
  added_ts TIMESTAMP DEFAULT now(),
  next_check_ts INT UNSIGNED DEFAULT 1,
  last_status SMALLINT UNSIGNED,
  error_msg VARCHAR(200),
  -- 2012/5/1
  -- alter table rss_links add total_feeds int unsigned default 0
  -- update rss_links rl set total_feeds = (select count(*) from feeds where feeds.rss_link_id = rl.id)
  total_feeds INT UNSIGNED default 0,
  check_interval MEDIUMINT DEFAULT 14400, -- seconds, in 4 h, min 3h
  -- alter table rss_links change check_interval check_interval mediumint default 14400
  last_modified VARCHAR(64),             -- from http response header
  -- 2012/5/3
  -- alter table rss_links add etag varchar(64)
  etag varchar(64),
  user_id INT UNSIGNED,      -- who first add it, REFERENCES users(no index)
  INDEX idx_rss_check_ts (next_check_ts)
);

CREATE TABLE feeds (
  id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  author VARCHAR(24),
  link VARCHAR(220),
  title VARCHAR(256),
  -- 2012/5/27
  -- alter table feeds drop original
  -- alter table feeds drop final_link
  -- original MEDIUMTEXT,          -- max 16m, orignal web page
  -- 2012/4/18
  -- ALTER TABLE feeds ADD COLUMN original MEDIUMTEXT
  -- ALTER TABLE feeds ADD COLUMN summary MEDIUMTEXT
  -- final_link VARCHAR(256),
  tags VARCHAR(128),
  updated_ts INT UNSIGNED,
  published_ts INT UNSIGNED,
  fetched_ts INT UNSIGNED,
  rss_link_id INT UNSIGNED,
  summary MEDIUMTEXT,           -- rss summary, given by download rss

             -- REFERENCES rss_links ON UPDATE CASCADE ON DELETE CASCADE,
  UNIQUE rss_link_id_link (rss_link_id, link)
);

CREATE TABLE user_subscription (
  id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  -- id INTEGER PRIMARY KEY auto_increment,
  user_id INT UNSIGNED NOT NULL,
    -- REFERENCES users ON UPDATE CASCADE ON DELETE CASCADE,
  rss_link_id INT UNSIGNED NOT NULL,
   -- REFERENCES rss_links  ON UPDATE CASCADE ON DELETE CASCADE,
  title VARCHAR(256), -- user defined title, default is subscription's title
  group_name VARCHAR(64),
  -- keep track of position, smallint is just ok, 0-65536
  sort_index SMALLINT UNSIGNED default 0,
  added_ts TIMESTAMP DEFAULT now(),
  UNIQUE (user_id, rss_link_id)
);

create table user_feed (
    user_id INT UNSIGNED,
    feed_id INT UNSIGNED,
    rss_link_id int UNSIGNED NOT NULL default 0,

    -- tiny int require 1 bytes, -128 127
    -- like 1, dislike -1, no pref 0
    vote_user TINYINT default 0,
    -- alter table user_feed change vote_sys vote_sys DOUBLE default 0;
    -- float => double 2012/4/30
    vote_sys DOUBLE default 0,  -- learn by program
    read_date INT default -1,   -- the reading date, -1, unread
    -- 2012/5/27
    -- alter table user_feed add vote_date int default -1 after read_date
    -- alter table user_feed change rss_link_id rss_link_id int unsigned not null default 0 after feed_id;
    vote_date INT default -1,   -- the user vote date
    -- 2012/4/29 --replace index with unique index to support upsert
    -- insert into user_feed (user_id, feed_id, vote_user) values (1, 557, 1) on duplicate key update vote_user = 10;
    -- alter table user_feed drop index user_feed_id
    -- alter table user_feed add unique index user_feed_id(user_id, feed_id)
    UNIQUE user_feed_id(user_id, feed_id),
    INDEX user_rsslink(user_id, rss_link_id)
    -- REFERENCES users ON UPDATE CASCADE ON DELETE CASCADE,
    -- REFERENCES feeds ON UPDATE CASCADE ON DELETE CASCADE,
    -- FOREIGN KEY (user_id) REFERENCES
    -- PRIMARY KEY(user_id, feed_id)

    -- 2012/4/30 performance
    -- ALTER TABLE user_feed add column rss_link_id int unsigned not null default 0
    -- UPDATE user_feed SET rss_link_id = (SELECT rss_link_id FROM feeds WHERE id = feed_id)
    -- ALTER TABLE user_feed add index user_rsslink (user_id, rss_link_id)
);

create table favicon (
     hostname VARCHAR(96) PRIMARY KEY,
     favicon BLOB,  -- max 64k
     -- SMALLINT, 0, 35536, 2 bytes storage
     code SMALLINT UNSIGNED     -- fetch result's http status code
);

delimiter //

-- TODO limit count
-- mysql does not support EXCEPT operator, use left join
CREATE PROCEDURE get_unvoted (user_id_p INT, published_ts_p INT)
BEGIN
SELECT p.*
FROM   (SELECT f.id,
               f.rss_link_id
        FROM   feeds f
               JOIN user_subscription us
                 ON f.rss_link_id = us.rss_link_id
                    AND us.user_id = user_id_p
        WHERE  f.published_ts > published_ts_p) p
       LEFT JOIN (SELECT feed_id
                  FROM   user_feed
                  WHERE  user_id = user_id_p
                         AND ( vote_user != 0
                                OR read_date > 0 )) q
         ON p.id = q.feed_id
WHERE  q.feed_id IS NULL;

END //

-- DROP PROCEDURE IF EXISTS `get_user_subs`;
CREATE PROCEDURE get_user_subs (user_id_p INT, like_s_p DOUBLE, neutral_s_p DOUBLE)
BEGIN
SELECT us.rss_link_id              AS id,
       us.group_name,
       l.alternate as url,
       us.sort_index,
       us.title,
       l.title                     AS o_title,
       l.total_feeds,
       (SELECT Count(*)
        FROM   user_feed
        WHERE  user_id = user_id_p
               AND rss_link_id = us.rss_link_id
               AND vote_sys > like_s_p
               AND read_date = -1) AS like_c,
       (SELECT Count(*)
        FROM   user_feed
        WHERE  user_id = user_id_p
               AND rss_link_id = us.rss_link_id
               AND vote_sys < neutral_s_p
               AND read_date = -1) AS dislike_c,
       (SELECT Count(*)
        FROM   user_feed
        WHERE  user_id = user_id_p
               AND rss_link_id = us.rss_link_id
               AND read_date = -1) AS total_c
FROM   user_subscription us
       JOIN rss_links l
         ON l.id = us.rss_link_id
WHERE  us.user_id = user_id_p;
END //

delimiter ;
