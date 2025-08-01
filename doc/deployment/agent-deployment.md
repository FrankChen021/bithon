This document describes how to deploy the agent to your Java applications.

The way to deploy the agent depends on how your Java applications are deployed.
Nowadays, there are 3 ways to deploy your Java applications:

1. **Standalone**: The Java application is deployed as a standalone process on a physical or virtual machine.
2. **Docker**: The Java application is shipped as a Docker image and deployed in a container.
3. **Kubernetes**: The Java application is deployed in a Kubernetes cluster, which is a container orchestration platform.

# Standalone Deployment

For standalone deployment, we need a script to download the agent, and start the target Java application.

The reason we download the agent in the script everytime before starting the Java application is to ensure that
each time the target application loads the latest stable java agent.

Here's a sample script:

```bash
#!/bin/sh

# Remember to change this URL to your own agent distribution URL
AGENT_URI=https://github.com/FrankChen021/bithon/releases/download/agent-distribution-latest/agent-distribution.zip

echo "Downloading agent from ${AGENT_URI}"
wget -T 10 -O agent.zip "${AGENT_URI}"

if [ -f agent.zip ] ; then
echo "Cleaning up agent..."
rm -fr ./agent-distribution

echo "Unzip agent compress file..."
unzip agent.zip
else
echo "Failed to downloading agent..."
fi

JAVA_TOOL_OPTIONS="-Dbithon.application.name=YOU_APPLICATION_NAME -Dbithon.application.env=YOUR_APPLICATION_ENV $JAVA_TOOL_OPTIONS"

# Automatically detect the JRE version and set the JAVA_OPTS accordingly.
JAVA_MAJOR="$(java -version 2>&1 | sed -n -E 's/.* version "([^."-]*).*/\1/p')"
echo "Detected JRE version: ${JAVA_MAJOR}"
if [ "$JAVA_MAJOR" != "" ] && [ "$JAVA_MAJOR" -ge "11" ]
then
  # Disable strong encapsulation for certain packages on Java 11+.
  JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS\
  --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED \
  --add-opens=java.base/sun.net.www=ALL-UNNAMED \
  --add-opens=java.base/java.net=ALL-UNNAMED \
  "
fi

if [ -f /opt/agent-distribution/agent-main.jar ] ; then
  #
  # Assuming the agent is extracted to /opt/agent-distribution/agent-main.jar
  # If not, change the path accordingly. Note that the path must be absolute.
  #
  JAVA_TOOL_OPTIONS="-javaagent:/opt/agent-distribution/agent-main.jar $JAVA_TOOL_OPTIONS"
  echo "Starting application with agent: $JAVA_TOOL_OPTIONS"
fi

exec java -jar YOUR_JAVA_APPLICATION.jar
```

If you already have a script to start your Java application, you can simply integrate above script into your existing script.

# Docker Deployment

This section describes how to deploy the agent to your applications running in a Docker container.

> Note:
> 
> If your Java application is running in a Kubernetes cluster, you should skip this section and refer to the Kubernetes Deployment section below.

## Add a startup script to your Docker image
This is similar to the standalone deployment, we can use above script to do the similar thing in the Docker container.
But the difference is that we set the environment variable outside the script.

```bash
#!/bin/sh

echo "Downloading agent from ${AGENT_URI}"
wget -T 10 -O agent.tar "${AGENT_URI}"

if [ "$WITH_AGENT" = true ] ; then
  echo "Downloading agent from ${AGENT_URI}"
  wget -T 10 -O agent.tar "${AGENT_URI}"
 
  if [ -f agent.tar ] ; then
    echo "Cleaning up agent..."
    rm -fr ./agent-distribution
 
    echo "Unzip agent compress file..."
    tar -xvf agent.tar
  else
    echo "Failed to downloading agent..."
  fi
fi

# Automatically detect the JRE version and set the options accordingly.
JAVA_MAJOR="$(java -version 2>&1 | sed -n -E 's/.* version "([^."-]*).*/\1/p')"
echo "Detected JRE version: ${JAVA_MAJOR}"
if [ "$JAVA_MAJOR" != "" ] && [ "$JAVA_MAJOR" -ge "11" ]
then
  # Disable strong encapsulation for certain packages on Java 11+.
  JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS\
  --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED \
  --add-opens=java.base/sun.net.www=ALL-UNNAMED \
  --add-opens=java.base/java.net=ALL-UNNAMED \
  "
fi

if [ -f /opt/agent-distribution/agent-main.jar ] ; then
  #
  # Assuming the agent is extracted to /opt/agent-distribution/agent-main.jar
  # If not, change the path accordingly. Note that the path must be absolute.
  #
  JAVA_TOOL_OPTIONS="-javaagent:/opt/agent-distribution/agent-main.jar $JAVA_TOOL_OPTIONS"
  echo "Starting application with agent: $JAVA_TOOL_OPTIONS"
fi

exec java -jar YOUR_JAVA_APPLICATION.jar
```

