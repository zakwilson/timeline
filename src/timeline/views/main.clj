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
            [clojure.string :as s]
            [noir.session :as session])
  (:import [java.io File]))

(defpartial layout [content & {:keys [js css]}]
  (html5
   [:head
    [:title "Timeline"]
    [:style "textarea {width: 600px; height: 300px;}
             label {display: block;}
             input {width: 200px; background-color: white; color: black;}
             div#entryform, div#loginform{min-height: 10em; float: left; margin-right: 2em;}"]
    (if (string? js)
      (include-js js)
      (map include-js js))
    (if (string? css)
      (include-css css)
      (map include-css css))
    ]
   [:body
    content
    [:div {:style "height: 100px"}]
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
  [:form {:id "evt-form"
          :method "post"
          :enctype "multipart/form-data"
          :action "/event/new"}
           (event-fields evt)
           (submit-button "Create")])

(defn detect-date-format [date-string]
  (let [[y m d] (s/split date-string #"/")]
    (cond (and y m d) "%Y/%c/%e"
          (and y m) "%Y/%c"
          y "%Y")))

(defn prepare-event [event]
  (-> (map-keys date event [:startdate :enddate])
      (dissoc :tags :file)
      (assoc :start_date_format (detect-date-format (:startdate event))
             :end_date_format (detect-date-format (:enddate event)))))

(defpage event-post [:post "/event/new"] {:as event}
  (if (and (valid-event? event)
           (session/get :user))
    (let [req (ring-request)
          event-for-sql (prepare-event event)
          evt (if (has-value? (:id event))
                ;id can't be prepared in the model because it's in the where clause
                (data/update-event! (update-in event-for-sql
                                               [:id]
                                               integer))
                (data/add-event! (assoc (dissoc event-for-sql :id)
                                   :user_id (:username (session/get :user)))))]
      (spit "event" event)
      (data/tag-event! evt (:tags event))
      (when (has-value? (get-in req [:params :file :filename]))
        (data/handle-file evt req))
      (redirect "/"))
    (render "/edit" event)))

(defpage event-delete [:post "/event/delete"] {:as event}
  (when (session/get :user)
    (data/delete-event! {:id (integer (:id event))}))
  "")

(defpartial login-form []
  (form-to [:post "/user/login"]
           (label "username" "Username")
           (text-field "username")
           (label "password" "Password")
           (password-field "password")
           (submit-button "Log in")))

(defpartial search-form []
  (form-to [:get "/"]
           (label "include" "Tags to include (comma delimited)")
           (text-field "include")
           (submit-button "Search"))
  [:p (link-to "/" "Show all")])

(defpage "/edit" {:as event}
  (layout
   (edit-event-form event)))

(defpage "/" {:as include}
  (layout [:div#maincontent
           [:div#placement {:style "height: 600px"}]
           [:form (hidden-field "include" (:include include))]
           [:div#flash {:style "margin-top: 20px"}
            (session/flash-get)]
           [:br]
           (if (session/get :user)
             [:div#entryform 
              [:p (str "Logged in as "
                       (:username (session/get :user))
                       " ")
               (link-to "/user/logout" "(Log out)")]
              (edit-event-form)]
             [:div#loginform 
              (link-to "/user/new" "Create an account")
              (login-form)])
           [:div#searchform
            [:h3 "Search"]
            (search-form)]]
          :css ["/widget/css/aristo/jquery-ui-1.8.5.custom.css"
                "/widget/js/timeglider/Timeglider.css"
                "/css/anytimec.css"]
          :js ["/widget/js/jquery-1.4.4.min.js"
               "/widget/js/jquery-ui-1.8.9.custom.min.js"
               "/widget/js/timeglider-0.0.9.min.js"
               "/javascript/timeline.js"
               "/javascript/anytimec.js"
               "/javascript/event.js"]))

(defn write-json-datetime [x out _]
  (.print out
          (str "\"" (unparse (formatters :year-month-day) x) "\"")))

(extend org.joda.time.DateTime Write-JSON
        {:write-json write-json-datetime})

(defn append-tags [e]
  (let [tags (:tag e)]
    (if (seq tags)
      (assoc e :description
             (str (:description e)
                  "\n\n"
                  "Tags: "
                  (apply str
                         (interpose ", " (:tag e)))))
      e)))

(defn md-desc [e]
  (assoc e :description (md (:description e))))

(defn md-links [e]
  (let [uploads (:uploads e)]
    (if (seq uploads)
      (assoc e :description
             (str (:description e)
                  "\n\nFiles: "
                  (apply str
                         (map #(str "[" (:filename %) "]"
                                    "("
                                    "/uploads/"
                                    (:id e)
                                    "/"
                                    (:filename %)
                                    ")\n")
                              uploads))))
      e)))

(defn tag-user [e]
  (if (:user_id e)
    (assoc e :description
           (str (:description e)
                "\n\nCreated by: " (:user_id e)))
    e))

(defn date-display [e]
  (let [disp (case (:start_date_format e)
                   "%Y" "ye"
                   "%Y/%c" "mo"
                   "%Y/%c/%e" "da"
                   nil "ye")]
    (assoc e :date_display disp)))

(defpage "/event/:id" {:keys [id]}
  (json-str (data/get-event (integer id))))

(defpage timeline "/timeline" {:keys [include]}
  (json-str [{:id "history"
              :title (if (has-value? include)
                       "Search results"
                       "A brief history of civilization")
              :description (if (has-value? include)
                             include
                             "All the interesting bits")
              :focus_date "-432-01-01 12:00:00"
              :initial_zoom "61"
              :events (->> (if (has-value? include)
                             (data/search include)
                             (data/get-all-events))
                           (map append-tags)
                           (map md-links)
                           (map md-desc)
                           (map tag-user)
                           (map date-display))}]))