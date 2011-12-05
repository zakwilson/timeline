(ns timeline.views.main
  (:use [noir core validation]
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
            [clojure.string :as s]))

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

(defpartial event-fields [{:keys [startdate enddate title description link importance tags]}]
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
  (form-to [:post "/event/new"]
           (event-fields evt)
           (submit-button "Create")))

(defpage event-post [:post "/event/new"] {:as event}
  (if (valid-event? event)
    (let [event-for-sql 
          (-> (map-keys date event [:startdate :enddate])
              (dissoc :tags))
          evt (data/add-event! event-for-sql)]
      (spit "event" event)
      (data/tag-event! evt (:tags event))
      (redirect "/"))
    (render "/" event)))

(defpage "/" {:as event}
  (layout [:div#maincontent
           [:div#placement {:style "height: 600px"}]
           [:div#entryform {:style "margin-top: 30px"}
            (edit-event-form event)]]
          :css ["/widget/css/aristo/jquery-ui-1.8.5.custom.css"
                "/widget/js/timeglider/Timeglider.css"
                "/css/anytimec.css"
                "/css/modal.css"]
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

(defn md-desc [e]
  (assoc e :description (md (:description e))))

(defpage timeline "/timeline" []
  (json-str [{:id "history"
              :title "A brief history of civilization"
              :description "All the interesting bits"
              :focus_date "-44-03-15 12:00:00"
              :initial_zoom "65"
              :event_modal {:type "full"
                            :href "/html/modal.html"}
              :events (map md-desc
                           (data/get-all-events))}]))
