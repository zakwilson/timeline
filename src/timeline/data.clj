(ns timeline.data
  (:use [zutil util map] timeline.common [clj-time.core :exclude [extend]] [korma db core])
  (:require [clojure.string :as s])
  (:import org.mindrot.jbcrypt.BCrypt))

(load-config)

(defdb db
  (postgres (:db @config)))

(defn sql->date [d]
  (when d
    (date-time (+ 1900 (.getYear d))
               (+ 1 (.getMonth d))
               (.getDate d))))

(defn date->sql [a-date]
  (when a-date
    (java.sql.Date. (- (year a-date) 1900)
                    (- (month a-date) 1)
                    (day a-date))))

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

(defn transform-dates [evt & keys]
  (map-keys sql->date
            evt
            keys))

(defn prepare-dates [evt & keys]
  (map-keys sql->date
            evt
            keys))

(defn transform-tags [evt]
  (assoc evt
    :tag
    (map :tag (:tag evt))))

(defn prepare-tags [evt]
  (assoc evt
    :tag
    (map #({:tag %
            :event-id (:id evt)})
         (:tag evt))))

(declare event)

(defentity tag
  (table :tag)
  (belongs-to event))

(defentity event
  (transform
   #(-> %
        (transform-dates :startdate :enddate)
        transform-tags))
  (prepare
   #(-> %
        (prepare-dates :startdate :enddate)))
  (has-many tag))

(declare assign-tag-to-event!)

(defn add-event! [e]
  "Event keys: [:startdate :enddate :title :description :link :importance]"
  (let [tags (:tag e)
        e (dissoc e :tag)]
    (insert event
            (values e))
    (when tags
      (map (partial assign-tag-to-event! e)
           tags))))

(defn get-event [id]
  (-> (select* event)
      (where {:id id})
      (with tag)
      exec
      first))

; PERF - there are a lot faster ways to do this

(defn assign-tag-to-event! [evt tagname]
  (let [existing-tag
        (-> (select* tag)
            (where {:event_id (:id evt)
                    :tag tagname})
            exec)]
    (when-not (seq existing-tag)
      (insert tag
              (values {:event_id (:id evt) :tag tagname})))))


(defn tag-event! [event tag-string]
  (let [tags (s/split #"\ " tag-string)]
    (map #(assign-tag-to-event! event %)
         tags)))