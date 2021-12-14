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

For the 2nd time to build the docker file, build argument `BUILD_FROM_SOURCE` could be added to the command to speed up the build process.
```bash
DOCKER_BUILDKIT=1 docker build -t bithon/server:{TAG} -f docker/Dockerfile-server --build-arg BUILD_FROM_SOURCE=false .
```

## Run

- Normal run.
    ```bash
    docker run -p 9895:9895 -p 9896:9896 -p 9897:9897 -p 9898:9898 -p 9899:9899 -itd bithon/server:{TAG} 
    ```

- Run with extra JVM arguments.
    ```bash
    docker run -p 9895:9895 -p 9896:9896 -p 9897:9897 -p 9898:9898 -p 9899:9899 -e JAVA_OPTS="-Xmx4g" -itd bithon/server:{TAG} 
    ```

- Run with customized agent download URL.
  
    By default, the process inside the docker will first download agent from [https://repo1.maven.org/](https://repo1.maven.org/).
    If this site is not available for you, you can change this site by specifying an extra environment argument when starting the docker as follows 
    ```bash
    docker run -p 9895:9895 -p 9896:9896 -p 9897:9897 -p 9898:9898 -p 9899:9899 -e AGENT_URI="YOUR_AGENT_URI" -itd bithon/server:{TAG} 
    ```
