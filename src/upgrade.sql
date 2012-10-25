drop procedure get_unvoted;


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


drop procedure get_voted;
delimiter //

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

alter table feeds modify author varchar(64) not null default '';
alter table feeds modify link varchar(220) not null;
alter table feeds modify title varchar(256) not null default '';
alter table feeds modify tags varchar(128) not null default '';
alter table feeds modify updated_ts int UNSIGNED not null default 0;
alter table feeds modify published_ts int UNSIGNED not null default 0;
alter table feeds modify rss_link_id int UNSIGNED not null;

alter table user_feed modify vote_user tinyint not null default 0;
alter table user_feed modify read_date int not null default -1;
alter table user_feed modify vote_date int not null default -1;

SELECT f.id, f.rss_link_id, f.published_ts
FROM feeds f
JOIN user_subscription us ON f.rss_link_id = us.rss_link_id AND us.user_id = 1
where f.id not in (select feed_id from user_feed where user_id = 1 )
LIMIT 5000 PROCEDURE ANALYSE(10000, 200000);;



alter table feeds drop index rss_link_id_link; -- 51.75s
alter table feeds add index rss_link_id_link(rss_link_id, link);         --
ALTER TABLE feeds MODIFY link VARCHAR(512) NOT NULL;


alter table rss_links drop index url;
alter table rss_links modify url varchar(512) not null;
alter table rss_links add index url (url);


--
update rss_links set url = 'http://feeds.feedburner.com/disclojure' where id = 28;
update user_subscription set rss_link_id = 145 where rss_link_id = 415;
delete from rss_links where id = 415;
update rss_links set url = 'http://jimmyg.org/blog/feed.rss' where id = 589;
update rss_links set url = 'http://robbin.iteye.com/rss' where id = 714;
update rss_links set url = 'http://feeds.feedburner.com/UbuntuGeek' where id = 793;
update rss_links set url = 'http://cookoo.iteye.com/rss' where id = 919;



update user_feed uf set uf.rss_link_id = (select rss_link_id from feeds f where uf.feed_id = f.id)


-- (3 min 16.06 sec)
insert into feed_data(id, summary) select id, summary from feeds;

-- (45.38 sec)
alter table feeds drop column summary;

alter table feed_data add column jsoup MEDIUMTEXT;
alter table feed_data add column compact MEDIUMTEXT;
alter table feed_data add column tagsoup MEDIUMTEXT;
alter table user_feed add column read_time MEDIUMINT UNSIGNED NOT NULL DEFAULT 0 after read_date


alter table feeds add column link_hash int not null default 0;
alter table feeds drop index rss_link_id_link;
alter table feeds add index rss_id_link_hash (rss_link_id, link_hash);



explain SELECT f.author,f.link,tags,f.published_ts,uf.read_date,uf.vote_user FROM feeds f LEFT JOIN user_feed uf ON uf.feed_id = f.id and uf.user_id =1 JOIN user_subscription us ON f.rss_link_id = us.rss_link_id where us.user_id = 1 order by f.published_ts desc limit 0,20;

explain select count(f.id) from feeds f join user_subscription us on us.id = f.rss_link_id and us.user_id = 1 order by f.published_ts desc;

alter table feeds add index rss_id_published_ts (rss_link_id, published_ts)

alter table user_feed modify vote_user tinyint not null default 0;

alter table rss_links modify next_check_ts int unsigned not null default 1;
alter table rss_links modify last_status SMALLINT unsigned not null default 0;


-- 2012/10/7
update user_subscription set rss_link_id = 74 where rss_link_id = 2803;
delete from rss_links where id = 2803;
delete from feed_data where id in (select id from feeds where rss_link_id = 2803);
delete from feeds where rss_link_id = 2803;


-- 2012/10/25
alter table feeds drop index rss_id_link_hash;
alter table feeds add column simhash bigint not null default -1;
alter table feeds drop column link_hash;
alter table feeds add index rss_id_idx(rss_link_id);
alter table feeds add index simhash_idx(simhash);
