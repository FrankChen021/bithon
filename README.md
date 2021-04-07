# What's Bithon

Bithon is a word combining binocular together with python.

It targets application metrics, logging, distributed tracing, alert and application risk governance under micro-service environment.

# Build

## 1. install dependencies

Enter `agent/agent-dependencies` directory, and execute the following command to install agent-dependencies on local.

```
mvn install 
```

## 2. build project

After step 1, to build the project, run the following command on root directory of project.

```
mvn clean package
```

# Run

Once the project has been built, you could run the project in a standalone mode to evaluate this project.

## 1. Launch collector

```
java -jar collector/target/collector-server.jar
```

By default, the application opens and listens on following ports at local

|Function|Port|
| --- | --- |
| tracing | 9895 |
| event  | 9896 |
| metric | 9898 |
| setting | 9899 |

## 2. Launch web server
```
java -jar web-server/target/web-server.jar
```

By default, web-server opens and listens on 9999 at local.
Once web-server has started, visit [http://localhost:9999/ui/home](http://localhost:9999/ui/home) to view the monitor.

## 3. Attach agent to your java application

Attach agent to your java agent by adding following VM arguments.

```
-javaagent:<YOUR_PROJECT_DIRECTORY>/agent/dest/agent-bootstrap.jar -Dbithon.application.name=<YOUR_APPLICATION_NAME> -Dbithon.application.env=<YOUR_APPLICATION_ENV>
```

|Variable|Description|
| --- | --- |
| YOUR_PROJECT_DIRECTORY | the directory where this project saves |
| YOUR_APPLICATION_NAME  | the name of your application. It could be any string |
| YOUR_APPLICATION_ENV | the name of your environment to label your application. It could be any string. Usually it could be `dev`, `test`, `prd` |

By default, the agent connects to collector running at local(127.0.0.1). 
Collector address could be changed in file `agent/agent-bootstrap/src/main/resources/agent.yml`.
Make sure to re-build the project after changing the configuration file above.
