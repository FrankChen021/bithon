
# The difference between OpenTelemetry and Bithon

This is another part of comparison series which I really don't like to do.

I don't know when OpenTelemetry emerged, but as the integration of metrics and tracing and logs is a very obvious trend in the distributed tracing domain, 
It also tries to solve the same problem.

From the whole, Opentelemetry is more like specification. It defines a specification for distributed tracing which now goes into W3C. I think it does a good job that there's chance to link two applications using different tracing system together.
Before that we know that there's a zipkin's trace standard, Open Tracing specification, and PinPoint and Skywalking define their own tracing specification on propagation.

Even in Bithon when I re-wrote the trace propagation, I also adopted OpenTelemetry's specification. This means at the distributed tracing part, Bithon is an implementer of the OpenTelemetry specification.
This means Bithon can accept trace context from any applications that are using OpenTelemetry standard, and also propagate the trace context to any other systems that accept OpenTelemetry standard.   

In one of our applications, it successfully propagates trace context to ClickHouse which accepts OpenTelemetry standard.

## Architecture

Similar architecture from agent to backend servers.

## Pros

1. Some de-facto standard such as distributed tracing
2. More clients supported
3. Less impact on startup time of target applications compared to Bithon. See the [Agent Benchmark](../../benchmark/agent/agent-benchmark.md) to get known the impact of Bithon.

## Cons

1. It seems like that opentelemtry's work on Java's agent is still on tracing feature. It supports lots of client libraries, all about tracing feature.
It does define it's onw metric API, but no metrics are integrated in its agent.

2. The design of agent class loader has some flaws, some target application can't start after the agent is attached due to class definition conflict by its class loader.  

   The class loader is the trickiest part in the agent. Bithon does not have such problems because of well-designed class loader and our experience on the agent. 

