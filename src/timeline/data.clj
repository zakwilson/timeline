(ns timeline.data
  (:use [zutil util map]
        timeline.common
        [clj-time.core :exclude [extend]]
        clj-time.coerce
        [korma db core])
  (:require [clojure.string :as s])
  (:import org.mindrot.jbcrypt.BCrypt))

(load-config)

(defdb db
  (postgres (:db @config)))

(defn sql->date [d]
  (when d
    (from-long (.getTime d))))

(defn date->sql [a-date]
  (when a-date
    (java.sql.Date. (.getMillis a-date))))

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

(defn prepare-integers [evt & keys]
  (map-keys integer evt keys))

(defn transform-dates [evt & keys]
  (map-keys sql->date
            evt
            keys))

(defn prepare-dates [evt & keys]
  (map-keys date->sql
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

(defentity uploads
  (belongs-to event))

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
        (prepare-dates :startdate :enddate)
        (prepare-integers :importance :id)))
  (has-many tag)
  (has-many uploads))

(declare assign-tag-to-event!)

(defn add-event! [e]
  "Event keys: [:startdate :enddate :title :description :link :importance]"
  (let [tags (:tag e)
        e (dissoc e :tag)
        new-event (insert event
                          (values e))]
    (when tags
      (map (partial assign-tag-to-event! e)
           tags))
    new-event))

(defn update-event! [e]
  (update event
          (set-fields e)
          (where {:id (:id e)})))

(defn get-event [id]
  (-> (select* event)
      (where {:id id})
      (with tag)
      exec
      first))

(defn get-all-events []
  (-> (select* event)
      (with tag)
      exec))

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
  (let [tags (map #(.toLowerCase %)
                  (s/split tag-string #","))]
    (map #(assign-tag-to-event! event %)
         tags)))

(defn add-upload! [evt filename]
  (insert uploads
          (values {:event_id (:id evt)
                   :filename filename})))