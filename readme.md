# Verteilte Systeme, UE - WS 2016/17 184.167 - Lab 1

## Acknowledgement

This program is based on the framework we got for this lab from the Institute of Information Systems. I used the shell as stated in the example from the TUWEL course and the testing framework.

## Multithreading

### Server

The server operates in a single thread with a threadpool that spawns 2 threads (TCP/UDP) on each TCP ingoing connection. The pool is of the type `cachedThreadPool` because the server can handle even large sets of threads.

### Client

The client operates in a single thread with a threadpool that spawns 2 threads (TCP/UPD) on start, listening for incoming streams. The pool is of the type `fixedThreadPool` because there will never be more than 3 threads at the same time. When the client types `!register <IP:Port>` the client program spawns a third thread whereas a other client can connect to it over TCP. Every Thread is closed upon `!exit`, not on `!logout` since the task description says "keep as long open as possible".

## Performance

(+) Can operate a large set of TCP/UDP threads (as long as there are ressources)

(+) Manages unused threads on its own

(-) Race conditions in shell class while testing (-> framework)

(-) Conditional controlling (depending on server response) is not optimal, e.g. waiting for `!ack`

## Credits

A lot of effort was put in this code. Please pardon minor coding bugs and/or formatting issues. Have a ☕️ and [create a new issue](https://github.com/MartinWeise/DSYS-UE1/issues/new).

[Martin Weise](https://github.com/MartinWeise) &copy; 2016