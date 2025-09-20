#!/bin/sh

if [ -f /opt/shared/conf/jvm.config ] ; then
  echo "Reading customer jvm configurations..."
  JAVA_OPTS="$(cat /opt/shared/conf/jvm.config | xargs) $JAVA_OPTS"
fi

JAVA_OPTS="-Dbithon.application.name=bithon-server $JAVA_OPTS"

if [ "$INJECT_AGENT" = true ] ; then
  TEMP_SCRIPT=$(mktemp)
  if curl -sSL https://raw.githubusercontent.com/FrankChen021/bithon/refs/heads/master/docker/inject-agent.sh -o "$TEMP_SCRIPT"; then
      # shellcheck disable=SC1090
      if ! . "$TEMP_SCRIPT"; then
          echo "WARNING: Agent injection script failed to execute"
      fi

      rm -f "$TEMP_SCRIPT"
  else
      echo "Agent injection skipped: Failed to download agent injection script"
  fi
else
  echo "Agent injection is NOT enabled. Injection skipped."
fi

# shellcheck disable=SC2086
exec java ${JAVA_OPTS} -jar /opt/bithon-server-starter.jar ${APP_OPTS}
