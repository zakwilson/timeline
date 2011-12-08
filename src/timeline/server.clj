(ns timeline.server
  (:use timeline.common
        [swank.swank :only (start-repl)])
  (:require [noir.server :as server])
  (:gen-class))


(server/load-views "src/timeline/views/")

(defn start-swank []
  (start-repl (@config :swank-port)
              :host "127.0.0.1"))

(defn start-web [& [m]]
  (let [mode (or (first m) :dev)]
    (server/start (@config :port)
                  {:mode (keyword mode)
                   :ns 'timeline})))

(defn -main [& m]
  (start-swank)
  (start-web m))