(defproject datomic/codeq "0.1.0-SNAPSHOT"
  :description "codeq does a code-aware import of your git repo into a Datomic db"
  :url "http://datomic.com"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main datomic.codeq.core
  :plugins [[lein-tar "1.1.0"]]
  :dependencies [
                 [com.datomic/client-cloud "0.8.96"]
                 ;[com.datomic/datomic-free "0.9.5703.21"]
                 [commons-codec "1.14"]
                 [org.clojure/clojure "1.10.1"]]
  :source-paths ["src" "examples/src"])
