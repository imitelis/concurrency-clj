(ns project.core
  (:require [clojure.tools.logging :as log]
            [clj-http.client :as client]
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
(defn non-parallel-requests
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
                #_(future
                )))
         (doall))))

(def r1 (time non-parallel-requests))

(defn parallel-requests
  []
  (let [urls (->> (client/get "https://pokeapi.co/api/v2/pokemon-species?limit=100"
                              {:as :json})
                  :body
                  :results
                  (map :url))]
    (->> urls
         (map (fn [url]
                (future
                  (-> (client/get url {:as :json})
                       :body
                       (select-keys [:name :shape])))))
         (map deref)
         (doall))))

(def r2 (time parallel-requests))

; Race conditions
(def counter 0)

(defn inc-counter-test
  []
  (alter-var-root #'counter (constantly 0))
  (let [f1 (future
             (dotimes [_ 1000]
               ;(Thread/sleep 1)
               (alter-var-root #'counter (constantly (inc counter))))
             (log/info "f1 ends")
             ::f1             
             )
        f2 (future
             (dotimes [_ 1000]
               ;(Thread/sleep 2)
               (alter-var-root #'counter (constantly (inc counter))))
             (log/info "f2 ends")
             ::f2)]
    (and (deref f1) (deref f2))
    (log/info "Here!" @f1 @f2)
    counter
    )
  )

(def counter-atom (atom 0))

(defn inc-counter-atom-test
  []
  (reset! counter-atom 0)
  (let [f1 (future
             (do ; Wrap multiple expressions
               (dotimes [_ 1000]
                 (swap! counter-atom inc))
               ::f1))
        f2 (future
             (do
               (dotimes [_ 1000]
                 (swap! counter-atom inc))
               ::f2))]
    (log/info "Here!" @f1 @f2)
    (deref counter-atom)))

(def state-atom (atom []))

(swap! state-atom (fn [old-value arg1 arg2]
                    (println old-value arg1 arg2)
                    (conj old-value 1)
                    ) 10 20)

; Pmap
(comment (pmap (fn [input]) [ 1 2 3]))

; Thread pools
(defn calypoole-parallel-requests
  []
  (let [urls (->> (client/get "https://pokeapi.co/api/v2/pokemon-species?limit=100"
                              {:as :json})
                  :body
                  :results
                  (map :url))]
    (->> urls
         (cp/map
          (cp/threadpool 100)
          (fn [url]
            (-> (client/get url {:as :json})
                :body
                (select-keys [:name :shape])))) 
         (doall))))