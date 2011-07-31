(ns ^{:doc ""
      :author "feng"}
  freader.crawler
  (:use [freader.util :only [extract-links]])
  (:require [freader.db.crawler :as db]
            [freader.http :as http]))

(defn download-page [{:keys [link] :as crawler-link}]

  )

