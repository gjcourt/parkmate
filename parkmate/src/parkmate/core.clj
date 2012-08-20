(ns parkmate.core
  (:require [parkmate.backend :as db]
            [korma.core :as k]
            [clj-time.core :as ct])
  (:use net.cgrand.enlive-html
        parkmate.utils
        ring.util.response
        [net.cgrand.moustache :only [app pass]]
        [ring.middleware file params session])
  (:gen-class))

(def form-sel [:form])
(def create-sel #{[:head :> :script] [:form]})
(def rem-sel #{[:head :> #{:style :script}] [:body]})

(defsnippet login "templates/login.html" form-sel
  [])

(defsnippet reminder "templates/reminder.html" rem-sel
  [])

(defsnippet create "templates/create.html" create-sel
  [])

(deftemplate index "templates/index.html"
  [{:keys [username title style script content]}]
  [:#title] (maybe-content title)
  [:head [:style first-of-type]] (maybe-after style)
  [:#content] (maybe-content content)
  [:.icon-user] (maybe-after (str " " username))
  [:body [:script last-of-type]] (maybe-after script))

(defn snip2dict
  [snippet sels]
  (let [items (map #(not-empty (select snippet %)) sels)]
    (reduce #(if-not (nil? (nth %2 1)) (apply assoc %1 %2))
            {} (map vector [:style :script :content] items))))

(defn wrapped-reminder-snip
  []
  (snip2dict (reminder) [[:style] [:script] [:body :> :*]]))

(defn wrapped-create-snip
  []
  (snip2dict (create) [[:style] [:script] [:form]]))

(defn user-query!
  [email pass]
  (k/select db/user
    (k/where {:email email
              :pass (sha1-hash pass)})))

(defn reminder-query!
  "Select all reminders for a user"
  [id]
  (let [dt (ct/now)]
    (k/select db/reminder
      (k/where {:user_id id
                :target < dt})
      (k/order :target :ASC)))

(defn authenticate-user
  [{user "user" pass "password"}]
  (let [query (user-query! user pass)]
    (if (= (count query) 1)
      (assoc (redirect "/reminder") :session {:user (first query)})
      (redirect "/login"))))

(defn authenticated?
  [session]
  (seq (:user session)))

(defn admin-user?
  [session]
  (= "admin" (:user session)))

(defn create-reminder!
  [{p :form-params session :session}]
  (let [offset (:offset p)
        trigger (:trigger p)
        ts (ct/plus (ct/now) (ct/hours offset))
        trg (ct/plus (ct/now) (ct/hours  trigger))
        uid (-> session :session :user)]
    (db/mk-reminder uid, ts, trg)))

; Middleware
(defn wrap-user-auth
  [app]
  (fn [req]
    (let [uri (:uri req)
          session (:session req)]
      (if (or (authenticated? session)
              (.startsWith uri "/css/") ; *.css
              (.startsWith uri "/scripts/")) ; *.js
        (app req)
        (redirect "/login")))))

;; Views
(defn home-handler
  [req]
  (render-to-response
    (index {:username (-> req :session :user :username)
            :title "Parkmate"})))

(defn login-handler
  [req]
  (if-let [params (not-empty (:form-params req))]
    (authenticate-user params)
    (render-to-response
      (index {:username (-> req :session :user :username)
              :title "Login"
              :content (login)}))))

(defn logout-handler
  [req]
  (assoc (redirect "/") :session nil))

(defn reminder-handler
  [req]
  (render-to-response
    (index (conj {:username (-> req :session :user :username)
                  :title "Reminder"}
                 (wrapped-reminder-snip)))))

(defn create-handler
  [req]
  (if-let [params (not-empty (:form-params req))]
    (create-reminder! params)
    (render-to-response
      (index (conj {:username (-> req :session :user :username)
                    :title "New reminder"}
                   (wrapped-create-snip))))))

;; Routes
(def routes
  (app
    (wrap-session)
    ; Signin or redirect to /<username>
    [""] home-handler
    ["login"] (wrap-params login-handler)
    ["logout"] logout-handler
    ; Auth required
    ["reminder" &] (app
                     (wrap-user-auth)
                     [""] reminder-handler
                     [&] pass)
    ["reminder" "create" &] (wrap-params
                              create-handler)
    [&] page-not-found))

; TODO
; Start off a background process that polls for reminders and triggers emails when approipriate.

(defn -main
  []
  (run-server routes))
