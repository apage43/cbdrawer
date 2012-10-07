# cbdrawer

A junk drawer of stuff for using [Couchbase Server](http://couchbase.com/) with Clojure.

This is not currently a complete wrapping of all of the client functionality, just the parts I've needed so far.

 * Wraps some of the operations that return a java.util.concurrent.Future so that they implement IDeref, and can be used with deref/@
 * Includes spymemcached Transcoders that serialize and deserialize Clojure/[EDN](http://edn-format.org) strings, and JSON strings.

[API docs](http://apage43.github.com/cbdrawer/doc/index.html)
