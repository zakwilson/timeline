(ns timeline.views.users
  (:use [noir core validation request]
        [hiccup core page-helpers form-helpers]
        [ring.middleware file]
        [ring.util response]
        compojure.core
        clojure.data.json
        [clj-time.core :exclude [extend]]
        clj-time.format
        [zutil util map]
        [timeline common]
        timeline.views.main)
  (:require [timeline.data :as data]
            [clojure.string :as s]
            [noir.session :as session])
  (:import [java.io File]))

(defpartial user-fields [{:keys [username email password confirm]}]
  (label "username" "Username")
  (on-error :username error-item)
  (text-field "username" username)

  (label "email" "Email")
  (on-error :email error-item)
  (text-field "email" email)

  (label "password" "Password")
  (on-error :password error-item)
  (password-field "password")

  (label "confirm" "Password (again)")
  (password-field "confirm"))

(defn valid-user? [{:keys [username email password confirm id]}]
  (rule (has-value? username)
        [:username "You'll have to call yourself something"])
  (rule (not (data/user-exists? username))
        [:username (str "Sorry, \"" 
                        username
                        "\" "
                        " is already taken")])
  (rule (has-value? email)
        [:email "Email is required"])
  (rule (<= 5 (count password))
        [:password "This may not be a bank account, but you should still use a password with at least five characters"])
  (rule (= password confirm)
        [:password "Passwords didn't match"])
  (not (errors? :username :email :password :confirm)))

(defpage "/user/new" {:as user}
  (layout 
   (form-to [:post "/user/new"]
            (user-fields user)
            (submit-button "Create"))))

(defpage [:post "/user/new"] {:as user}
  (if (valid-user? user)
    (do (data/add-user! user)
        (session/put! :user (data/get-user (:username user)))
        (session/flash-put! "Account created - you are logged in")
        (redirect "/"))
    (render "/user/new" user)))

(defpage [:post "/user/login"] {:as user}
  (if (data/check-user (:username user) (:password user))
    (session/put! :user (data/get-user (:username user)))
    (session/flash-put! "Login failed"))
  (redirect "/"))

(defpage  "/user/logout" []
  (session/clear!)
  (redirect "/"))