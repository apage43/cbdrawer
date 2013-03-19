(ns cbdrawer.view
  (:use [slingshot.slingshot :only [try+]])
  (:require [clj-http.client :as http]
            [cheshire.core :as json]))

(def ^:dynamic *view-chunk-size* 200)

(defn- view-cursor [url & [parms]]
  {:url url
   :last-key nil
   :last-id nil
   :parms (merge {:reduce false} parms)
   :limit *view-chunk-size*
   :end false})

(defn- fetch-view-chunk [curs]
  (let [{:keys [url last-key limit last-id skip parms]} curs
        rqparms (merge parms
                       {:limit limit}
                       (if skip {:skip skip})
                       (if last-key {:startkey (json/encode last-key)
                                     :startkey_docid last-id}))
        rq (http/get url {:query-params rqparms :as :json})
        jbody (:body rq)
        rows (:rows jbody)
        lastrow (last rows)
        newcurs (merge curs {:last-key (:key lastrow)
                             :last-id (:id lastrow)
                             :skip 1}
                       (if (empty? rows) {:end true}))]
    [newcurs rows]))

(defn- view-iterator [cursatom]
  (lazy-seq
   (if (:end @cursatom) []
       (let [[nextcurs rows] (fetch-view-chunk @cursatom)]
         (reset! cursatom nextcurs)
         (concat rows (lazy-seq (view-iterator cursatom)))))))

(defn view-seq
  "Return a lazy sequence of items from the view at url, taking view parameters as a map.
  If startkey or endkey are passed they will be JSON encoded."
  [url {:keys [startkey endkey] :as params}]
  (let [startend (merge (if startkey {:startkey (json/encode startkey)})
                        (if endkey {:endkey (json/encode endkey)}))
        cursatom (atom (view-cursor url (merge params startend)))]
    (view-iterator cursatom)))

(defn view-url
  "Get the a URL for use with view-seq. bases is the Couch API bases list from cbdrawer.client/capi-bases"
  [bases ddoc viewname]
  (str (rand-nth bases) "/_design/" ddoc "/_view/" viewname))

(defn install-ddoc
  "Install a design document to a bucket, if it doesn't already exist, or if the current one
  is not the same."
  [bases ddoc source]
  (let [ddocurl (str (rand-nth bases) "/_design/" ddoc)
        current (try+ (:body (http/get ddocurl)) (catch [:status 404] {:as ex} nil))]
    (if (not= current source)
      (:body (http/put ddocurl {:content-type :json :body source :as :json}))
      :up_to_date)))
