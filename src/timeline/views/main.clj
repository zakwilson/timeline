(ns timeline.views.main
  (:use [noir core validation]
        [hiccup core page-helpers form-helpers]
        [ring.middleware file]
        [ring.util response]
        compojure.core
        clojure.data.json
        [clj-time.core :exclude [extend]]
        clj-time.format
        [zutil util map])
  (:require [timeline.data :as data]
            [clojure.string :as s]))

(defpartial layout [content & {:keys [js css]}]
  (html5
   [:head
    [:title "Timeline"]
    [:style "textarea {width: 600px; height: 300px; display: block;}
             input {width: 200px; background-color: white; color: black; display: block;}"]
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

(defpartial event-fields [{:keys [startdate enddate title description link importance]}]
  (on-error :startdate error-item)
  (label "startdate" "And so it begins...")
  (text-field "startdate" startdate)
  (on-error :enddate error-item)
  (label "enddate" "When will it end? (optional)")
  (text-field "enddate" enddate)
  (on-error :title error-item)
  (label "title" "Title")
  (text-field "title" title)
  (on-error :description error-item)
  (label "description" "Description")
  (text-area "description" description)
  (on-error :link error-item)
  (label "link" "Link (wikipedia, maybe?)")
  (text-field "link" link)
  (on-error :importance error-item)
  (label "importance" "Importance (1-100)")
  (text-field "importance" (or importance 50)))

(defn valid-event? [{:keys [startdate enddate title description link importance]}]
  (rule (has-value? startdate)
        [:startdate "An event must have a starting date (ending date is optional)"]
  (rule (has-value? title)
        [:title "A title is required"])
  (rule (has-value? description)
        [:description "A description is required"])
  (comment (rule (and (has-value? importance)
             (>= 100 (integer importance))
             (<= 1 (integer importance)))
        [:importance "Importance must be between 1 and 100"]))
  (not (errors? :lastname :firstname))))

(defpartial edit-event-form [& [evt]]
  (form-to [:post "/event/new"]
           (event-fields evt)
           (submit-button "Create")))

(defn date [date-str]
  (when-not (empty? date-str)
    (let [[y m d e] (map maybe-integer
                         (s/split date-str #"/"))]
      (date-time y m d))))

(defpage event-post [:post "/event/new"] {:as event}
;  (if (valid-event? event)
    (do (data/add-event! (map-keys date event [:startdate :enddate]))
        (redirect "/")))
;    (render "/event/new" event)))
  

(defpage home "/" []
  (layout [:div#maincontent
           [:div#placement {:style "height: 600px"}]
           [:div#entryform {:style "margin-top: 30px"}
            (edit-event-form)]]
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
          (unparse (formatters :year-month-day) x)))

(extend org.joda.time.DateTime Write-JSON
        {:write-json write-json-datetime})

(defpage timeline "/timeline" []
  (json-str [{:id "history"
              :title "A brief history of civilization"
              :description "All the interesting bits"
              :focus_date "1941-12-07 12:00:00"
              :initial_zoom "37"
              :events (map #(dissoc % :tag)
                           (data/get-all-events))}]))


