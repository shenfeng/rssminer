SELECT 
     s.link,  author , LENGTH(f.alternate) as link_length, LENGTH(summary) as summary_length
FROM 
     feeds AS f
     JOIN subscriptions AS s
     ON s.id = f.subscription_id;

SELECT title, LENGTH(favicon), added_ts, user_id FROM subscriptions;
