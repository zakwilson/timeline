(ns timeline.web.base)

(def use-list
     '[compojure.core
       [zutil util web map]
       ring.adapter.jetty
       [ring.middleware params cookies file stacktrace multipart-params session]
       [ring.util response codec]
       [swank.swank :only (start-server)]
       [hiccup core page-helpers form-helpers]
       sandbar.stateful-session
       clojure.contrib.json])

(def require-list
     '[[timeline.data :as data]
       [clojure.contrib.duck-streams :as ds]
       [clojure.contrib.java-utils :as j]])

(apply use use-list)
(apply require require-list)