(ns cbdrawer.client
  (:refer-clojure :exclude [get set!])
  (:require [cbdrawer.transcoders :as xcoders])
  (:import [com.couchbase.client CouchbaseClient CouchbaseConnectionFactory]
           [net.spy.memcached.transcoders Transcoder]
           [net.spy.memcached CachedData CASMutator CASMutation]
           [java.net URI]))

(defn factory
  "Create a CouchbaseConnectionFactory from a cluster base URI.
  (factory \"bucketname\" \"password\" \"http://localhost:8091/\")"
  [bucket password & uris]
  (CouchbaseConnectionFactory. (mapv #(.resolve (URI. %) "/pools") uris) bucket password))

(defn client
  "Open a CouchbaseClient.
   (client \"bucketname\" \"password\" \"http://localhost:8091/\") or (client factory)"
  ([^CouchbaseConnectionFactory factory]
   (CouchbaseClient. factory))
  ([bucket password & uris]
   (CouchbaseClient. (mapv #(.resolve (URI. %) "/pools") uris)  bucket password)))

(defn shutdown
  "Shut down a CouchbaseClient"
  ([^CouchbaseClient client] (.shutdown client))
  ([^CouchbaseClient client timeout unit] (.shutdown client timeout unit)))

(defn capi-bases
  "Get the Couch-API base URLs from a cluster/bucket."
  [^CouchbaseConnectionFactory connectionfactory]
  (vec (.getCouchServers (.getVBucketConfig connectionfactory))))

(def ^:dynamic ^Transcoder *transcoder* xcoders/clj-transcoder)

(defn set-transcoder!
  "Globally reset the default transcoder."
  [transcoder]
  (alter-var-root #'*transcoder* (fn [_] transcoder)))


(defmacro with-transcoder
  "Serialize and deserialize items in this block using the specified transcoder.
  (with-transcoder my-transcoder exprs)"
  [xcoder & body]
  `(binding [*transcoder* ~xcoder]
     ~@body))

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

(defn- cas-with-transcoder!
  "Atomically update an item with the result of applying `f` to it,
   with a specified transcoder."
  [^CouchbaseClient conn key f ^Transcoder transcoder]
     (let [mutation (proxy [CASMutation] []
                      (getNewValue [current]
                        (f current)))]
       (.cas (CASMutator. conn transcoder) key nil 0 mutation)))

(defn- to-key
  [keylike]
  (cond 
    (keyword? keylike) (let [kns (namespace keylike)
                             kname (name keylike)]
                         (str (when kns (str kns "/")) kname))
    true (str keylike)))

(defn cas!
  "Atomically update an item with f and additional args. Returns the new value."
  [^CouchbaseClient conn k f & args]
  (cas-with-transcoder! conn (to-key k) #(apply (partial f %) args) *transcoder*))

(defn get
  "Get an item, synchronously"
  [^CouchbaseClient conn k]
  (.get conn (to-key k) *transcoder*))

(defn get-async
  "Get an item asynchronously. Returns the result in a future"
  [^CouchbaseClient conn k]
  (derefable-future (.asyncGet conn (to-key k) *transcoder*)))

(defn delete!
  "Delete an item. Returns a boolean indicating whether the operation succeeded
  in a future"
  [^CouchbaseClient conn k]
  (derefable-future (.delete conn (to-key k))))

(defn add!
  "Create an item iff it doesn't already exist. Returns a boolean indicating
  whether the operation succeeded in a future"
  ([^CouchbaseClient conn k value ^long expiration] 
   (derefable-future (.add conn (to-key k) expiration value *transcoder*)))
  ([conn k value]
   (add! conn k value 0)))

(defn force!
  "Update the value of an item, creating it if it does not exist. Returns a
  boolean indicating whether the operation succeeded in a future"
  ([^CouchbaseClient conn k value ^long expiration] 
   (derefable-future (.set conn (to-key k) expiration value *transcoder*)))
  ([conn k value]
   (force! conn k value 0)))
