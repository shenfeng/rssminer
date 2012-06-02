(ns rssminer.db.user
  (:use [rssminer.database :only [mysql-query select-sql-params
                                 mysql-insert-and-return with-mysql]]
        [rssminer.util :only [md5-sum]]
        [clojure.java.jdbc :only [update-values]]
        [clojure.data.json :only [read-json]]))

(defn create-user [{:keys [email password] :as user}]
  (mysql-insert-and-return :users
                           (assoc user :password
                                  (when password
                                    (md5-sum (str email "+" password))))))

(defn find-user [attr]
  (when-let [user (first (mysql-query (select-sql-params :users attr)))]
    (assoc user :conf
           (when-let [conf (:conf user)] (read-json conf)))))

(defn find-user-by-id [id]
  (first
   (mysql-query ["SELECT name, email, conf, like_score, neutral_score
                  FROM users WHERE id = ?" id])))

(defn authenticate [email plain-password]
  (if-let [user (find-user {:email email})]
    (when (= (md5-sum (str email "+" plain-password)) (:password user))
      user)))

(defn update-user [id data]
  (with-mysql (update-values :users ["id = ?" id] data)))
