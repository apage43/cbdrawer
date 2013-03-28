# cbdrawer

A utility belt for using [Couchbase Server](http://couchbase.com/) with Clojure.

```clojure
[apage43/cbdrawer "0.2.1"]
```

This is not currently a complete wrapping of all of the client functionality, just the parts I've needed so far.

 * Wraps some of the operations that return a java.util.concurrent.Future so that they implement IDeref, and can be used with deref/@
 * Includes spymemcached Transcoders for serializing and deserializing:
    * Clojure/[EDN](http://edn-format.org) strings, can represent clojure datastructures and records without loss of information. These work with the default transcoder, but this is safer, readable, and much smaller than the Java serialization.
    * JSON strings, for use with Couchbase views
    * [JSON SMILE](http://wiki.fasterxml.com/SmileFormat) a compact binary format that is equivalent to JSON. Smaller than JSON, but won't work in views.
 * Ops take keyword keys as well as strings
 * Includes a cas! function in the style of swap! and friends. `(cas! conn :key-of-some-list conj :newitem)`

For all functionality, see the [API docs](http://apage43.github.com/cbdrawer/).

