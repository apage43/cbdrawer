(ns cbdrawer.client
  (:refer-clojure :exclude [get])
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
  (vec (.. connectionfactory (getVBucketConfig) (getCouchServers))))

(def ^:dynamic ^Transcoder *transcoder* xcoders/json-transcoder)

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
  ([^CouchbaseClient conn key f ^Transcoder transcoder initial-value]
     (let [mutation (reify CASMutation
                      (getNewValue [_this current]
                        (f current)))]
       (.cas (CASMutator. conn transcoder) key initial-value 0 mutation)))
  ([conn key f transcoder]
   (cas-with-transcoder! conn key f transcoder nil)))

(defn- to-key ^String
  [keylike]
  (condp apply [keylike]
    keyword? (let [kns (namespace keylike)
                   kname (name keylike)]
               (str (when kns (str kns "/")) kname))
    (str keylike)))

(defn cas!
  "Atomically update an item with f and additional args. Returns the new value.
  Will fail if item does not exist."
  [^CouchbaseClient conn k f & args]
  (cas-with-transcoder! conn (to-key k) #(apply (partial f %) args) *transcoder*))

(defn add-or-cas!
  "Atomically update an item with f and additional args. Returns the new value.
  Takes an value to insert (as-is!) if the item does not already exist."
  [^CouchbaseClient conn k init f & args]
  (cas-with-transcoder! conn (to-key k) #(apply (partial f %) args) *transcoder* init))

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
  whether the operation succeeded in a future."
  ([^CouchbaseClient conn k value ^long expiration] 
   (derefable-future (.add conn (to-key k) expiration value *transcoder*)))
  ([conn k value]
   (add! conn k value 0)))

(defn update!
  "Update an item iff it already exists. Prefer cas! to prevent clobbering
  other updates. Returns a boolean indicating whether the operation succeeded
  in a future."
  ([^CouchbaseClient conn k value ^long expiration] 
   (derefable-future (.add conn (to-key k) expiration value *transcoder*)))
  ([conn k value]
   (add! conn k value 0)))

(defn force!
  "Update the value of an item, creating it if it does not exist. Prefer cas!
  to prevent clobbering other updates. Returns a boolean indicating whether
  the operation succeeded in a future."
  ([^CouchbaseClient conn k value ^long expiration]
   (derefable-future (.set conn (to-key k) expiration value *transcoder*)))
  ([conn k value]
   (force! conn k value 0)))

