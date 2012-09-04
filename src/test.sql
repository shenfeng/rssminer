-- 2012/6/2

-- users
alter table users add like_score double not null default 1 after conf;
alter table users add neutral_score double not null default 0 after like_score;
alter table users drop scores;


    -- 2012/5/29
    alter table user_feed drop index user_feed_id;
    alter table user_feed drop column vote_sys;
    alter table user_feed drop index user_rsslink;
    alter table user_feed add primary key (user_id, feed_id);

    delete from user_feed where vote_user = 0 and read_date = -1;

    DROP PROCEDURE IF EXISTS `get_user_subs`;

-- 2012/9/3

total 2.2G
drwx------ 2 mysql mysql  340 Sep  4 21:55 .
drwxr-xr-x 6 mysql feng   180 Sep  4 21:55 ..
-rw-rw---- 1 mysql mysql   61 Sep  4 21:55 db.opt
-rw-rw---- 1 mysql mysql 8.5K Sep  4 21:55 favicon.frm
-rw-rw---- 1 mysql mysql  96K Sep  4 21:55 favicon.ibd
-rw-rw---- 1 mysql mysql 8.4K Sep  4 21:55 feed_data.frm
-rw-rw---- 1 mysql mysql 2.0G Sep  4 21:56 feed_data.ibd
-rw-rw---- 1 mysql mysql 8.7K Sep  4 21:55 feeds.frm
-rw-rw---- 1 mysql mysql 192M Sep  4 21:56 feeds.ibd
-rw-rw---- 1 mysql mysql  17K Sep  4 21:55 rss_links.frm
-rw-rw---- 1 mysql mysql  10M Sep  4 21:55 rss_links.ibd
-rw-rw---- 1 mysql mysql 8.6K Sep  4 21:55 user_feed.frm
-rw-rw---- 1 mysql mysql 208K Sep  4 21:55 user_feed.ibd
-rw-rw---- 1 mysql mysql  13K Sep  4 21:55 users.frm
-rw-rw---- 1 mysql mysql 112K Sep  4 21:55 users.ibd
-rw-rw---- 1 mysql mysql 8.6K Sep  4 21:55 user_subscription.frm
-rw-rw---- 1 mysql mysql 736K Sep  4 21:55 user_subscription.ibd

bash -x ./scripts/admin restore-db  22.72s user 0.79s system 36% cpu 1:05.01 total

bash -x scripts/admin restore-db  13.52s user 0.97s system 5% cpu 4:09.65 total

total 1.1G
drwx------ 2 mysql mysql  340 Sep  4 22:06 .
drwxr-xr-x 6 mysql feng   180 Sep  4 22:06 ..
-rw-rw---- 1 mysql mysql   61 Sep  4 22:06 db.opt
-rw-rw---- 1 mysql mysql 8.5K Sep  4 22:06 favicon.frm
-rw-rw---- 1 mysql mysql  96K Sep  4 22:06 favicon.ibd
-rw-rw---- 1 mysql mysql 8.4K Sep  4 22:06 feed_data.frm
-rw-rw---- 1 mysql mysql 824M Sep  4 22:10 feed_data.ibd
-rw-rw---- 1 mysql mysql 8.7K Sep  4 22:06 feeds.frm
-rw-rw---- 1 mysql mysql 192M Sep  4 22:08 feeds.ibd
-rw-rw---- 1 mysql mysql  17K Sep  4 22:06 rss_links.frm
-rw-rw---- 1 mysql mysql  10M Sep  4 22:06 rss_links.ibd
-rw-rw---- 1 mysql mysql 8.6K Sep  4 22:06 user_feed.frm
-rw-rw---- 1 mysql mysql 208K Sep  4 22:06 user_feed.ibd
-rw-rw---- 1 mysql mysql  13K Sep  4 22:06 users.frm
-rw-rw---- 1 mysql mysql 112K Sep  4 22:06 users.ibd
-rw-rw---- 1 mysql mysql 8.6K Sep  4 22:06 user_subscription.frm
-rw-rw---- 1 mysql mysql 736K Sep  4 22:06 user_subscription.ibd


sudo rm /tmp/mysql2 -rf && mkdir /tmp/mysql2

sudo mysql_install_db --user=mysql --basedir=/usr --datadir=/tmp/mysql2c

sudo mysqld --basedir=/usr --datadir=/tmp/mysql2 --user=mysql --server-id=2 --innodb_file_per_table --port=3307 --innodb_file_format=Barracuda

cat /tmp/data.sql| mysql -uroot --port 3307 rssminer

mysql -uroot --port 3307 rssminer

