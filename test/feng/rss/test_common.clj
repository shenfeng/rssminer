(ns feng.rss.test-common
  (:require [feng.rss.config])
  (:use [feng.rss.routes :only [app]]))

(def test-app
  (app))

