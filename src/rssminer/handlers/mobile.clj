(ns rssminer.handlers.mobile
  (:use (rssminer [util :only [defhandler to-int now-seconds]]
                  [classify :only [on-feed-event]]))
  (:require [rssminer.config :as cfg]
            [rssminer.db.subscription :as sdb]
            [rssminer.db.feed :as fdb]
            [rssminer.tmpls :as tmpls]
            [clojure.string :as str])
  (:import rssminer.jsoup.HtmlUtils
           java.net.URI))

(defhandler landing-page [req uid]
  (let [subs (map (fn [s]
                    (let [s (bean s)]
                      (when (and (:title s) (> (count (:title s)) 0))
                        (assoc s
                          :like? (> (:like s) 3)
                          :host (when-let [host ^String (:url s)]
                                  (str/reverse (.getHost (URI/create host))))))))
                  (sdb/fetch-user-subs uid))]
    (tmpls/m-subs {:subs (filter identity subs)})))

(defn- readable [feeds]
  (let [now (now-seconds)]
    (map (fn [f]
           (assoc f
             :read?  (> (:readts f) 0)
             :tags (filter #(> (.length ^String %) 0)
                           (take 3 (str/split (:tags f) #";")))
             :pts (let [ts (- now (:publishedts f))]
                    (cond (< ts 60) (str ts " 秒前")
                          (< ts 3600) (str (quot ts 60) " 分前")
                          (< ts 86400) (str (quot ts 3600) " 小时前")
                          :else (str (quot ts 86400) " 天前"))))) feeds)))

(defhandler list-feeds [req uid sid limit offset sortby]
  (let [sub (sdb/fetch-rss-link-by-id sid)
        feeds (map bean (:feeds
                         (case sortby
                           "latest" (fdb/fetch-sub-newest uid (to-int sid) 30 offset)
                           "likest" (fdb/fetch-sub-likest uid (to-int sid) 30 offset))))]
    (tmpls/m-feeds {:feeds (readable feeds)
                    :title (:title sub)
                    :category (case sortby
                                "latest" "最新"
                                "likest" "猜你喜欢")})))

(defhandler show-feed [req fid uid]
  (let [fid (to-int fid)
        feed (-> (fdb/fetch-feeds uid [fid])
                 first bean)]
    (when (= 0 (:readts feed))
      (fdb/mark-as-read uid fid)
      (on-feed-event uid fid))
    (tmpls/m-feed (assoc feed
                    :summary (HtmlUtils/cleanForMobile (:summary feed)
                                                       (:link feed))))))
