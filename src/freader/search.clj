(ns freader.search
  (:import [org.apache.lucene.document Document Field Field$Index
            Field$Store Field$TermVector NumericField]
           [org.apache.lucene.index IndexWriter IndexWriterConfig
            IndexWriterConfig$OpenMode IndexReader]
           [org.apache.lucene.search IndexSearcher Query Sort]
           [org.apache.lucene.store MMapDirectory RAMDirectory]
           [org.apache.lucene.queryParser MultiFieldQueryParser QueryParser]
           org.apache.lucene.analysis.standard.StandardAnalyzer
           org.apache.lucene.util.Version
           java.io.File))

(def ^{:private true} version Version/LUCENE_32)

(defonce indexer (atom nil))

(def ^{:private true} m-store {:yes Field$Store/YES
                               :no Field$Store/NO})

(def ^{:private true} m-index {:analyzed Field$Index/ANALYZED
                               :not Field$Index/NOT_ANALYZED})

(def ^{:private true} m-termvector {:yes Field$TermVector/YES
                                    :no Field$TermVector/NO})

(defn- create-analyzer [] (StandardAnalyzer. version))

;;; TODO better query
(defn- create-query [term & fields]
  (let [fields (into-array (map name fields))
        parser (MultiFieldQueryParser. version fields (create-analyzer))]
    (.parse parser term)))

(defn close-global-index-writer! []
  (when-not (nil? @indexer)
    (.close @indexer)
    (reset! indexer nil)))

(defn use-index-writer! [^String path]
  "It will close previous indexer"
  (close-global-index-writer!)
  (let [conf (doto (IndexWriterConfig. version (create-analyzer))
               (.setOpenMode IndexWriterConfig$OpenMode/CREATE_OR_APPEND))
        dir (if (= path :RAM)
              (RAMDirectory.)
              (MMapDirectory. (File. path)))]
    (reset! indexer (IndexWriter. dir conf))))

(defn- ^NumericField create-numericfield
  [field-name value & [{:keys [store] :or {store :yes}}]]
  (doto (NumericField. (name field-name) (store m-store) true)
    (.setIntValue value)))

(defn- ^Field create-field
  [field-name value & {:keys [store index termvector]
                       :or {store :no index :analyzed termvector :no}}]
  (Field. (name field-name) value
          (store m-store)
          (index m-index)
          (termvector m-termvector)))

(defn- create-document [{:keys [id rss_link_id author title summary]}]
  (let [^Document document (Document.)
        author (or author "")
        title (or title "")
        summary (or summary "")]
    (doto document
      (.add (create-numericfield :feed_id id))
      (.add (create-numericfield :rss_link_id rss_link_id))
      (.add (create-field :author author))
      (.add (create-field :title title :store :yes))
      (.add (create-field :summary summary)))))

(defn index-feeds [feeds]
  (let [^IndexWriter indexer @indexer]
    (doseq [feed feeds]
      (.addDocument indexer (create-document feed)))
    (.commit indexer)))

(defn- get-doc [^IndexSearcher searcher doc-id]
  (let [doc (.doc searcher doc-id)
        fget (fn [field]
               (.get doc (name field)))]
    {:id (fget :feed_id)
     :rss_link_id (fget :rss_link_id)
     :title (fget :title)}))

(defn- search* [term n]
  (let [searcher (IndexSearcher. (IndexReader/open @indexer true))
        query (create-query term :title :summary)
        hits (.search searcher query n)]
    {:total-hits (.totalHits hits)
     :docs (map (fn [score-doc]
                  (get-doc searcher (.doc score-doc)))
                (.scoreDocs hits))}))

(defn search [req]
  (let [term (-> req :params :term)
        result (search* term 10)]
    (map :title (:docs result))))
