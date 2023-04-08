
Bithon provides a SQL-based command to debug/profile an application in interactive way.

All commands are sent to a specific application instance immediately to perform the query or command.
These commands are not persistent in the backend of Bithon, this means once the application instance restarts,
previous commands went away.

Following list describes the features that the Bithon supports now.
1. query instances
2. query running threads(including call stack)
3. query logger level
4. update logger level
5. query instrumented methods

## Query instances

### SQL

```sql
SELECT * FROM agent.instance
```

### Output example

| appName        | appId               | endpoint        | agentVersion                                                                        |
|----------------|---------------------|-----------------|-------------------------------------------------------------------------------------|
| bithon-web-dev | 192.168.50.151:9897 | 127.0.0.1:55000 | 1.0-SNAPSHOT@ab9fca6d44b6f19fc751b383a3ebe3aa44cbddb1@2023-04-08T13:38:16.532+08:00 |


### Field Explanation

| Field        | Explanation                                                                                                         |
|--------------|---------------------------------------------------------------------------------------------------------------------|
| appName      | The application name of an target application, also the value of `-Dbithon.application.name` JVM parameter.         |
| appId        | The instance id of target application.                                                                              |
| endpoint     | The real endpoint of the target application that connects to the Bithon collector                                   |
| agentVersion | The version of the agent that the target application loads. It's in the format as: <VERSION>@<CommitId>@<Timestamp> |


## Query running threads(including call stack)

### SQL

```sql
SELECT * FROM agent.thread WHERE appId = '<THE TARGET APPLICATION ID>'
```

### Field Explanation

| Field         | Explanation                                                                                                                                                                                                                                                                   |
|---------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| threadId      | Id of a running thread                                                                                                                                                                                                                                                        |
| name          | Name of a running thread                                                                                                                                                                                                                                                      |
| daemon        | Whether the thread is running as a daemon thread                                                                                                                                                                                                                              |
| priority      | The priority of a thread                                                                                                                                                                                                                                                      |
| state         | State of a running thread                                                                                                                                                                                                                                                     |
| cpuTime       | The total CPU time in nanoseconds for a thread if CPU time measurement is enabled; -1 otherwise.<br/>Whether the CPU time measurement is enabled is determined by the JVM implementation. See `ManagementFactory.getThreadMXBean().isThreadCpuTimeSupported()` for more info. |
| userTime      | The user-level CPU time for a thread if CPU time measurement is enabled; -1 otherwise.                                                                                                                                                                                        |
| blockedTime   | The approximate accumulated elapsed time in milliseconds that a thread entered the BLOCKED state; -1 if thread contention monitoring is disabled.                                                                                                                             |
| blockedCount  | The total number of times that the thread entered the BLOCKED state.                                                                                                                                                                                                          |
| waitedTime    | The approximate accumulated elapsed time in milliseconds that a thread has been in the WAITING or TIMED_WAITING state; -1 if thread contention monitoring is disabled.                                                                                                        |
| waitedCount   | The total number of times that the thread was in the WAITING or TIMED_WAITING state.                                                                                                                                                                                          |
| lockName      | The string representation of the object on which the thread is blocked if any; null otherwise.                                                                                                                                                                                |
| lockOwnerId   | The thread ID of the owner thread of the object this thread is blocked on; -1 if this thread is not blocked or if the object is not owned by any thread.                                                                                                                      |
| lockOwnerName | The name of the thread that owns the object this thread is blocked on; null if this thread is not blocked or if the object is not owned by any thread.                                                                                                                        |
| inNative      | 1 if the thread is executing native code; 0 otherwise.                                                                                                                                                                                                                        |
| suspended     | 1 if the thread is suspended; 0 otherwise.                                                                                                                                                                                                                                    |
| stack         | The stack trace of one thread.                                                                                                                                                                                                                                                |

## Query logger level

> NOTE: This only works for application based on SpringBoot 1.5 and above.
> 

### SQL

```sql
SELECT * FROM agent.logger WHERE appId = '192.168.50.151:9897'
```

### Output field Explanation

| Field          | Explanation                                                                                                             |
|----------------|-------------------------------------------------------------------------------------------------------------------------|
| name           | The name of a logger.                                                                                                   |
| level          | The configured logging level of corresponding logger. <br/> Can be one of: OFF, TRACE, DEBUG, INFO, WARN, ERROR, FATAL. |
| effectiveLevel | The effective logging level of a logger.                                                                                |

## Set logger level

> NOTE: This only works for application based on SpringBoot 1.5 and above.
>

We can also change the configured logging level during application running by using `UPDATE` statement.

### SQL

```sql
UPDATE agent.logger SET level = 'DEBUG' where appId = '192.168.50.151:9897' AND name = 'org.bithon'
```

> NOTE:
> 1. The SQL must provide `appId` and `name` filter in the `WHERE` clause.
> 2. Only `level` can be UPDATED

## Query instrumented methods

During agent plugin development and agent debugging, we need to know whether our interceptors have been successfully installed.
To do this, we can use the following SQL to check.

### SQL

```sql
SELECT * FROM agent.instrumented_method WHERE appId = '192.168.50.151:9897'
```

### Output fields explanation

| Field      | Explanation                                                                          |
|------------|--------------------------------------------------------------------------------------|
| clazzName  | The class of the target method.                                                      |
| isStatic   | Whether the target method is a static method.                                        |
| returnType | The return type of target method.                                                    |
| methodName | The name of intercepted method. `<ctor>` represents constructor of the target class. |
| parameters | The parameters of intercepted method.                                                |


