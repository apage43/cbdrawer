(ns cbdrawer.transcoders
  (:require [cheshire.core :as cheshire])
  (:import [net.spy.memcached.transcoders Transcoder]
           [net.spy.memcached CachedData]))

(def 
  ^{:doc
 "A spymemcached Transcoder that serializes Clojure datastructures to
  JSON, which allows use of Couchbase Views."}
  json-transcoder
  (reify Transcoder
    (asyncDecode [_this _] false)
    (decode [_this bs] (cheshire/parse-string (String. (.getData bs)) true))
    (encode [_this o] (CachedData. 0 (.getBytes (cheshire/generate-string o)) CachedData/MAX_SIZE))
    (getMaxSize [_this] CachedData/MAX_SIZE)))

(def 
  ^{:doc
 "A spymemcached Transcoder that serializes to SMILE, a JSON-compatible compact
 binary format. http://wiki.fasterxml.com/SmileFormatSpec"}
  smile-transcoder
  (reify Transcoder
    (asyncDecode [_this _] false)
    (decode [_this bs] (cheshire/parse-smile (.getData bs) true))
    (encode [_this o] (CachedData. 0 (cheshire/generate-smile o) CachedData/MAX_SIZE))
    (getMaxSize [_this] CachedData/MAX_SIZE)))

(def 
  ^{:doc
 "A spymemcached Transcoder that serializes Clojure datastructures.
  The default java Serializing transcoder will work for any Clojure type,
  but this format is smaller and readable. See edn-format.org"}
  clj-transcoder
  (reify Transcoder
    (asyncDecode [_this _] false)
    (decode [_this bs] (read-string (String. (.getData bs))))
    (encode [_this o] (CachedData. 0 (.getBytes (pr-str o)) CachedData/MAX_SIZE))
    (getMaxSize [_this] CachedData/MAX_SIZE)))

(def ^{:doc "The default spymemcached serializing transcoder"} spy-transcoder
  (net.spy.memcached.transcoders.SerializingTranscoder.))

