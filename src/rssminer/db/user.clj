(ns rssminer.db.user
  (:use [rssminer.database :only [mysql-query with-mysql
                                  mysql-insert-and-return]]
        [rssminer.util :only [md5-sum]]
        [clojure.java.jdbc :only [update-values]]))

(defn create-user [{:keys [email password] :as user}]
  (mysql-insert-and-return :users
                           (assoc user :password
                                  (when password
                                    (md5-sum (str email "+" password))))))

(defn find-by-email [email]
  (first
   (mysql-query ["SELECT id, password, email, conf, like_score, neutral_score
                  FROM users WHERE email = ?" email])))

(defn find-by-id [id]
  (first
   (mysql-query ["SELECT email, conf, like_score, neutral_score
                  FROM users WHERE id = ?" id])))

(defn authenticate [email plain-password]
  (if-let [user (find-by-email email)]
    (when (= (md5-sum (str email "+" plain-password)) (:password user))
      user)))

(defn update-user [id data]
  (with-mysql (update-values :users ["id = ?" id] data)))
