--rlwrap java -cp /tmp/h2-1.3.158.jar org.h2.tools.Shell -url jdbc:h2:/tmp/test/crawler_tmph92 -user sa -password sa

-- select datediff('SECOND', add_ts, now()) - check_interval as t from rss_link order by t limit 1

SET COMPRESS_LOB  DEFLATE;
---
create table crawler_link (
  id INTEGER PRIMARY KEY auto_increment,
  url VARCHAR UNIQUE,
  title VARCHAR,
  add_ts TIMESTAMP default now(),
  domain VARCHAR, --assume one domain, one rss
  last_status INTEGER default 200,
  last_check_ts TIMESTAMP default DATE'1980-1-1',
  last_modified TIMESTAMP default DATE'2300-1-1',
  last_md5 VARCHAR,
  check_interval INTEGER default 60 * 60 * 24 * 10, -- in seconds, ten days
  server VARCHAR,
  referer_id INTEGER REFERENCES crawler_link
      ON UPDATE CASCADE ON DELETE SET NULL,
)

---
create index idx_crawler_link_domain on crawler_link(domain)
----

create table rss_link (
  id INTEGER PRIMARY KEY auto_increment,
  url VARCHAR UNIQUE,
  title VARCHAR,
  add_ts TIMESTAMP default now(),
  last_check_ts TIMESTAMP default DATE'1970-1-1',
  check_interval INTEGER default 60 * 60 * 24, -- in seconds, one day
  last_update_ts TIMESTAMP,
  last_md5 VARCHAR,
  favicon CLOB,
  server VARCHAR,
  crawler_link_id INTEGER  REFERENCES crawler_link
     ON UPDATE CASCADE ON DELETE SET NULL
)
----
create table multi_rss_domain (
  domain varchar UNIQUE,
)
----
create table rss_xml (
  id INTEGER PRIMARY KEY auto_increment,
  add_ts TIMESTAMP default now(),
  last_modified TIMESTAMP,
  etag VARCHAR,
  length INTEGER,
  content CLOB,
  rss_link_id INTEGER REFERENCES rss_link
    ON UPDATE CASCADE ON DELETE SET NULL
)
----
--seeds
insert into crawler_link (url, domain) values
('http://blog.jquery.com/', 'http://blog.jquery.com'),
('http://blogs.oracle.com/', 'http://blogs.oracle.com'),
('http://blog.sina.com.cn/', 'http://blog.sina.com.cn'),
('http://blog.sina.com.cn/kaifulee', 'http://blog.sina.com.cn'),
('http://briancarper.net/', 'http://briancarper.net'),
('http://channel9.msdn.com/', 'http://channel9.msdn.com'),
('http://clj-me.cgrand.net/', 'http://clj-me.cgrand.net'),
('http://clojure-libraries.appspot.com/', 'http://clojure-libraries.appspot.com'),
('http://data-sorcery.org/', 'http://data-sorcery.org'),
('http://ejohn.org/', 'http://ejohn.org'),
('http://emacs-fu.blogspot.com/', 'http://emacs-fu.blogspot.com'),
('http://emacsblog.org/', 'http://emacsblog.org'),
('http://googlewebtoolkit.blogspot.com', 'http://googlewebtoolkit.blogspot.com'),
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
insert into rss_link (url) values
('http://feeds.feedburner.com/ruanyifeng'),
('http://blog.sina.com.cn/rss/kaifulee.xml'),
('http://cemerick.com/feed/'),

----
insert into multi_rss_domain (domain) values
('http://blog.sina.com.cn'),
('http://blogs.oracle.com'),
('http://xianguo.com'),
('http://www.ibm.com')
