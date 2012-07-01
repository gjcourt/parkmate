(ns packmate.database
  "In memory user database to support other examples.")

(defonce db (atom {:id 1
                   :things {}
                   :roles #{:admin :user}
                   :regions #{{:id 1 :value "North America"}
                              {:id 2 :value "South America"}
                              {:id 3 :value "Europe"}
                              {:id 4 :value "Australia"}}
                   :languages #{{:id 1 :name "Clojure"}
                                {:id 2 :name "Ruby"}
                                {:id 3 :name "Java"}
                                {:id 4 :name "C"}
                                {:id 5 :name "Go"}
                                {:id 6 :name "Erlang"}
                                {:id 7 :name "C++"}
                                {:id 8 :name "C#"}
                                {:id 9 :name "Haskell"}
                                {:id 10 :name "Python"}
                                {:id 11 :name "Groovy"}}}))

(defn all-things []
  (map val (:things @db)))

(defn all-roles []
  (:roles @db))

(defn all-regions []
  (:regions @db))

(defn all-langs []
  (:languages @db))

(defn store [thing]
  (swap! db
         (fn [old u]
           (let [id (Integer/valueOf (if (:id u) (:id u) (inc (:id old))))]
             (if (:id u)
               (assoc-in old [:things id] u)
               (-> old
                   (assoc :id id)
                   (assoc-in [:things id] (assoc u :id id))))))
         thing))

(defn fetch [id]
  (get (:things @db) (Integer/valueOf id)))
