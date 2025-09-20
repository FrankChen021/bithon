#!/bin/sh

# This script downloads and injects the agent automatically.
# It's mainly for the deployment in a standalone server or docker deployment of the target application.
# For K8S deployment, can refer to the doc/deployment/agent-deployment.md

# Set default AGENT_URI if not defined
if [ -z "$AGENT_URI" ] ; then
  AGENT_URI=https://github.com/FrankChen021/bithon/releases/download/agent-distribution-latest/agent-distribution.tar
fi

# Create temporary directory for agent download
TEMP_DIR=$(mktemp -d)
cd "$TEMP_DIR" || return 0

# Attempt to download the agent
echo "Downloading agent from ${AGENT_URI} to ${TEMP_DIR}"
if ! curl -sSL --connect-timeout 5 -o agent.tar "${AGENT_URI}"; then
  echo "Failed to download agent from ${AGENT_URI}. Injection skipped."
  rm -f agent.tar  # Clean up any partial download
  rm -rf "$TEMP_DIR"
  return 0
fi

# Check if agent.tar was successfully created
if [ ! -f agent.tar ] ; then
  echo "Can't find downloaded agent. Injection skipped."
  rm -rf "$TEMP_DIR"
  return 0
fi

# Extract the agent
tar -xf agent.tar

# Check if the agent jar file exists after extraction
AGENT_LOCATION="$TEMP_DIR"/agent-distribution/agent-main.jar
if [ ! -f "$AGENT_LOCATION" ] ; then
  echo "Agent not found(May be corrupted?). Injection skipped."
  cd - > /dev/null
  rm -rf "$TEMP_DIR"
  return 0
fi

export JAVA_TOOL_OPTIONS="-javaagent:$AGENT_LOCATION $JAVA_TOOL_OPTIONS"
echo "Inject agent via JAVA_TOOL_OPTIONS: $JAVA_TOOL_OPTIONS"
