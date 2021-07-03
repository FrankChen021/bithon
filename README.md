# What's Bithon

Bithon is a word combining binocular together with python.

It targets application metrics, logging, distributed tracing, alert and application risk governance under micro-service environment.

# Demon

A preview demo is hosted [here](https://www.bithon.cn:9897/ui/home).

# Build

## 1. clone source code

## 2. build & install dependencies

Enter `shaded` directory, and execute the following command to build and install dependencies on your local.

```
mvn clean install 
```

## 3. build project

After step 2, to build the project, run the following command on root directory of project.

```
mvn clean package
```

# Run

Once the project has been built, you could run the project in a standalone mode to evaluate this project.

## 1. Launch collector & web server

To launch server in evaluation mode, execute the following command:

```
java -jar server/server-starter/target/bithon-server-starter.jar
```

By default, the application opens and listens on following ports at local

|Function|Port|
| --- | --- |
| tracing | 9895 |
| event  | 9896 |
| metric | 9898 |
| ctrl | 9899 |
| web | 9897 |

Once the application has started, visit [http://localhost:9897/ui/home](http://localhost:9897/ui/home) to view the monitor.

## 2. Attach agent to your java application

Attach agent to your java agent by adding following VM arguments.

```
-javaagent:<YOUR_PROJECT_DIRECTORY>/agent/dest/agent-main.jar -Dbithon.application.name=<YOUR_APPLICATION_NAME> -Dbithon.application.env=<YOUR_APPLICATION_ENV>
```

|Variable|Description|
| --- | --- |
| YOUR_PROJECT_DIRECTORY | the directory where this project saves |
| YOUR_APPLICATION_NAME  | the name of your application. It could be any string |
| YOUR_APPLICATION_ENV | the name of your environment to label your application. It could be any string. Usually it could be `dev`, `test`, `prd` |

By default, the agent connects collector running at local(127.0.0.1). 
Collector address could be changed in file `agent/agent-main/src/main/resources/agent.yml`.
Make sure to re-build the project after changing the configuration file above.

# Contribution

To develop for this project, intellij is recommended. 

A code style template file(`dev/bithon_intellij_code_style`) must be imported into intellij for coding.

For more information, check [Here](dev/README.md).

# License

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
