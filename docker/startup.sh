#!/bin/sh

echo "Downloading agent from ${AGENT_URI}"
wget -T 10 -O agent.zip "${AGENT_URI}"

if [ -f agent.zip ] ; then
  echo "Cleaning up agent..."
  rm -fr /opt/agent-distribution

  echo "Unzip agent compress file..."
  unzip agent.zip
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

exec java ${JAVA_OPTS} -jar /opt/bithon-server-starter.jar ${APP_OPTS}
