(ns parkmate.core
  (:use net.cgrand.enlive-html
        ring.util.response
        [net.cgrand.moustache :only [app]]
        [ring.middleware file params session]
        [parkmate.utils :only [run-server
                               page-not-found
                               render
                               render-to-response
                               maybe-content
                               maybe
                               maybe-after]]))

(def form-sel [:form])
(def rem-sel #{[:head :> #{:style :script}] [:body]})

(defsnippet login "templates/login.html" form-sel
  [])

(defsnippet reminder "templates/reminder.html" rem-sel
  [])

(deftemplate index "templates/index.html"
  [{:keys [title style script content]}]
  [:#title] (maybe-content title)
  [:head [:style first-of-type]] (maybe-after style)
  [:#content] (maybe-content content)
  [:body [:script last-of-type]] (maybe-after script))

(defn rem2dict
  [snippet]
  {:style (select snippet [:style])
   :script (select snippet [:script])
   :content (select snippet [:body :> :*])})

(defn authenticate-user
  [{user "user" password "password"}]
  (println "AUTHENTICATING" user password)
  (if (or (and (= user "gjcourt") (= password "password"))
          (and (= user "admin") (= password "secret")))
    (assoc (redirect "/reminder") :session {:user user})
    (redirect "/login")))

(defn authenticated?
  [session]
  (seq (:user session)))

(defn admin-user?
  [session]
  (= "admin" (:user session)))

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
    (index {:title "Parkmate"})))

(defn wrap-login
  [app]
  (fn [req]
    (let [wrapped-req (wrap-params req)]
      (println wrapped-req)
      (app wrapped-req))))

(defn login-handler
  [req]
  (let [params (:form-params req)]
    (if (and (params "user")
             (params "password"))
      (authenticate-user params)
      (render-to-response
        (index {:title "Login"
                :content (login)})))))

(defn logout-handler
  [req]
  (assoc (redirect "/") :session nil))

(defn reminder-handler
  [req]
  (render-to-response
    (index (conj {:title "Reminder"}
                 (rem2dict (reminder))))))

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
                  reminder-handler)
    [&] page-not-found))

(defonce *server* (run-server routes))

; TODO
; Start off a background process that polls for reminders and triggers emails when approipriate.
