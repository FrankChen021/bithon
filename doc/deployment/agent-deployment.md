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

We have already shipped the agent injection script which does the above things automatically for you, what you need to do is just 
copy-paste the following if-else script into your start up script.

For example:

```bash
#!/bin/sh

# COPY the entire if-else block of the following before the command where your application starts
if [ "$INJECT_AGENT" = true ] ; then
  TEMP_SCRIPT=$(mktemp)
  if curl -sSL https://raw.githubusercontent.com/FrankChen021/bithon/refs/heads/master/docker/inject-agent.sh -o "$TEMP_SCRIPT"; then
      if source "$TEMP_SCRIPT"; then
          echo "Agent injection script executed successfully"
      else
          echo "WARNING: Agent injection script failed to execute"
      fi
      rm -f "$TEMP_SCRIPT"
  else
      echo "Agent injection skipped: Failed to download agent injection script"
  fi
else
  echo "Agent injection is NOT enabled. Injection skipped."
fi

# Assume this is the command line that starts your target application
exec java -jar YOUR_JAVA_APPLICATION.jar
```

# Docker Deployment

This section describes how to deploy the agent to your applications running in a Docker container.

> Note:
> 
> If your Java application is running in a Kubernetes cluster, you can skip this section and refer to the Kubernetes Deployment section below.

## Add a startup script to your Docker image
This is similar to the standalone deployment, we can use above script to do the similar thing in the Docker container.

```bash
#!/bin/sh

# COPY the entire if-else block of the following before the command where your application starts
if [ "$INJECT_AGENT" = true ] ; then
  TEMP_SCRIPT=$(mktemp)
  if curl -sSL https://raw.githubusercontent.com/FrankChen021/bithon/refs/heads/master/docker/inject-agent.sh -o "$TEMP_SCRIPT"; then
      if source "$TEMP_SCRIPT"; then
          echo "Agent injection script executed successfully"
      else
          echo "WARNING: Agent injection script failed to execute"
      fi
      rm -f "$TEMP_SCRIPT"
  else
      echo "Agent injection skipped: Failed to download agent injection script"
  fi
else
  echo "Agent injection is NOT enabled. Injection skipped."
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

or you can add the script to your docker file:


```dockerfile
COPY startup.sh /startup.sh
RUN chmod +x /startup.sh 
 
ENV JAVA_TOOL_OPTIONS ""
ENV WITH_AGENT=true

# Remember to change this URL to your own agent distribution URL
ENV AGENT_URI https://github.com/FrankChen021/bithon/releases/download/agent-distribution-latest/agent-distribution.tar

# Add the inject-agent.sh script into docker file
ADD https://raw.githubusercontent.com/FrankChen021/bithon/main/docker/inject-agent.sh /tmp/inject-agent.sh

WORKDIR /opt
 
# Replace the ENTRYPOINT to the startup.sh
ENTRYPOINT ["/startup.sh"]
```

```bash
# Run the script to inject agent
source /tmp/inject-agent.sh

# Assume this is the command that starts your application
exec java -jar YOUR_JAVA_APPLICATION.jar
```


## Start the Docker container

You can start the Docker container with the following command:

```bash
docker run -d \
  -e bithon_application_name=YOUR_APPLICATION_NAME -e bithon_application_env=YOUR_APPLICATION_ENV \
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
          # But BE CAREFUL that this imposes a strong dependency to the image registry service. If it's down, the container is not able to start
          imagePullPolicy: Always
          command: ["sh", "-c", "cp -r /opt/agent-distribution/* /opt/bithon/agent"]
          volumeMounts:
          - mountPath: /opt/bithon/agent
            name: bithon-agent
          resources:
            requests:
              cpu: 500m
              memory: 64Mi
            limits:
              cpu: 500m
              memory: 64Mi
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
            # value: "-javaagent:/opt/bithon/agent/agent-main.jar"
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
