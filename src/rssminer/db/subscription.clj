(ns rssminer.db.subscription
  (:use [rssminer.db.util :only [select-sql-params mysql-insert-and-return
                                 mysql-query with-mysql]]
        [clojure.java.jdbc :only [delete-rows update-values do-commands]]))

(defn fetch-rss-link [map]
  (first
   (mysql-query (select-sql-params :rss_links map))))

(defn fetch-feeds-count-by-id [rss-id]
  (-> (mysql-query ["SELECT COUNT(*) as count
                FROM feeds WHERE rss_link_id = ?" rss-id])
      first :count))

(defn fetch-user-subs [user-id like neutral]
  (map (fn [{:keys [o_title title] :as s}]
         (dissoc (assoc s :title (or title o_title))
                 :o_title))
       (mysql-query ["call get_user_subs(?, ?, ?)" user-id like neutral])))

(defn fetch-user-subsurls [user-id]     ; only url
  (mysql-query ["SELECT url FROM rss_links r JOIN user_subscription s
                 ON r.id = s.rss_link_id WHERE s.user_id = ?" user-id]))

(defn fetch-user-subids [user-id]
  (map :id (mysql-query ["SELECT rss_link_id AS id FROM user_subscription
                 WHERE user_id = ?" user-id])))

(defn fetch-user-sub [id]
  (first (mysql-query ["SELECT id, url, title, alternate
              FROM rss_links WHERE id = ?" id])))

(defn fetch-subscription [user-id rss-link-id]
  (first (mysql-query ["SELECT id, rss_link_id, title, group_name FROM
                       user_subscription
                       WHERE user_id = ? AND rss_link_id = ?"
                       user-id rss-link-id])))

(defn delete-subscription [user-id rss-id]
  (with-mysql
    (delete-rows :user_subscription
                 ["user_id = ? AND rss_link_id = ?" user-id rss-id])
    (delete-rows :user_feed
                 ["user_id = ? AND rss_link_id = ?" user-id rss-id])))

(defn update-sort-order [user-id data]
  (with-mysql
    (apply do-commands
           (map (fn [d idx] (str "UPDATE user_subscription SET sort_index = "
                                idx
                                " , group_name = '" (:g d) "'"
                                " WHERE user_id = " user-id
                                " AND rss_link_id = " (:id d)))
                data (range 400 200000 4)))))
