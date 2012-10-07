(defproject apage43/cbdrawer "0.1.0-SNAPSHOT"
  :description "Couchbase utilities"
  :url "http://github.com/apage43/cbdrawer"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories {"couchbase" {:url "http://files.couchbase.com/maven2/"}}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [clj-http "0.5.5"]
                 [cheshire "4.0.3"]
                 [couchbase/couchbase-client "1.1-dp3"
                  ; Keep couchbase from pulling in an incompatible httpcore
                  ; with the httpclient clj-http brings in
                  :exclusions [org.apache.httpcomponents/httpcore
                               org.apache.httpcomponents/httpcore-nio]]
                 [org.apache.httpcomponents/httpcore "4.2.1"]
                 [org.apache.httpcomponents/httpcore-nio "4.2.1"]])
