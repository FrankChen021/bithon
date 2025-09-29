## Test Case

The test cases requires docker to run a mysql5 container.
If colima is used, set these environment variables to your IDE(e.g. IntelliJ) run configuration:

```shell
DOCKER_HOST=unix://$HOME$/.colima/docker.sock
TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=$HOME$/.colima/docker.sock"
TESTCONTAINERS_RYUK_DISABLED=true
```