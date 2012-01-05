(ns rssminer.db.user
  (:use [rssminer.db.util :only [h2-query select-sql-params
                                 h2-insert-and-return with-h2]]
        [rssminer.util :only [md5-sum]]
        [clojure.java.jdbc :only [update-values]]))

(defn create-user [{:keys [email password] :as user}]
  (h2-insert-and-return :users
                        (assoc user :password
                               (md5-sum (str email "+" password)))))

(defn find-user [attr]
  (first (h2-query (select-sql-params :users attr))))

(defn authenticate [email plain-password]
  (if-let [user (find-user {:email email})]
    (when (= (md5-sum (str email "+" plain-password)) (:password user))
      user)))

(defn update-user [id data]
  (with-h2 (update-values :users ["id = ?" id] data)))

(defn fetch-conf [user-id]
  (-> (h2-query ["SELECT conf FROM users WHERE id = ?"
                 user-id])
      first :conf ))
