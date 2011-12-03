(ns timeline.server
  (:require [noir.server :as server]))


(server/load-views "src/timeline/views/")

(defn -main [& m]
  (let [mode (or (first m) :dev)]
    (server/start 8000 {:mode (keyword mode)
                        :ns 'timeline})))