## Update Dockerfile to include the startup script

```dockerfile
COPY startup.sh /startup.sh
RUN chmod +x /startup.sh 
 
ENV JAVA_TOOL_OPTIONS ""
ENV WITH_AGENT=true

# Remember to change this URL to your own agent distribution URL
ENV AGENT_URI https://github.com/FrankChen021/bithon/releases/download/agent-distribution-latest/agent-distribution.tar
 
WORKDIR /opt
 
# Replace the ENTRYPOINT to the startup.sh
ENTRYPOINT ["/startup.sh"]
```

## Start the Docker container

You can start the Docker container with the following command:

```bash
docker run -d \
  -e JAVA_TOOL_OPTIONS="-Dbithon.application.name=YOUR_APPLICATION_NAME -Dbithon.application.env=YOUR_APPLICATION_ENV" \
  your_docker-image:latest
```

# Kubernetes Deployment

Unlike the above deployment that requires us to modify the startup script of the Java application, 
we can use `initContainers` in Kubernetes to download the agent and copy it to a shared volume, which can be mounted by the main application container.

The following YAML demonstrates how to do it:
1. it adds an `initContainer` to download the agent distribution and copy it to a shared volume.
2. it mounts the shared volume to the main application container.
3. it sets the `JAVA_TOOL_OPTIONS` environment variable to enable the agent.
4. it sets the `bithon_application_instance` environment variables to the container name, which is useful for identifying the application instance.
5. You have to change the `bithon_application_name` and `bithon_application_env` environment variables to your own application name and environment.

```yaml
spec:
  template:
    spec:
      #----------------Add the following initContainers--------------
      volumes:
        - emptyDir: {}
          name: bithon-agent
      initContainers:
        - name: agent-init
          #
          # If you have your own agent distribution, change the image below to your own
          #
          image:  bithon/agent:latest
          # It's recommended to use the latest agent image so that every time the containers starts, it downloads the latest agent distribution.
          imagePullPolicy: Always
          command: ["sh", "-c", "cp -r /opt/agent-distribution/* /opt/bithon/agent"]
          volumeMounts:
          - mountPath: /opt/bithon/agent
            name: bithon-agent
          resources:
            requests:
              cpu: 1
              memory: 500Mi
            limits:
              cpu: 1
              memory: 500Mi
      #----------------Added End-------------- 
      containers:
        - name: your-container-name
          image: your-docker-image:latest
          #---------Added the following to your container------------
          volumeMounts:
          - mountPath: /opt/bithon/agent
            name: bithon-agent
          env:
          - name: JAVA_TOOL_OPTIONS
            # For JDK 8,9,10, use the following value  
            value: "-javaagent:/opt/bithon/agent/agent-main.jar"
            # If the target JDK is Java 11 and above, use the following value
            # value: "-javaagent:/opt/bithon/agent/agent-main.jar --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED --add-opens=java.base/sun.net.www=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED"
          - name: bithon_application_name
            value: CHANGE_TO_YOUR_APPLICATION_NAME
          - name: bithon_application_env
            value: CHANGE_TO_YOUR_APPLICATION_NAME
          - name: bithon_application_instance
            valueFrom:
              fieldRef:
                fieldPath: metadata.name
          #-------Added End--------------------------------------------------------
   
```