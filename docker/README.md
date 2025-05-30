<!--
  ~
  ~ Copyright 2020 bithon.org
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  ~
  -->

## Build

From the root of the repo, run 

```bash
DOCKER_BUILDKIT=1 docker build -t bithon/server:{TAG} -f docker/Dockerfile-server .
```

> NOTE:
> 
> The above command compiles all necessary dependencies for the server, including the shaded libraries and jOOQ.
> In most cases, these two dependencies do not change, and compiling them takes more time.
> To save building time, we can compile the module out of docker, and then use the following command to speed up.
> 

```bash
DOCKER_BUILDKIT=1 docker build -t bithon/server:{TAG} -f docker/Dockerfile-server --build-arg BUILD_FROM_SOURCE=false .
```

## Run

- Run with JVM arguments.
    ```bash
    docker run -p 9895:9895 -p 9896:9896 -p 9897:9897 -p 9898:9898 -p 9899:9899 -e JAVA_OPTS="-Xmx4g -Dbithon.application.env=test -Dspring.profiles.active=all-in-one" -itd bithon/server:{TAG} 
    ```

- Run with extra JVM arguments in a file.

    Say there's file named as `jvm.config` which is located at `/usr/local/software/bithon/config`. 
    It contains some JVM arguments in multiple lines which will be passed to the application running in docker.

    ```bash
    -Dbithon.application.env=test
    -Dspring.profiles.active=all-in-one
    -Xms=2g
    -Xmx=4G
    ```
    We can use the following command to mount a local directory that contains the above file and run the docker.
    ```bash
    docker run -p 9895:9895 -p 9896:9896 -p 9897:9897 -p 9898:9898 -p 9899:9899 -v /usr/local/software/bithon/config:/opt/shared/conf -itd bithon/server:{TAG} 
    ```
  
- Run with customized agent download URL.
  
    By default, the process inside the docker will first download agent from [https://repo1.maven.org/](https://repo1.maven.org/).
    If this site is not available for you, you can change this site by specifying an extra environment argument when starting the docker as follows 
    ```bash
    docker run -p 9895:9895 -p 9896:9896 -p 9897:9897 -p 9898:9898 -p 9899:9899 -e AGENT_URI="YOUR_AGENT_URI" -e JAVA_OPTS="-Dbithon.application.env=test -Dspring.profiles.active=all-in-one" -itd bithon/server:{TAG} 
    ```
  
Once the docker runs, you can visit http://localhost:9897 to see the UI and the server itself is being monitored.

> NOTE:
> The above command launches the server in default mode for a show case only.
> In production deployment, you should properly configure the server.
> You can take a look at [example configuration files](../server/server-starter/src/main/resources) for reference.

## Run from docker-compose

This docker-compose.yml file integrates the backend service and front service as well as ClickHouse as storage.
You can run the following command to start the whole system.

```bash
docker-compose -f docker/docker-compose.yml up
```

Once all services in the docker-compose are up, you can visit http://localhost:9900 to access UI.
And by default, the application itself is configured to be self-monitored, you will see the metrics/tracing of the application itself.

> NOTE
> 1. The docker-compose.yml file pulls images from the DockerHub by default. You can change the image name to the one you built locally.
> 2. http://localhost:9900 is the address of the front service configured in the docker-compose.yml file if you don't make any changes to the file.
> You can customize the port number by your need.

## Integrate the agent in your image

### Package the agent in your image

This is not the recommended way because once the agent changes,
you have to re-build the images of your applications to get the agent updated.
But you still can try this way.

In your Dockerfile, include the following lines:

```
FROM bithon/agent:latest as agent
COPY --from=agent /opt/agent-distribution /opt/agent-distribution
```

And modify the entrypoint of your java application to use the agent:
```
java --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED \
     --add-opens=java.base/sun.net.www=ALL-UNNAMED \
     --add-opens=java.base/java.net=ALL-UNNAMED \
     -javaagent:/opt/agent-distribution/agent-main.jar \
     -Dbithon.application.name={YOUR_APP_NAME} \
     -Dbithon.application.env={YOUR_ENV_NAME}
```

### Download the agent before the application starts

You can take a reference of the startup.sh in the repo to download the agent from a distribution URL.