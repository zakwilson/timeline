(ns timeline.views.main
  (:use [noir core validation request]
        [hiccup core page-helpers form-helpers]
        [ring.middleware file]
        [ring.util response]
        compojure.core
        clojure.data.json
        [clj-time.core :exclude [extend]]
        clj-time.format
        [zutil util map]
        [timeline common])
  (:require [timeline.data :as data]
            [clojure.string :as s])
  (:import [java.io File]))

(defpartial layout [content & {:keys [js css]}]
  (html5
   [:head
    [:title "Timeline"]
    [:style "textarea {width: 600px; height: 300px;}
             label {display: block;}
             input {width: 200px; background-color: white; color: black;}"]
    (if (string? js)
      (include-js js)
      (map include-js js))
    (if (string? css)
      (include-css css)
      (map include-css css))
    ]
   [:body
    content
    ]
   ))

(defpartial error-item [[first-error]]
  [:p.error first-error])

(defpartial event-fields [{:keys [startdate enddate title description link importance tags id]}]
  (hidden-field "id" id)
  (label "startdate" "Start date (year/month/day) Only year is required")
  (on-error :startdate error-item)
  (text-field "startdate" startdate)
  [:a {:href "javascript:void(0)" :id "start-date-picker"} "Picker"]
  (label "enddate" "End date (optional)")
  (on-error :enddate error-item)
  (text-field "enddate" enddate)
  [:a {:href "javascript:void(0)" :id "end-date-picker"} "Picker"]
  (label "title" "Title")
  (on-error :title error-item)
  (text-field "title" title)
  (label "description" "Description")
  (on-error :description error-item)
  (text-area "description" description)
  (label "link" "Link (wikipedia, maybe?)")
  (on-error :link error-item)
  (text-field "link" link)
  (label "importance" "Importance (1-100)")
  (on-error :importance error-item)
  (text-field "importance" (or importance 50))
  (label "file" "Attach files")
  [:input {:type "file" :id "file" :name "file"}]
  (label "tags" "Tags (comma delimited)")
  (text-field "tags" tags))

(defn date [date-str]
  (when-not (empty? date-str)
    (let [base-args (map maybe-integer
                         (s/split date-str #"/"))
          args (filter identity base-args)]
      (when-not (empty? args)
        (apply date-time args)))))

(defn valid-event? [{:keys [startdate enddate title description link importance]}]
  (rule (has-value? startdate)
        [:startdate "An event must have a starting date (ending date is optional)"])
  (rule (date startdate)
        [:startdate "Invalid date"])
  (rule (if (empty? enddate)
          true
          (date enddate))
        [:enddate "Invalid date"])
  (rule (has-value? title)
        [:title "A title is required"])
  (rule (has-value? description)
        [:description "A description is required"])
  (rule (and (has-value? importance)
                      (>= 100 (integer importance))
                      (<= 1 (integer importance)))
        [:importance "Importance must be between 1 and 100"])
  (not (errors? :startdate :enddate :title :description :link :importance)))

(defpartial edit-event-form [& [evt]]
  [:form {:method "post"
          :enctype "multipart/form-data"
          :action "/event/new"}
           (event-fields evt)
           (submit-button "Create")])

(defn detect-date-format [date-string]
  (let [[y m d] (s/split date-string #"/")]
    (cond (and y m d) "%Y/%c/%e"
          (and y m) "%Y/%c"
          y "%Y")))

(defn handle-file [evt req]
  (let [file-req (get-in req [:params :file])
        filename (:filename file-req)
        tmpfile (:tempfile file-req)
        new-file-dir (str "resources/public/uploads/"
                           (:id evt)
                           "/")
        new-file-path (str new-file-dir filename)]
    (data/add-upload! evt filename)
    (ensure-directory-exists (File. new-file-dir))
    (spit new-file-path ; FIXME - is this needed?
          (slurp (.getPath tmpfile)))))

(defpage event-post [:post "/event/new"] {:as event}
  (if (valid-event? event)
    (let [req (ring-request)
          event-for-sql 
          (-> (map-keys date event [:startdate :enddate])
              (dissoc :tags :file)
              (assoc :start_date_format (detect-date-format (:startdate event))
                     :end_date_format (detect-date-format (:enddate event))))
          evt (if (has-value? (:id event))
                (data/update-event! event-for-sql)
                (data/add-event! (dissoc event-for-sql :id)))]
      (spit "event" event)
      (data/tag-event! evt (:tags event))
      (when (has-value? (get-in req [:params :file :filename]))
        (handle-file evt req))
      (redirect "/"))
    (render "/" event)))

(defpage "/" {:as event}
  (layout [:div#maincontent
           [:div#placement {:style "height: 600px"}]
           [:div#entryform {:style "margin-top: 30px"}
            (edit-event-form event)]]
          :css ["/widget/css/aristo/jquery-ui-1.8.5.custom.css"
                "/widget/js/timeglider/Timeglider.css"
                "/css/anytimec.css"]
          :js ["/widget/js/jquery-1.4.4.min.js"
               "/widget/js/jquery-ui-1.8.9.custom.min.js"
               "/widget/js/timeglider.min.js"
               "/javascript/timeline.js"
               "/javascript/anytimec.js"
               "/javascript/event.js"]))

(defn write-json-datetime [x out _]
  (.print out
          (str "\"" (unparse (formatters :year-month-day) x) "\"")))

(extend org.joda.time.DateTime Write-JSON
        {:write-json write-json-datetime})

(defn append-tags [e]
  (assoc e :description
         (str (:description e)
              "\n\n"
              "Tags: "
              (apply str
                     (interpose ", " (:tag e))))))

(defn md-desc [e]
  (assoc e :description (md (:description e))))

(defpage json-event "/event/:id" [id]
  (json-str (data/get-event id)))

(defpage timeline "/timeline" []
  (json-str [{:id "history"
              :title "A brief history of civilization"
              :description "All the interesting bits"
              :focus_date "-44-03-15 12:00:00"
              :initial_zoom "65"
              :events (map md-desc
                           (map append-tags
                                (data/get-all-events)))}]))
