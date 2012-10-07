(ns cbdrawer.ops
  (:refer-clojure :exclude [get set!])
  (:require [cbdrawer.client :as client])
  (:import [net.spy.memcached CASMutator CASMutation]))

(defn cas-with-transcoder!
  "Atomically update an item with the result of applying `f` to it,
   with a specified transcoder."
  [conn key f transcoder]
     (let [mutation (proxy [CASMutation] []
                      (getNewValue [current]
                        (f current)))]
       (.cas (CASMutator. conn transcoder) key nil 0 mutation)))

(def ^:private cb-default-transcoder (net.spy.memcached.transcoders.SerializingTranscoder.))
(def ^:dynamic *transcoder* client/clj-transcoder)

(defn derefable-future
  "Wrap a java.util.concurrent.Future so it can be used with deref/@"
  [^java.util.concurrent.Future fut]
  (reify
    clojure.lang.IDeref
     (deref [_] (.get fut))
    java.util.concurrent.Future
     (get [_ timeout unit] (.get fut timeout unit))
     (isCancelled [_] (.isCancelled fut))
     (isDone [_] (.isDone fut))
     (cancel [_ interrupt?] (.cancel fut interrupt?))))

(defmacro with-json-encoding
  "Serialize and deserialize items in this block as JSON rather than Clojure strings."
  [& body]
  `(binding [*transcoder* client/json-transcoder]
     ~@body))

(defmacro with-cb-default-encoding
  "Serialize and deserialize with the default Couchbase client behavior, rather
  than as Clojure string."
  [& body]
  `(binding [*transcoder* cb-default-transcoder]
     ~@body))

(defn cas!
  "Atomically update an item with f and additional args."
  [conn key f & args]
  (cas-with-transcoder! conn key #(apply (partial f %) args) *transcoder*))

(defn get
  "Get an item, synchronously"
  [conn k]
  (.get conn k *transcoder*))

(defn get-async
  "Get an item, asynchronously"
  [conn k]
  (derefable-future (.asyncGet conn k *transcoder*)))

(defn delete!
  "Delete an item"
  [conn k]
  (derefable-future (.delete conn k)))

(defn add!
  "Create an item iff it doesn't already exist"
  [conn k expiration value]
  (derefable-future (.add conn k expiration value *transcoder*)))

(defn force!
  "Update the value of an item, creating it if it does not exist"
  [conn k expiration value]
  (derefable-future (.set conn k expiration value *transcoder*)))
