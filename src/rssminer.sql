--rlwrap java -cp /tmp/h2-1.3.158.jar org.h2.tools.Shell -url jdbc:h2:/tmp/test/crawler_tmph92 -user sa -password sa

-- select datediff('SECOND', add_ts, now()) - check_interval as t from rss_link order by t limit 1

SET COMPRESS_LOB DEFLATE;
----
SET DEFAULT_LOCK_TIMEOUT 120000;
----
SET CACHE_SIZE 16384;
----
set WRITE_DELAY 3000;           -- default 500ms
----
CREATE TABLE users
(
  id serial PRIMARY KEY,
  email VARCHAR UNIQUE,
  name VARCHAR,
  password VARCHAR,
  authen_toekn VARCHAR,
  added_ts timestamp DEFAULT now()
);

----
create table crawler_links (
  id INTEGER PRIMARY KEY auto_increment,
  url VARCHAR UNIQUE,
  domain VARCHAR UNIQUE,        --assume one domain, one rss
  added_ts TIMESTAMP default now(),
  title VARCHAR,
  next_check_ts INTEGER default 10,
  last_modified VARCHAR,
  check_interval INTEGER default 60 * 60 * 24 * 10, -- in seconds, ten days
  referer_id INTEGER REFERENCES crawler_links
      ON UPDATE CASCADE ON DELETE SET NULL,
)

----
create table rss_links (
  id INTEGER PRIMARY KEY auto_increment,
  url VARCHAR UNIQUE,
  title VARCHAR,
  description VARCHAR,
  alternate VARCHAR,            -- usually, the site's link
  added_ts TIMESTAMP default now(),
  next_check_ts INTEGER default 1,
  check_interval INTEGER default 60 * 60 * 24, -- in seconds, one day
  last_modified VARCHAR,      -- from http response header
  favicon CLOB,               -- base64 encoded
  subscription_count INTEGER default 0, -- how much user subscribed
  user_id INTEGER REFERENCES users      -- who first add it
     ON UPDATE CASCADE ON DELETE SET NULL,
  crawler_link_id INTEGER  REFERENCES crawler_links
     ON UPDATE CASCADE ON DELETE SET NULL,
)
----
CREATE TABLE user_subscription
(
  id INTEGER PRIMARY KEY auto_increment,
  user_id INTEGER NOT NULL
       REFERENCES users  ON UPDATE CASCADE ON DELETE CASCADE,
  rss_link_id INTEGER NOT NULL
       REFERENCES rss_links  ON UPDATE CASCADE ON DELETE CASCADE,
  title VARCHAR, --user defined title, default is subscription's title
  group_name VARCHAR,
  added_ts TIMESTAMP DEFAULT now(),
  UNIQUE (user_id, rss_link_id)
);
----
CREATE TABLE feeds
(
  id INTEGER PRIMARY KEY auto_increment,
  author VARCHAR,
  link VARCHAR UNIQUE,
  title VARCHAR,
  summary CLOB,
  snippet VARCHAR,
  tags VARCHAR,                 -- tags by feed author(parsed)
  updated_ts TIMESTAMP,
  published_ts TIMESTAMP,
  rss_link_id INTEGER
             REFERENCES rss_links ON UPDATE CASCADE ON DELETE CASCADE
);
----
CREATE TABLE comments
(
  id INTEGER PRIMARY KEY auto_increment,
  content VARCHAR,
  user_id INTEGER NOT NULL
          REFERENCES users ON UPDATE CASCADE ON DELETE CASCADE,
  feed_id INTEGER NOT NULL
          REFERENCES feeds  ON UPDATE CASCADE ON DELETE CASCADE,
  added_ts TIMESTAMP  DEFAULT now(),
);
---
CREATE TABLE feed_tag
(
    id INTEGER PRIMARY KEY auto_increment,
    tag VARCHAR,
    user_id INTEGER NOT NULL
            REFERENCES users ON UPDATE CASCADE ON DELETE CASCADE,
    feed_id INTEGER NOT NULL
            REFERENCES feeds ON UPDATE CASCADE ON DELETE CASCADE,
);
---
create table user_feed_pref
(
    user_id INTEGER NOT NULL
            REFERENCES users ON UPDATE CASCADE ON DELETE CASCADE,
    feed_id INTEGER NOT NULL
            REFERENCES feeds ON UPDATE CASCADE ON DELETE CASCADE,
    pref BOOLEAN,
    UNIQUE(user_id, feed_id),
)

