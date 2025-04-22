;; lein pom
;; mvn dependency:tree -Dverbose=true
(defproject jepsen.template "0.1.0-SNAPSHOT"
  :description "A Jepsen test for hbase"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main jepsen.systarget
  :repositories {"local" "file:///u/vqx2dc/lein_repo"}
  :dependencies [[org.clojure/clojure "1.11.4"]
                 [jepsen "0.3.5"]
                 [edu.uva.liftlab/java-client "1.0.31"]])
