(ns project.core
  (:require [clj-http.client :as client]
            [clojure.tools.logging :as log]
            [com.climate.claypoole :as cp]))

(defn foo []
  (println "Hello World"))

; Immutability
(defn some-scary-black-box-fn
  [my-object])
(comment
  (let [my-object {:name "value"}]
    (some-scary-black-box-fn my-object)
    my-object))

; Threads
(defn run-java-thread
  []
  (log/info "Before thread")
  (.start (new Thread (fn []
                        (Thread/sleep 1000)
                        (log/info "from thread"))))
  (log/info "After thread"))
  
; Futures
(defn run-clojure-future
  []
  (log/info "Before future")
  (future
    (Thread/sleep 1000)
    (log/info "from future"))
  (let [f
        (future
          (Thread/sleep 1000)
          (log/info "From future body")
          (log/info "from future")
          {:result "from future"})]
    (log/info "After future")
    (log/info "Result of future is " @f #_(deref f)))
  )

; Parallelism
(defn parallel-requests
  []
  (let [urls (->> (client/get "https://pokeapi.co/api/v2/pokemon-species?limit=100"
                              {:as :json})
                  :body
                  :results
                  (map :url))]
    (->> urls
         (map (fn [url]
                (-> (client/get url {:as :json})
                    :body
                    (select-keys [:name :shape]))
                
                ))
         (doall))))