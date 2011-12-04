(ns timeline.views.main
  (:use noir.core
        [hiccup core page-helpers]
        [ring.middleware file]
        [ring.util response]))

(defpartial layout [content & {:keys [js css]}]
  (html5
   [:head
    [:title "Timeline"]
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

(defpage home "/" []
  (layout [:div#maincontent
           [:div#placement]]

          :css ["/widget/css/aristo/jquery-ui-1.8.5.custom.css"
                "/widget/js/timeglider/Timeglider.css"]
          :js ["/widget/js/jquery-1.4.4.min.js"
               "/widget/js/jquery-ui-1.8.9.custom.min.js"
               "/widget/js/timeglider.min.js"
               "/javascript/timeline.js"]))

(post-route "/javascript/*" [*]
            (or (file-response (str "static/javascript/" *)) :next))

(post-route "/css/*" [*]
            (or (file-response (str "static/css/" *)) :next))

(post-route "/widget/*" [*]
            (or (file-response (str "static/widget/" *)) :next))