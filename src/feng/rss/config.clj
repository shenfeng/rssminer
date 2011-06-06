(ns feng.rss.config)

(def env-profile (atom :production))

(defn in-prod? []
  (= @env-profile :production))

(defn in-dev? []
  (= @env-profile :development))

(def DB_HOST
  (get (System/getenv) "READER_DB_HOST" "127.0.0.1"))
(def PSQL_USERNAME "postgres")
(def PSQL_PASSWORD "123456")

;;; public/imgs/32px-feed-icon.png 14x14
(def default-icon "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAOCAYAAAAfSC3RAAAABmJLR0QA/wD/AP+gvaeTAAAACXBIWXMAAABIAAAASABGyWs+AAAACXZwQWcAAAAOAAAADgCxW/H3AAACXklEQVQoz02QPWiddRjFf8/z/N/33tvbq71JhfQjik2M34MhLQhaKVZBF3EIjsHJdnJycGm1LuLoIHRwEFwcFNRF1IBQsAYpQkuCVFpoytUSbEzpvfS++X88DongcDjTOZzzk8tvPXSo1++fC6JviNAVE9QEUUEMRAXddXdG94bxi9uDv8+EXr9/rsaWRCuz2lAfo5rQSrHK0Ao07JaZdDt7O0vu+wlBbJEYbe+JN+k8/SJ5c508+I2yfhEZDf4LICYAKNnanWoxiNKzSrB2G5ucJkzNwZMvU+4MyKtfUn7/BslDQCjRSeMChZ78eWbBKzJ2X5+wb5Kw/0GquRPYkeNI3aVcXyavfEze2iA1TmqcZltQUdBKaD32Aq35RaRqkVY+If7wLn77KjrzEnrsbRJd0jiTxoWSHBUTNAjhwKPUzyzSfuUD6lc/QgTST+9TNtaw2ZPY46+Txk5uCiU79s7Jw+/VNTC8iW9cQRT0wDw2/Sz5xq/EaxcJM8fRB2YZr10g/bOJq6KqoJUiwwE++IV04UPypfPQuh87dprtWzdoVn/E9h2kmn2O1OT/TTXHjjxP/dp5wtFTxCtfk/5YxqaeQA8tcO/yMnihfniB4jVefBeOCTb1FDIxAwePkqLSrH4PODY9T7y1Th5uYpOHIeyhZCe4c1eC9sq173BtEwdrOz/GV5GVr4h/XSdubTH8+VvcoaSC1HJXbp6d/3RiqrOkniyNyw7yxsmNk7edvF12FMEL2J4qe9c/s6VHqktoPVGyzcXodYxCKkJBKQiuigdFWoZ2wsjb/vlodOfsv+CuDYS3FBkqAAAAJXRFWHRkYXRlOmNyZWF0ZQAyMDExLTA2LTAzVDA5OjMwOjE5KzA4OjAwW7SFpQAAACV0RVh0ZGF0ZTptb2RpZnkAMjAwOS0wNy0xOFQwMzo0MzoxMiswODowMPgSDTcAAAAASUVORK5CYII=")
