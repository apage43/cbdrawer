(ns cbdrawer.client
  (:require [cheshire.core :as json])
  (:import [com.couchbase.client CouchbaseClient CouchbaseConnectionFactory]
           [net.spy.memcached.transcoders Transcoder]
           [net.spy.memcached CachedData]
           [java.net URI]))

(def 
  ^{:doc
 "A spymemcached Transcoder that serializes Clojure datastructures to
  JSON, which allows use of Couchbase Views."}
  json-transcoder
  (proxy [Transcoder] []
    (asyncDecode [_] false)
    (decode [bs] (json/parse-string (String. (.getData bs)) true))
    (encode [o] (CachedData. 0 (.getBytes (json/encode o)) CachedData/MAX_SIZE))
    (getMaxSize [] CachedData/MAX_SIZE)))

(def 
  ^{:doc
 "A spymemcached Transcoder that serializes Clojure datastructures.
  The default java Serializing transcoder will work for any Clojure type,
  but this format is smaller and readable. See edn-format.org"}
  clj-transcoder
  (proxy [Transcoder] []
    (asyncDecode [_] false)
    (decode [bs] (read-string (String. (.getData bs))))
    (encode [o] (CachedData. 0 (.getBytes (pr-str o)) CachedData/MAX_SIZE))
    (getMaxSize [] CachedData/MAX_SIZE)))

(defn factory
  "Create a CouchbaseConnectionFactory from a cluster base URI.
  (factory \"bucketname\" \"password\" \"http://localhost:8091/\")"
  [bucket password & uris]
  (CouchbaseConnectionFactory. (mapv #(.resolve (URI. %) "/pools") uris) bucket password))

(defn client
  "Open a CouchbaseClient.
   (client \"bucketname\" \"password\" \"http://localhost:8091/\") or (client factory)"
  ([factory]
   (CouchbaseClient. factory))
  ([bucket password & uris]
   (CouchbaseClient. (mapv #(.resolve (URI. %) "/pools") uris)  bucket password)))

(defn shutdown
  "Shut down a CouchbaseClient"
  ([client] (.shutdown client))
  ([client timeout unit] (.shutdown client timeout unit)))

(defn capi-bases
  "Get the Couch-API base URLs from a cluster/bucket."
  [^CouchbaseConnectionFactory connectionfactory]
  (vec (.getCouchServers (.getVBucketConfig connectionfactory))))
