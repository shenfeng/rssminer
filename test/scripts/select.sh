#! /bin/bash

for i in {1..100}; do
    mysql rssminer -e "SELECT f.id,f.rss_link_id,left(f.title, 10),f.author,left(f.link, 30),tags,f.published_ts,uf.read_date,uf.vote_user, uf.vote_date FROM feeds f LEFT JOIN user_feed uf ON uf.feed_id = f.id and uf.user_id =1 JOIN user_subscription us ON f.rss_link_id = us.rss_link_id where us.user_id = $i order by f.published_ts desc limit 0,20" > /dev/null
done
