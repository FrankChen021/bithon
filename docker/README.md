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
DOCKER_BUILDKIT=1 docker build -t bithon/agent:{AGENT_TAG} -f docker/Dockerfile-agent .
DOCKER_BUILDKIT=1 docker build -t bithon/server:{TAG} -f docker/Dockerfile-server --build-arg AGENT={AGENT_TAG} .
```

For the 2nd time to build the docker file, build argument `BUILD_FROM_SOURCE` could be added to the command to speed up the build process.
```bash
DOCKER_BUILDKIT=1 docker build -t bithon/server:{TAG} -f docker/Dockerfile-server --build-arg AGENT={AGENT_TAG} --build-arg BUILD_FROM_SOURCE=false .
```

## Run

```bash
docker run -itd bithon/server:{TAG}
```
