# What's Bithon

Bithon is a word combining binocular together with python.

It targets application metrics, logging, distributed tracing, alert and application risk governance under micro-service environment.

# Demon

A preview demo is hosted [here](https://www.bithon.org:9897/web/home).

# Build

## 1. clone source code

## 2. choose a right JDK

It's highly recommended that JDK that is used to compile bithon is the same as the JRE that runs your applications.
See [JDKs Compatibility](#jdks-compatibility) for more information.

So you have to choose a right JDK to compile bithon. If you have multiple JDKs on your machine, use `export JAVA_HOME={YOUR_JDK_HOME}` command to set correct JDK.

For example

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/openjdk-8.jdk/Contents/Home
```


## 3. build & install dependencies

There some dependencies that are needed to be built out of the main project for the sake of faster build speed of the main project.

1. Enter `shaded` directory, and execute the following command to build and install dependencies on your local.

    ```bash
    cd shaded
    mvn clean install 
    ```
   
2. Build jOOQ. Execute following commands at the root module

    ```bash
    git submodule sync
    git submodule update --init
    cd server/jOOQ
    mvn clean install 
    ```


## 4. build project

After step 2, to build the project, run the following command on root directory of project.

```bash
mvn clean package
```

# Run

Once the project has been built, you could run the project in a standalone mode to evaluate this project.

## 1. Launch collector & web server

To launch server in evaluation mode, execute the following command:

```bash
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

Once the application has started, visit [http://localhost:9897/web/home](http://localhost:9897/web/home) to view the monitor.

## 2. Attach agent to your java application

Attach agent to your java agent by adding following VM arguments.

```bash
-javaagent:<YOUR_PROJECT_DIRECTORY>/agent/agent-distribution/target/agent-distribution/agent-main.jar -Dbithon.application.name=<YOUR_APPLICATION_NAME> -Dbithon.application.env=<YOUR_APPLICATION_ENV>
```

For applications running on JRE 9 or above, append following arguments to above command line
```bash
--add-exports java.base/jdk.internal.misc=ALL-UNNAMED
```

|Variable|Description|
| --- | --- |
| YOUR_PROJECT_DIRECTORY | the directory where this project saves |
| YOUR_APPLICATION_NAME  | the name of your application. It could be any string |
| YOUR_APPLICATION_ENV | the name of your environment to label your application. It could be any string. Usually it could be `dev`, `test`, `prd` |

By default, the agent connects collector running at local(127.0.0.1). 
Collector address could be changed in file `agent/agent-main/src/main/resources/agent.yml`.
Make sure to re-build the project after changing the configuration file above.

# JDKs Compatibility

Following matrix lists the JDKs that have been tested on macOS. And in theory, this matrix works both for Windows and Linux.

|JDK| Supported | 
| --- | --- |
| JDK 1.8.0_291 | Yes |
| JDK 9.0.4 | Yes |
| JDK 10.0.2 | Yes |
| JDK 11.0.12 | Yes |
| JDK 12.0.2 | Yes |
| JDK 13.0.2 | Yes |
| JDK 14.0.2 | Yes |
| JDK 15.0.2 | Yes |
| JDK 16.02 | Yes |
| JDK 17 | Yes |

Due to the backward compatibility of JRE, Bithon that is compiled with a specific version of JDK may work for a range of JREs that applications are running on.

For example:

- Bithon + JDK 1.8 works only for JRE 1.8
- Bithon + JDK  9 works for JRE between 9 and 17
- Bithon + JDK 10 works for JRE between 10 and 17
- Bithon + JDK 17 works for JRE 17

# Contribution

To develop for this project, intellij is recommended. 

A code style template file(`dev/bithon_intellij_code_style`) must be imported into intellij for coding.

For more information, check the [development doc](doc/development.md).

# License

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)

# User Doc
1. [Configuration](doc/configuration.md)
2. [Metrics](doc/metrics.md)
