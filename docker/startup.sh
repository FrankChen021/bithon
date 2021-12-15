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

if [ -f /opt/shared/conf/jvm.config ] ; then
  echo "Reading customer jvm configurations..."
  JAVA_OPTS="$(cat /opt/shared/conf/jvm.config | xargs) $JAVA_OPTS"
fi

JAVA_OPTS="-Dbithon.application.name=bithon-server $JAVA_OPTS"

if [ -f /opt/agent-distribution/agent-main.jar ] ; then
  JAVA_OPTS="-javaagent:/opt/agent-distribution/agent-main.jar $JAVA_OPTS"
  echo "Starting application with agent: $JAVA_OPTS"
else
  echo "Starting application WITHOUT agent: $JAVA_OPTS"
fi

exec java ${JAVA_OPTS} -jar /opt/bithon-server-starter.jar
