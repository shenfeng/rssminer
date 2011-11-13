(ns rssminer.time
  (:import java.util.Date
           java.util.GregorianCalendar
           java.sql.Timestamp))

(definline ^Date now
  "return current time as of java.util.Date"
  []
  `(Date.))

(definline now-seconds []
  `(quot (System/currentTimeMillis) 1000))

(defn time-pairs []
  (let [gc (GregorianCalendar.)
        last-day (do (.add gc GregorianCalendar/DAY_OF_YEAR -1)
                     (quot (.getTimeInMillis gc) 1000))
        last-week (do (.add gc GregorianCalendar/DAY_OF_YEAR -6)
                      (quot (.getTimeInMillis gc) 1000))
        last-month (do (.add gc GregorianCalendar/DAY_OF_YEAR -23)
                       (quot (.getTimeInMillis gc) 1000))]
    (list last-day last-week last-month)))
