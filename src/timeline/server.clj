(ns timeline.server
  (:use timeline.common
        [swank.swank :only (start-repl)])
  (:require [noir.server :as server])
  (:gen-class))


(server/load-views "src/timeline/views/")

(defn -main [& m]
  (let [mode (or (first m) :dev)]
    (start-repl (@config :swank-port)
                :host "127.0.0.1")
    (server/start (@config :port)
                  {:mode (keyword mode)
                   :ns 'timeline})))