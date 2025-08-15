This plugin instrument Netty's ResourceLeakDector to report any resource leak events inside Netty.

When such event is detected, the plugin creates an Exception message and send the exception information to remote servers.
And these events can be seen from the `exception-metrics` related dashboard.

Here's an example of how the report event looks like

```text
LEAK: ByteBuf.release() was not called before it's garbage-collected. See https://netty.io/wiki/reference-counted-objects.html for more information.
Recent access records: 
Created at:
	io.netty.buffer.UnpooledByteBufAllocator.newDirectBuffer(UnpooledByteBufAllocator.java:96)
	io.netty.buffer.AbstractByteBufAllocator.directBuffer(AbstractByteBufAllocator.java:188)
	io.netty.buffer.AbstractByteBufAllocator.directBuffer(AbstractByteBufAllocator.java:179)
	io.netty.channel.unix.PreferredDirectByteBufAllocator.ioBuffer(PreferredDirectByteBufAllocator.java:53)
	io.netty.channel.DefaultMaxMessagesRecvByteBufAllocator$MaxMessageHandle.allocate(DefaultMaxMessagesRecvByteBufAllocator.java:114)
	io.netty.channel.epoll.EpollRecvByteAllocatorHandle.allocate(EpollRecvByteAllocatorHandle.java:75)
	io.netty.channel.epoll.AbstractEpollStreamChannel$EpollStreamUnsafe.epollInReady(AbstractEpollStreamChannel.java:780)
	io.netty.channel.epoll.EpollEventLoop.processReady(EpollEventLoop.java:480)
	io.netty.channel.epoll.EpollEventLoop.run(EpollEventLoop.java:378)
	io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:986)
	io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
	io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
	java.base/java.lang.Thread.run(Thread.java:829)
```