(ns parkmate.core
  (:use net.cgrand.enlive-html
        parkmate.utils
        ring.util.response
        [net.cgrand.moustache :only [app pass]]
        [ring.middleware file params session]))

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
  [{:keys [title style script content]}]
  [:#title] (maybe-content title)
  [:head [:style first-of-type]] (maybe-after style)
  [:#content] (maybe-content content)
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

(defn authenticate-user
  [{user "user" password "password"}]
  (println "AUTHENTICATING" user password)
  (if (or (and (= user "gjcourt") (= password "password"))
          (and (= user "admin") (= password "secret")))
    (assoc (redirect "/reminder") :session {:user user})
    (redirect "/login")))

(defn authenticated?
  [session]
  (comment (seq (:user session)))
  true)

(defn admin-user?
  [session]
  (= "admin" (:user session)))

(defn create-reminder!
  [{week "week" day "day" hour "hour" :as params}]
  ; TODO fillin with call to backend
  (println "PARAMS" week, day, hour)
  (println "PARAMS" params)
  (redirect "/reminder/create"))

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

(defn login-handler
  [req]
  (if-let [params (not-empty (:form-params req))]
    (authenticate-user params)
    (render-to-response
      (index {:title "Login"
              :content (login)}))))

(defn logout-handler
  [req]
  (assoc (redirect "/") :session nil))

(defn reminder-handler
  [req]
  (render-to-response
    (index (conj {:title "Reminder"}
                 (wrapped-reminder-snip)))))

(defn create-handler
  [req]
  (if-let [params (not-empty (:form-params req))]
    (create-reminder! params)
    (render-to-response
      (index (conj {:title "New reminder"}
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

(defonce *server* (run-server routes))

; TODO
; Start off a background process that polls for reminders and triggers emails when approipriate.
