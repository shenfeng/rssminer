(ns rssminer.handlers.dashboard
  (:use  [rssminer.middleware :only [*json-body*]])
  (:require [rssminer.db.dashboard :as db]
            [rssminer.config :as conf]))

(defn- get-stats []
  {:total_count (db/get-total-count)
   :crawled_count (db/get-crawled-count)
   :rss_links_cout (db/get-rss-links-count)})

(defn get-rsslinks [req]
  (assoc (get-stats)
    :rss_links (or (db/get-rss-links) [])))

(defn get-crawler-pending [req]
  (assoc (get-stats)
    :pending_links (or (db/get-pending-links) [])))

(defn get-crawled [req]
  (assoc (get-stats)
    :crawled_links (or (db/get-crawled-links) [])))

(defn get-black-domain-pattens [req]
  (assoc (get-stats)
    :black_domain_pattens (map str @conf/black-domain-pattens)))

(defn add-black-domain-patten [req]
  (let [patten (:patten *json-body*)]
    (when (> (count patten) 2)
      (conf/add-black-domain-patten patten)
      (map str @conf/black-domain-pattens))))
