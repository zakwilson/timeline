(ns timeline.data
  (:refer-clojure
   :exclude [take drop sort distinct conj! disj! compile case spit extend])
  (:use zutil.util clojureql.core clojureql.predicates timeline.common clojure.contrib.duck-streams clj-time.core)
  (:require [clojure.string :as s]
            [clojure.contrib.sql :as sql])
  (:import org.mindrot.jbcrypt.BCrypt))

(def db
 (:db @config))

(def events (table db :events))
(def tags (table db :tags))
(def events-tags (table db :tags_to_events))