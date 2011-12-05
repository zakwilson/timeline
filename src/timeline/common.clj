(ns timeline.common
  (:import [com.petebevin.markdown MarkdownProcessor]))

;(def ^{:dynamic true} *md* (per-thread-singleton #(MarkdownProcessor.)))
(def mdp (MarkdownProcessor.))

(defn ^String md [content]
  (.markdown ^MarkdownProcessor mdp content))

; This provides for a system for sitewide configuration and defaults
; There MUST be a config.clj in the base directory with, at a minimum
; a database connection map named :db. It should look like this:

;{ :db {:classname   "org.postgresql.Driver"
;      :subprotocol "postgresql"
;      :user        "db-user-name"
;      :password    "db-password"
;      :subname      "db-name"}}

(def base-config
     {:site-url ""
      :site-name "Timeline"
      :email-from ""
      :site-abbrev "Timeline"})

(def config
     (atom {}))

(defn read-config [_]
  (try (merge base-config
              (read-string (slurp "config.clj")))
       (catch Exception _
         base-config)))

(defn load-config []
  (swap! config read-config))

(defn save-config []
  (spit "config.clj" @config))