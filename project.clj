(defproject apage43/cbdrawer "0.2.0"
  :description "Couchbase utilities"
  :url "http://github.com/apage43/cbdrawer"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories {"couchbase" {:url "http://files.couchbase.com/maven2/"}}
  :codox {:src-dir-uri "http://github.com/apage43/cbdrawer/blob/master"
          :src-linenum-anchor-prefix "L"}
  :plugins [[codox "0.6.4"]]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clj-http "0.6.5"]
                 [cheshire "5.0.2"]
                 [couchbase/couchbase-client "1.1.4"
                  ; Keep couchbase from pulling in an incompatible httpcore
                  ; with the httpclient clj-http brings in
                  :exclusions [org.apache.httpcomponents/httpcore]]])
