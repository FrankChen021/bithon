#!/bin/sh

echo "Downloading agent compressed file..."
wget -T 10 "${AGENT_URI}"

if [ -f agent-distribution-1.1.0-RELEASE.zip ] ; then
  echo "Cleaning up agent..."
  rm -fr /opt/agent-distribution

  echo "Unzip agent compress file..."
  unzip agent-distribution-1.1.0-RELEASE.zip
else
  echo "Failed to downloading agent..."
fi

if [ -f /opt/agent-distribution/agent-main.jar ] ; then
  echo "Starting application with agent..."
  exec java -javaagent:/opt/agent-distribution/agent-main.jar "${JAVA_OPTS}" -Dbithon.application.name=bithon-server -Dbithon.application.env=dev -jar /opt/bithon-server-starter.jar
else
  echo "Starting application WITHOUT agent..."
  exec java "${JAVA_OPTS}" -Dbithon.application.name=bithon-server -Dbithon.application.env=dev -jar /opt/bithon-server-starter.jar
fi