----
create index idx_link_check_ts on crawler_links(next_check_ts)
----
create index idx_rss_check_ts on rss_links(next_check_ts)
----
create table multi_rss_domains (
  id INTEGER PRIMARY KEY auto_increment,
  domain VARCHAR UNIQUE,
  added_ts TIMESTAMP DEFAULT now(),
)
----
create table black_domain_pattens (
   id INTEGER PRIMARY KEY auto_increment,
   patten VARCHAR UNIQUE,
   ADDED_ts TIMESTAMP DEFAULT now(),
)
----
create table reseted_domain_pattens (
   id INTEGER PRIMARY KEY auto_increment,
   patten VARCHAR UNIQUE,
   added_ts TIMESTAMP DEFAULT now(),
)
----
insert into crawler_links (url, domain) values --seeds
('http://blog.jquery.com/', 'http://blog.jquery.com'),
('http://briancarper.net/', 'http://briancarper.net'),
('http://channel9.msdn.com/', 'http://channel9.msdn.com'),
('http://clj-me.cgrand.net/', 'http://clj-me.cgrand.net'),
('http://data-sorcery.org/', 'http://data-sorcery.org'),
('http://ejohn.org/', 'http://ejohn.org'),
('http://emacs-fu.blogspot.com/', 'http://emacs-fu.blogspot.com'),
('http://emacsblog.org/', 'http://emacsblog.org'),
('http://norvig.com', 'http://norvig.com'),
('http://planet.clojure.in/', 'http://planet.clojure.in'),
('http://testdrivenwebsites.com/', 'http://testdrivenwebsites.com'),
('http://timepedia.blogspot.com/', 'http://timepedia.blogspot.com'),
('http://weblogs.asp.net/scottgu/', 'http://weblogs.asp.net'),
('http://www.masteringemacs.org/', 'http://www.masteringemacs.org'),
('http://www.ruanyifeng.com/blog/', 'http://www.ruanyifeng.com'),
('http://www.ubuntugeek.com/', 'http://www.ubuntugeek.com'),
('https://www.ibm.com/developerworks/', 'https://www.ibm.com'),
('http://www.omgubuntu.co.uk/', 'http://www.omgubuntu.co.uk'),
('http://tech2ipo.com/', 'http://tech2ipo.com'),
('http://www.dbanotes.net/', 'http://www.dbanotes.net'),
('http://xianguo.com/hot', 'http://xianguo.com')
----
insert into multi_rss_domains (domain) values
('http://blogs.oracle.com'),
----
insert into black_domain_pattens (patten) values
('\d{3,}'),
('\.a-\w+.com'),
('informer\.|typepad\.'),
('over-blog|backpage|https'),
('blshe|linkinpark|shop|soufun'),
('skyrock|tumblr|deviantart|taobao'),
('news\.|forum|bbs\.|sports\.|wap\.'),
('canalblog|livejournal|blogcu|house'),
('adult|live|cam|pussy|joyfeeds|sex|girl'),
('horny|naughty|penetrationista|suckmehere'),
----
insert into reseted_domain_pattens (patten) values
('\.blogspot\.com')
----
insert into rss_links (url) values
('http://aria42.com/blog/?feed=rss2'),
('http://bartoszmilewski.wordpress.com/feed/'),
('http://blog.higher-order.net/feed/'),
('http://blog.raek.se/feed/'),
('http://blog.sina.com.cn/rss/kaifulee.xml'),
('http://blogs.oracle.com/alexismp/feed/entries/rss'),
('http://blogs.oracle.com/briangoetz/feed/entries/rss'),
('http://blogs.oracle.com/chegar/feed/entries/rss'),
('http://cemerick.com/feed/'),
('http://clj-me.cgrand.net/feed/'),
('http://data-sorcery.org/feed/'),
('http://emacs-fu.blogspot.com/feeds/posts/default?alt=rss'),
('http://emacsblog.org/feed/'),
('http://feeds.feedburner.com/ruanyifeng'),
('http://feeds2.feedburner.com/JohnResig'),
('http://norvig.com/rss-feed.xml'),
('http://philippeadjiman.com/blog/feed/rss/'),
('http://planet.clojure.in/atom.xml'),
('http://sujitpal.blogspot.com/feeds/posts/default'),
('http://techbehindtech.com/feed/'),
('http://weblogs.asp.net/scottgu/atom.aspx'),
('http://www.alistapart.com/rss.xml'),
('http://www.ibm.com/developerworks/views/java/rss/libraryview.jsp'),
('http://www.ubuntugeek.com/feed/'),
