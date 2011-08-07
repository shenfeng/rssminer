(ns freader.db.user
  (:use [freader.db.util :only [h2-query select-sql-params
                                h2-insert-and-return]]
        [freader.util :only [md5-sum]]))

(defn create-user [user]
  (h2-insert-and-return :users
                        (update-in user [:password]
                                   #(md5-sum %))))

(defn find-user [attr]
  (first (h2-query (select-sql-params :users attr))))

(defn authenticate [email plain-password]
  (if-let [user (find-user {:email email})]
    (when (= (md5-sum plain-password) (:password user))
      user)))

