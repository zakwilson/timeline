(ns timeline.views.main
  (:use noir.core
        [hiccup core page-helpers]))

(defpartial layout [& content]
  (html5
   [:head
    [:title "Timeline"]
    ]
   [:body
    [:h1 "Nothing to see yet"]]
   ))

(defpage "/" []
  (layout [:h2 "Coming soon"]))