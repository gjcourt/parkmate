(defproject parkmate "0.1.0-SNAPSHOT"
  :description "A little testing project"
  :url "http://gjcourt.com/parkmate"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  ; :aot :all
  :main parkmate.core
  :dependencies [[clj-time "0.4.4"]
                 [enlive "1.0.1"]
                 [korma "0.3.0-beta11"]
                 [net.cgrand/moustache "1.1.0"]
                 [org.clojure/clojure "1.4.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [org.xerial/sqlite-jdbc "3.7.2"]
                 [ring "1.1.1"]
                 ])
