(defproject timeline "0.0.1-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [noir "1.2.1"]
                 [korma "0.2.1"]
                 [postgresql/postgresql "9.0-801.jdbc4"]
                 [org.mindrot/jbcrypt "0.3m"]
                 [org.apache.commons/commons-email "1.2"]
                 [org.markdownj/markdownj "0.3.0-1.0.2b4"]
                 [net.htmlparser.jericho/jericho-html "3.1"]
                 [clj-http "0.1.3"]
                 [clj-time "0.3.3"]]
  :dev-dependencies [[swank-clojure "1.3.4-SNAPSHOT"]]
  :main timeline.web.main)