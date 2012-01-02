SET COMPRESS_LOB DEFLATE;
----
SET DEFAULT_LOCK_TIMEOUT 120000;
----
set WRITE_DELAY 3000;           -- default 500ms
----
CREATE TABLE users (
  id INTEGER PRIMARY KEY auto_increment,
  email VARCHAR UNIQUE,
  name VARCHAR,
  conf VARCHAR,                 -- json string
  model VARCHAR,                -- json string
  password VARCHAR,
  authen_token VARCHAR,
  added_ts INTEGER              -- interger is easier to serialize
);

----
create table crawler_links (
  id INTEGER PRIMARY KEY auto_increment,
  url VARCHAR UNIQUE,
  domain VARCHAR UNIQUE,        --assume one domain, one rss
  added_ts TIMESTAMP default now(),
  title VARCHAR,
  next_check_ts INTEGER default 1,
  last_modified VARCHAR,
  check_interval INTEGER default 60 * 60 * 24 * 30, -- in seconds, 1 month
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
  last_status INTEGER,
  check_interval INTEGER default 60 * 60 * 8, -- seconds, in 8 h, min 1.5h
  last_modified VARCHAR,        -- from http response header
  user_id INTEGER      -- who first add it, REFERENCES users(no index)
)

----
CREATE TABLE user_subscription (
  id INTEGER PRIMARY KEY auto_increment,
  user_id INTEGER NOT NULL
       REFERENCES users  ON UPDATE CASCADE ON DELETE CASCADE,
  rss_link_id INTEGER NOT NULL
       REFERENCES rss_links  ON UPDATE CASCADE ON DELETE CASCADE,
  title VARCHAR, --user defined title, default is subscription's title
  group_name VARCHAR,
  sort_index INTEGER default 0, --sort index, keep track of position
  added_ts TIMESTAMP DEFAULT now()
  -- UNIQUE (user_id, rss_link_id)
);

----
CREATE TABLE feeds (
  id INTEGER PRIMARY KEY auto_increment,
  author VARCHAR,
  link VARCHAR,
  title VARCHAR,
  summary CLOB,
  original CLOB,
  tags VARCHAR,
  updated_ts INTEGER,
  published_ts INTEGER,
  fetched_ts INTEGER,
  rss_link_id INTEGER
             REFERENCES rss_links ON UPDATE CASCADE ON DELETE CASCADE,
  UNIQUE(link, rss_link_id)
);

----
create table user_feed (
    user_id INTEGER NOT NULL
            REFERENCES users ON UPDATE CASCADE ON DELETE CASCADE,
    feed_id INTEGER NOT NULL
            REFERENCES feeds ON UPDATE CASCADE ON DELETE CASCADE,
    vote INTEGER default 0,    -- like 1, dislike -1, no pref 0
    vote_sys DOUBLE default 0, -- learn by program
    read_date INTEGER default -1, -- the reading date, -1, unread
    UNIQUE(user_id, feed_id),
)

----
create table favicon (
     hostname VARCHAR primary key,
     favicon BINARY,
     code INTEGER               -- fetch result's http status code
)

----
create index idx_link_check_ts on crawler_links(next_check_ts)
----
create index idx_rss_check_ts on rss_links(next_check_ts)
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
