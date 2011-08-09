(ns rssminer.time
  (:import java.util.Date
           java.sql.Timestamp))

(definline ^Date now
  "return current time as of java.util.Date"
  []
  `(Date.))

(definline to-ts [^Date date]
  `(Timestamp. (.getTime ~date)))

(definline now-seconds []
  `(quot (System/currentTimeMillis) 1000))
