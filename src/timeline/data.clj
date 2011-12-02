(ns timeline.data
  (:use [zutil util map] timeline.common [clj-time.core :exclude [extend]] [korma db core])
  (:require [clojure.string :as s])
  (:import org.mindrot.jbcrypt.BCrypt))

(load-config)

(defdb db
  (postgres (:db @config)))

(defn sql->date [d]
  (date-time (+ 1900 (.getYear d))
             (+ 1 (.getMonth d))
             (.getDate d)))

(defn date-difference [d1 d2]
  (Math/floor (/ (in-minutes (interval d1 d2))
                 1440))) ; minutes per day

(defn interval-length [event]
  (date-difference (:startdate event)
                   (:enddate event)))

(defn date [date-str]
  (let [[y m d] (map #(Integer/parseInt %)
                     (s/split date-str #"-"))]
    (java.sql.Date. (- y 1900) (- m 1) d)))

(defn date->sql [a-date]
  (java.sql.Date. (- (year a-date) 1900)
                  (- (month a-date) 1)
                  (day a-date)))

(defentity tags
  (pk :tag))

(declare events)

(defentity event-tag
  (table :tags_to_events)
  (has-one tags)
  (has-one events))

(defentity events
  (transform
   #(map-keys sql->date
              % 
              [:startdate :enddate]))
  (prepare
   #(map-keys date->sql
              %
              [:startdate :enddate]))
  (has-many tags))

(defn add-event! [event]
  "Event keys: [:startdate :enddate :title :description :link :importance]"
  (insert events
          (values event)))


(defn add-tag! [tagname]
  (conj! tags {:tag tagname}))

; PERF - there are a lot faster ways to do this

(defn assign-tag-to-event [event tagname]
  (let [existing-tag
        @(select events-tags (where (and (= :event (:id event))
                                         (= :tag tagname))))]
    (when-not (seq existing-tag)
      (conj! events-tags {:event (:id event) :tag tagname}))))


(defn tag-event [event tag-string]
  (let [tags (s/split #"\ " tag-string)]
    (map #(assign-tag-to-event event %)
         tags)))