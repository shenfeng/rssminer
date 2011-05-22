(ns feng.rss.test-common
  (:require [feng.rss.config])
  (:use [feng.rss.routes :only [app]]
        [sandbar.stateful-session :only [session-get]]))

(def test-user {:name "test-user"
                :password "123456"
                :email "test@user.com"})

(def test-rss-str
  (slurp (-> (clojure.lang.RT/baseLoader)
             (.getResourceAsStream "test-rss.xml"))))

(defn mock-http-get [& args]
  {:body test-rss-str})

(def test-app
  (app))

(def auth-app
  (let [mock-session-get (fn [arg]
                           (if (=  arg :user)
                             (assoc test-user
                               :id 1)
                             arg))]
    (fn [& args]
      (binding [session-get mock-session-get]
        (apply (app) args)))))
