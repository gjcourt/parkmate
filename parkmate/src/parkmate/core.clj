(ns parkmate.core
  (:use [net.cgrand.enlive-html :only [deftemplate
                                       defsnippet
                                       emit*
                                       nth-of-type
                                       content]]
        ring.util.response
        [net.cgrand.moustache :only [app]]
        [ring.middleware file params session]
        [parkmate.utils :only [run-server
                               page-not-found
                               render
                               render-to-response
                               maybe-content]]))


(deftemplate index "templates/index.html"
  [{:keys [title snippet]}]
  [:#title] (maybe-content title)
  [:#content] (maybe-content snippet))

(def form-sel [:form (nth-of-type 1)])

(defsnippet login "templates/login.html" form-sel
  []
  )

(defn authenticate-user
  [{user "user" password "password"}]
  (println "AUTHENTICATING" user password)
  (if (or (and (= user "gjcourt") (= password "password"))
          (and (= user "admin") (= password "secret")))
    (assoc (redirect "/alert") :session {:user user})
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
    (index {})))

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
      (render-to-response (index {:title "Login"
                                  :snippet (login)})))))

(defn logout-handler
  [req]
  (assoc (redirect "/") :session nil))

(defn alert-handler
  [req]
  (render-to-response
    (index {:title "ALERT"
            :content "This is the alert handler"})))

(defn admin-handler
  [req]
  (render-to-response
    (index {:title "ADMIN"
            :content "This is the admin handler"})))

(def routes
  (app
    (wrap-session)
    ; Signin or redirect to /<username>
    [""] home-handler
    ["login"] (wrap-params
                login-handler)
    ["logout"] logout-handler
    ; Auth required
    ["alert" &] (app
                  (wrap-user-auth)
                  alert-handler)
    [&] page-not-found))

(defonce *server* (run-server routes))

; TODO
; Start off a background process that polls for alerts and triggers emails when approipriate.
