(ns cbdrawer.transcoders
  (:require [cheshire.core :as cheshire])
  (:import [net.spy.memcached.transcoders Transcoder]
           [net.spy.memcached CachedData]))

(def 
  ^{:doc
 "A spymemcached Transcoder that serializes Clojure datastructures to
  JSON, which allows use of Couchbase Views."}
  json-transcoder
  (proxy [Transcoder] []
    (asyncDecode [_] false)
    (decode [^CachedData bs] (cheshire/parse-string (String. (.getData bs)) true))
    (encode [o] (CachedData. 0 (.getBytes (cheshire/generate-string o)) CachedData/MAX_SIZE))
    (getMaxSize [] CachedData/MAX_SIZE)))

(def 
  ^{:doc
 "A spymemcached Transcoder that serializes to SMILE, a JSON-compatible compact
 binary format. http://wiki.fasterxml.com/SmileFormatSpec"}
  smile-transcoder
  (proxy [Transcoder] []
    (asyncDecode [_] false)
    (decode [^CachedData bs] (cheshire/parse-smile (.getData bs) true))
    (encode [o] (CachedData. 0 (cheshire/generate-smile o) CachedData/MAX_SIZE))
    (getMaxSize [] CachedData/MAX_SIZE)))

(def 
  ^{:doc
 "A spymemcached Transcoder that serializes Clojure datastructures.
  The default java Serializing transcoder will work for any Clojure type,
  but this format is smaller and readable. See edn-format.org"}
  clj-transcoder
  (proxy [Transcoder] []
    (asyncDecode [_] false)
    (decode [^CachedData bs] (read-string (String. (.getData bs))))
    (encode [o] (CachedData. 0 (.getBytes (pr-str o)) CachedData/MAX_SIZE))
    (getMaxSize [] CachedData/MAX_SIZE)))

(def ^{:doc "The default spymemcached serializing transcoder"} spy-transcoder
  (net.spy.memcached.transcoders.SerializingTranscoder.))

