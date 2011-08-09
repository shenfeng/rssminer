SELECT
     s.link,  author , LENGTH(f.alternate) as link_length, LENGTH(summary) as summary_length
FROM
     feeds AS f
     JOIN subscriptions AS s
     ON s.id = f.subscription_id;

SELECT title, LENGTH(favicon), added_ts, user_id FROM subscriptions;

SELECT
   us.group_name, s.id, s.title, s.favicon,
   (SELECT COUNT(*) FROM feeds WHERE feeds.subscription_id = s.id) AS total_count,
   (SELECT COUNT(*) FROM feeds
    WHERE  feeds.subscription_id = s.id AND
           feeds.id NOT IN (SELECT feed_id FROM feedcategory
                             WHERE user_id = 1 AND
                                  'type' = 'rssminer' AND
                                   text = 'read' )) AS unread_count
FROM
   user_subscription AS us
   JOIN subscriptions AS s ON s.id = us.subscription_id
WHERE us.user_id = 1


SELECT type, text, added_ts FROM feedcategory WHERE user_id = 1 AND feed_id = 1

SELECT id, content, added_ts FROM comments WHERE user_id = 1 AND feed_id =1

SELECT id, author, title, summary, alternate, published_ts FROM feeds WHERE subscription_id = 1
