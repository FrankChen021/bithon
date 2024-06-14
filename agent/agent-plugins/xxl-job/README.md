# Rationale

This plugin instruments the xxl-job and has been tested with the release 2.3.0.

In xxl-job, the `com.xxl.job.core.server.EmbedServer` is responsible for receiving remote requests from xxl-job admin.
This class internally uses `Netty` as the underlying network library to serve HTTP requests.

So,
a hook on the `process` method of xxl-job class `EmbedHttpServerHandler` class is set up
to initializing tracing context for each request from the xxl-job admin.

The tricky part in xxl-job is that, for each job execution request,
it uses a customized thread and queue to save the request,
and this request is polled in another thread from this queue to schedule a corresponding job.

To correctly keep and restore tracing context from the customized queue,
the constructor of the xxl-job class `JobThread` is intercepted.

The interceptor replaces the internal queue by our overridden queue,
which saves the tracing context when an item is pushed to the queue,
restores the tracing context on the calling thread when an item is polled from the queue.

When the execution comes to the `IJobHandler`, namely `GlueJobHandler`,
`MethodJobHandler`, `ScriptJobHandler`, we will restore the tracing context from the calling thread.


