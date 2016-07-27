# Play File Watch Library

This is the Play File Watch library. It can be used to watch files in a platform independent way. It uses the JDK7 FileWatchService on platforms that provide an asynchronous file watch service (notably OSX doesn't, it uses polling), falling back to JNotify if the platform supports it (eg OSX), and then to simple polling.

It does not depend on any particular build tool, but does bring in sbt IO, which is a standalone IO library that was once part of sbt.