```text
java.lang.LinkageError: loader (instance of  sun/misc/Launcher$AppClassLoader): attempted  duplicate class definition for name: "org/jboss/netty/channel/ChannelFutureListener"
        at java.lang.ClassLoader.defineClass1(Native Method) ~[?:1.8.0_291]
        at java.lang.ClassLoader.defineClass(ClassLoader.java:756) ~[?:1.8.0_291]
        at java.security.SecureClassLoader.defineClass(SecureClassLoader.java:142) ~[?:1.8.0_291]
        at java.net.URLClassLoader.defineClass(URLClassLoader.java:468) ~[?:1.8.0_291]
        at java.net.URLClassLoader.access$100(URLClassLoader.java:74) ~[?:1.8.0_291]
        at java.net.URLClassLoader$1.run(URLClassLoader.java:369) ~[?:1.8.0_291]
        at java.net.URLClassLoader$1.run(URLClassLoader.java:363) ~[?:1.8.0_291]
        at java.security.AccessController.doPrivileged(Native Method) ~[?:1.8.0_291]
        at java.net.URLClassLoader.findClass(URLClassLoader.java:362) ~[?:1.8.0_291]
        at java.lang.ClassLoader.loadClass(ClassLoader.java:418) ~[?:1.8.0_291]
        at sun.misc.Launcher$AppClassLoader.loadClass(Launcher.java:355) ~[?:1.8.0_291]
        at java.lang.ClassLoader.loadClass(ClassLoader.java:351) ~[?:1.8.0_291]
        at org.apache.druid.java.util.http.client.HttpClientInit.createClient(HttpClientInit.java:81) ~[druid-core-0.23.0-SNAPSHOT.jar:0.23.0-SNAPSHOT]
        at org.apache.druid.guice.http.HttpClientModule$HttpClientProvider.get(HttpClientModule.java:121) ~[druid-server-0.23.0-SNAPSHOT.jar:0.23.0-SNAPSHOT]
        at org.apache.druid.guice.http.HttpClientModule$HttpClientProvider.get(HttpClientModule.java:83) ~[druid-server-0.23.0-SNAPSHOT.jar:0.23.0-SNAPSHOT]
        at com.google.inject.internal.ProviderInternalFactory.provision(ProviderInternalFactory.java:81) ~[guice-4.1.0.jar:?]
        at com.google.inject.internal.InternalFactoryToInitializableAdapter.provision(InternalFactoryToInitializableAdapter.java:53) ~[guice-4.1.0.jar:?]
        at com.google.inject.internal.ProviderInternalFactory.circularGet(ProviderInternalFactory.java:61) ~[guice-4.1.0.jar:?]
        at com.google.inject.internal.InternalFactoryToInitializableAdapter.get(InternalFactoryToInitializableAdapter.java:45) ~[guice-4.1.0.jar:?]
        at com.google.inject.internal.ProviderToInternalFactoryAdapter$1.call(ProviderToInternalFactoryAdapter.java:46) ~[guice-4.1.0.jar:?]
        at com.google.inject.internal.InjectorImpl.callInContext(InjectorImpl.java:1092) ~[guice-4.1.0.jar:?]
        at com.google.inject.internal.ProviderToInternalFactoryAdapter.get(ProviderToInternalFactoryAdapter.java:40) ~[guice-4.1.0.jar:?]
        at com.google.inject.internal.SingletonScope$1.get(SingletonScope.java:194) ~[guice-4.1.0.jar:?]
        at com.google.inject.internal.InternalFactoryToProviderAdapter.get(InternalFactoryToProviderAdapter.java:41) ~[guice-4.1.0.jar:?]
        at com.google.inject.internal.SingleParameterInjector.inject(SingleParameterInjector.java:38) ~[guice-4.1.0.jar:?]
        at com.google.inject.internal.SingleParameterInjector.getAll(SingleParameterInjector.java:62) ~[guice-4.1.0.jar:?]
        at com.google.inject.internal.ConstructorInjector.provision(ConstructorInjector.java:110) ~[guice-4.1.0.jar:?]
        at com.google.inject.internal.ConstructorInjector.construct(ConstructorInjector.java:90) ~[guice-4.1.0.jar:?]
        at com.google.inject.internal.ConstructorBindingImpl$Factory.get(ConstructorBindingImpl.java:268) ~[guice-4.1.0.jar:?]
        at com.google.inject.internal.ProviderToInternalFactoryAdapter$1.call(ProviderToInternalFactoryAdapter.java:46) ~[guice-4.1.0.jar:?]
        at com.google.inject.internal.InjectorImpl.callInContext(InjectorImpl.java:1092) ~[guice-4.1.0.jar:?]
        at com.google.inject.internal.ProviderToInternalFactoryAdapter.get(ProviderToInternalFactoryAdapter.java:40) ~[guice-4.1.0.jar:?]
        at org.apache.druid.guice.LifecycleScope$1.get(LifecycleScope.java:68) ~[druid-core-0.23.0-SNAPSHOT.jar:0.23.0-SNAPSHOT]
        at com.google.inject.internal.InternalFactoryToProviderAdapter.get(InternalFactoryToProviderAdapter.java:41) ~[guice-4.1.0.jar:?]
        at com.google.inject.internal.InjectorImpl$2$1.call(InjectorImpl.java:1019) ~[guice-4.1.0.jar:?]
        at com.google.inject.internal.InjectorImpl.callInContext(InjectorImpl.java:1085) ~[guice-4.1.0.jar:?]
        at com.google.inject.internal.InjectorImpl$2.get(InjectorImpl.java:1015) ~[guice-4.1.0.jar:?]
        at com.google.inject.internal.InjectorImpl.getInstance(InjectorImpl.java:1050) ~[guice-4.1.0.jar:?]
        at org.apache.druid.guice.LifecycleModule$2.start(LifecycleModule.java:150) ~[druid-core-0.23.0-SNAPSHOT.jar:0.23.0-SNAPSHOT]
        at org.apache.druid.cli.GuiceRunnable.initLifecycle(GuiceRunnable.java:165) [druid-services-0.23.0-SNAPSHOT.jar:0.23.0-SNAPSHOT]
        at org.apache.druid.cli.ServerRunnable.run(ServerRunnable.java:63) [druid-services-0.23.0-SNAPSHOT.jar:0.23.0-SNAPSHOT]
        at org.apache.druid.cli.Main.main(Main.java:113) [druid-services-0.23.0-SNAPSHOT.jar:0.23.0-SNAPSHOT]
2022-01-29T10:29:47,529 DEBUG [Thread-4] org.apache.hadoop.util.ShutdownHookManager - ShutdownHookManger complete shutdown.
```