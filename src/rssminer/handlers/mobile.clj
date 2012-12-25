(ns rssminer.handlers.mobile
  (:use [compojure.core :only [defroutes GET POST DELETE ANY context]]
        (rssminer [util :only [defhandler to-int now-seconds]]
                  [classify :only [on-feed-event]]))
  (:require [rssminer.config :as cfg]
            [rssminer.db.subscription :as sdb]
            [rssminer.db.feed :as fdb]
            [rssminer.tmpls :as tmpls]
            [clojure.string :as str])
  (:import rssminer.jsoup.HtmlUtils
           java.net.URI))

(defn- user-subs [uid]
  (filter identity (map (fn [s]
                          (when (and (:title s)
                                     (> (:total s) 0)
                                     (> (count (:title s)) 0))
                            (assoc s
                              :host (when-let [url ^String (:url s)]
                                      (when-let [host (.getHost (URI/create url))]
                                        (str/reverse host))))))
                        (sdb/fetch-subs uid))))

(defhandler landing-page [req uid]
  (tmpls/m-subs {:subs (user-subs uid)}))

(defhandler show-folder [req folder uid]
  (tmpls/m-folder {:folder folder
                   :subs (filter #(= folder (:group %1)) (user-subs uid))}))

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
                           "latest" (fdb/fetch-sub-newest uid (to-int sid) 35 offset)
                           "likest" (fdb/fetch-sub-likest uid (to-int sid) 35 offset))))]
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
