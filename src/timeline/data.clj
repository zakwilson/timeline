(ns timeline.data
  (:use [zutil util map]
        timeline.common
        [clj-time.core :exclude [extend]]
        clj-time.coerce
        [korma db core]
        clojure.java.io)
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

(defentity user
  (table :users)
  (has-many event)
  (prepare
   #(-> %
        (assoc :hash
          (if-not (empty? (:password %))
            (BCrypt/hashpw (:password %)
                           (BCrypt/gensalt 10))
            (:hash %)))
        (dissoc :password :confirm))))

(defentity event
  (transform
   #(-> %
        (transform-dates :startdate :enddate)
        transform-tags))
  (prepare
   #(-> %
        (prepare-dates :startdate :enddate)
        (prepare-integers :importance)))
  (has-many tag)
  (has-many uploads)
  (belongs-to user))

(declare assign-tag-to-event!
         tag-event!)

(defn add-event! [e]
  "Event keys: [:startdate :enddate :title :description :link :importance :username]"
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
      (with uploads)
      exec
      first))

(defn get-all-events []
  (-> (select* event)
      (with tag)
      (with uploads)
      exec))

; PERF - there are a lot faster ways to do this

(defn assign-tag-to-event! [evt tagname]
  (spit "tag" (str evt "\n\n" tagname "\n"))
  (let [existing-tag
        (-> (select* tag)
            (where {:event_id (:id evt)
                    :tag tagname})
            exec)]
    (when-not (seq existing-tag)
      (insert tag
              (values {:event_id (:id evt) :tag tagname})))))

(defn string->tags [tag-string]
  (map #(.toLowerCase (s/trim %))
       (s/split tag-string #",")))

(defn tag-event! [evt tag-string]
  (let [tags (string->tags tag-string)]
    (dorun
     (map #(assign-tag-to-event! evt %)
          tags))))

(defn add-upload! [evt filename]
  (insert uploads
          (values {:event_id (:id evt)
                   :filename filename})))

(defn event-upload-dir [evt]
  (str "resources/public/uploads/"
       (:id evt)
       "/"))

(defn handle-file [evt req]
  (let [file-req (get-in req [:params :file])
        filename (:filename file-req)
        tmpfile (:tempfile file-req)
        new-file-dir (event-upload-dir evt)
        new-file-path (str new-file-dir filename)]
    (add-upload! evt filename)
    (ensure-directory-exists (file new-file-dir))
    (copy tmpfile (file new-file-path))))

(defn delete-dir [d]
  (doseq [f (reverse (file-seq d))]
    (delete-file f true)))

(defn delete-uploads! [evt]
  (delete-dir (file (event-upload-dir evt))))

(defn delete-event! [evt]
  (delete tag
          (where {:event_id (:id evt)}))
  (delete-uploads! evt)
  (delete uploads
          (where {:event_id (:id evt)}))
  (delete event
          (where {:id (:id evt)})))

(defn user-exists? [username]
  (not (empty? (select user (where {:username username})))))

(defn add-user! [u]
  (insert user
          (values u)))

(defn get-user [username]
  (first (select user (where {:username username}))))

(defn check-user [username password]
  (let [u (get-user username)]
    (when (and u
               (BCrypt/checkpw password (:hash u)))
      u)))

(defn search [& [include exclude]]
  (let [inctags (string->tags (or include ""))
        exctags (string->tags (or exclude ""))]
    (-> (select* event)
        (join tag)
        (where {:tag.tag [in inctags]})
        (with tag)
        (exec))))