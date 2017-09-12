# Play File Watch Library

This is the Play File Watch library. It can be used to watch files in a platform independent way. It uses the JDK7 WatchService on platforms that provide an asynchronous file watch service (notably OS X doesn't, it uses polling), falling back to a different implementation for Mac OS X and then to a polling watch service.

The OS X directory watching implementation is provided by https://github.com/gmethvin/directory-watcher. That provides a native Mac OS X implementation of the WatchService and a thin abstraction layer for recursively watching directories.
