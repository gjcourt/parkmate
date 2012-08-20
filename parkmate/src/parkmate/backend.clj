(ns parkmate.backend
  (:use [korma db core]
        [parkmate.utils :only [sha1-hash]]))

(defdb dev (sqlite3 {:db "parkmate.db"}))

(declare reminder)

(defentity user
  (has-many reminder)
  (database dev))

(defentity reminder
  (belongs-to user)
  (database dev))

(defn print-entity
  [entity label]
  (let [query (select entity)]
    (doseq [ent query]
      (println label ent))))

(defn print-rem
  []
  (let [query (select reminder (with user))]
    (doseq [ent query]
      (println "Rem" ent))))

(defn mk-user
  [username email pw first last flags admin]
  (insert user
    (values {:username username
             :email email
             :pass (sha1-hash pw)
             :first first
             :last last
             :flags flags
             :admin admin})))

(defn mk-reminder
  ([id date] (mk-reminder id date date))
  ([id date trigger]
    (insert reminder
      (values {:target date
               :trigger trigger
               :flags 0
               :media 1
               :user_id id}))))

(defn load-users
  []
  (mk-user "gjcourt" "gjcourt@gmail.com" "password" "George" "Courtsunis" 0 1)
  (mk-user "mclarke" "mike.clarke@gmail.com" "password" "Mike" "Clarke" 0 0)
  (mk-user "mhuang" "matt.huang@gmail.com" "password" "Matt" "Huang" 0 0)
  (mk-user "tlewis" "tlewis0411@gmail.com" "password" "Terri" "Lewis" 0 0))

(defn load-reminders
  []
  (let [ids (select user (fields :id))]
    (doseq [[d {id :id}] (map-indexed vector ids)]
      (mk-reminder id (str "2012-08-0" (inc d))))))

(defn -main
  []
  (load-users)
  (load-reminders)
  (print-entity user "User")
  (print-rem))
