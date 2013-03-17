CREATE TABLE users (
  id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  email VARCHAR(64) UNIQUE,
  name VARCHAR(64),
  conf VARCHAR(1024),           -- json string
  -- 2012/5/29
  -- alter table users add like_score double not null after conf
  -- alter table users add neutral_score double not null after like_score;
  -- alter table drop scores;
  like_score DOUBLE NOT NULL default 1.0,
  neutral_score DOUBLE NOT NULL default 0,
  -- alter table users add scores varchar(32) after conf;
  -- scores VARCHAR(32),            -- like score and
  `password` VARCHAR(32),
  provider VARCHAR(10),         -- openid provider, eg: google
  authen_token VARCHAR(32),
  added_ts TIMESTAMP DEFAULT now() -- timestamp is easier to read
);

CREATE TABLE rss_links (
  id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  url VARCHAR(512) NOT NULL,
  title VARCHAR(1024),
  description VARCHAR(1024),
  alternate VARCHAR(512),       -- usually, the site's link
  added_ts TIMESTAMP DEFAULT now(),
  next_check_ts INT UNSIGNED NOT NULL DEFAULT 1,
  last_status SMALLINT UNSIGNED NOT NULL DEFAULT 0,
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
  INDEX url(url),
  INDEX idx_rss_check_ts (next_check_ts)
);

CREATE TABLE feeds (
  id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  author VARCHAR(64) NOT NULL default '',

  --  ALTER TABLE feeds MODIFY link VARCHAR(512) NOT NULL
  link VARCHAR(512) NOT NULL,
  title VARCHAR(256) NOT NULL default '',
  -- 2012/5/27
  -- alter table feeds drop original
  -- alter table feeds drop final_link
  -- original MEDIUMTEXT,          -- max 16m, orignal web page
  -- 2012/4/18
  -- ALTER TABLE feeds ADD COLUMN original MEDIUMTEXT
  -- ALTER TABLE feeds ADD COLUMN summary MEDIUMTEXT
  -- final_link VARCHAR(256),
  tags VARCHAR(128) NOT NULL default '',
  -- link_hash int NOT NULL DEFAULT 0,
  updated_ts INT UNSIGNED NOT NULL default 0,
  published_ts INT UNSIGNED NOT NULL default 0,
  -- fetched_ts INT UNSIGNED,
  rss_link_id INT UNSIGNED NOT NULL,
  simhash bigint NOT NULL DEFAULT -1,
  key simhash_idx(simhash),
  key rss_id_url(rss_link_id, link(70))

  -- summary MEDIUMTEXT,           -- rss summary, given by download rss

  -- alter table feeds drop index rss_link_id_link;
  -- alter table feeds add index rss_link_id_link(rss_link_id, link);
             -- REFERENCES rss_links ON UPDATE CASCADE ON DELETE CASCADE,
  -- 9977856 vs 83886080 compared to index on link
  -- key rss_id_link_hash (rss_link_id, link_hash)
);

create table feed_data (
  id INT UNSIGNED PRIMARY KEY,
  summary MEDIUMTEXT            -- from rss
  -- content MEDIUMTEXT            -- extract content, need working
)ENGINE=InnoDB DEFAULT CHARSET=utf8 KEY_BLOCK_SIZE=4;

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
    vote_user TINYINT NOT NULL default 0,
    -- alter table user_feed change vote_sys vote_sys DOUBLE default 0;
    -- float => double 2012/4/30
    -- vote_sys DOUBLE default 0,  -- learn by program
    read_date INT NOT NULL default -1,   -- the reading date, -1, unread
    read_time MEDIUMINT UNSIGNED NOT NULL DEFAULT 0,
    -- 2012/5/27
    -- alter table user_feed add vote_date int default -1 after read_date
    -- alter table user_feed change rss_link_id rss_link_id int unsigned not null default 0 after feed_id;
    vote_date INT NOT NULL default -1,   -- the user vote date
    -- 2012/4/29 --replace index with unique index to support upsert
    -- insert into user_feed (user_id, feed_id, vote_user) values (1, 557, 1) on duplicate key update vote_user = 10;
    -- alter table user_feed drop index user_feed_id
    -- alter table user_feed add unique index user_feed_id(user_id, feed_id)
    -- 2012/5/29
    -- alter table user_feed drop index user_feed_id;
    -- alter table user_feed add primary key (user_id, feed_id);
    -- alter table user_feed drop column vote_sys
    -- alter table user_feed drop index user_rsslink
    PRIMARY KEY(user_id, feed_id)
    -- UNIQUE user_feed_id(user_id, feed_id),
    -- INDEX user_rsslink(user_id, rss_link_id)
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
)ENGINE=InnoDB DEFAULT CHARSET=utf8 KEY_BLOCK_SIZE=2;

create table feedback (
     id int unsigned primary key auto_increment,
     email varchar(64),
     ip varchar(15) not null,
     feedback text,
     refer varchar(1024),
     user_id int unsigned,
     added_ts TIMESTAMP DEFAULT now() -- timestamp is easier to read
);

delimiter //

create PROCEDURE get_unvoted (p_uid INT)
BEGIN
SELECT f.id, f.rss_link_id, f.published_ts
FROM feeds f
JOIN user_subscription us ON f.rss_link_id = us.rss_link_id AND us.user_id = p_uid
where f.id not in (select feed_id from user_feed where user_id = p_uid )
ORDER BY published_ts DESC
LIMIT 6000;

END //

-- delimiter //
CREATE PROCEDURE get_voted (p_uid INT) BEGIN
(
SELECT feed_id, vote_user
FROM user_feed
WHERE vote_user != 0 AND user_id = p_uid
ORDER BY vote_date DESC
LIMIT 100) UNION ALL (
SELECT feed_id, vote_user
FROM user_feed
WHERE user_id = p_uid AND read_date > 0
ORDER BY read_date DESC
LIMIT 150);

END //

insert into users (email) values ('demo@rssminer.net');
