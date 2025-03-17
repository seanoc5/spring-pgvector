#!/bin/bash
set -e

# -------------------------------------------------------------------
# 1) Start Solr in the background, in cloud mode, pointing to ZK
# -------------------------------------------------------------------
echo "Starting Solr in cloud mode..."
/docker-entrypoint.sh solr -cloud -z "${ZK_HOST}" &
SOLR_PID=$!

# -------------------------------------------------------------------
# 2) Wait for Solr to come up
# -------------------------------------------------------------------
echo "Waiting for Solr to be available on port 8983..."
# Just a simple loop that tries to curl Solr until it's up
until curl -s "http://localhost:8983/solr/admin/info/system" > /dev/null; do
  sleep 3
done
echo "Solr is up."

# -------------------------------------------------------------------
# 3) Upload config set to ZooKeeper if not already uploaded
# -------------------------------------------------------------------
CONFIG_NAME="contracting"
CONFIG_PATH="/opt/solr/server/solr/configsets/${CONFIG_NAME}/conf"

echo "Checking if config '${CONFIG_NAME}' exists in ZooKeeper..."
# We'll parse the output of 'solr zk ls' to see if the config is uploaded
if /opt/solr/bin/solr zk ls /configs -z "${ZK_HOST}" 2>&1 \
   | grep -q "${CONFIG_NAME}"; then
  echo "Config '${CONFIG_NAME}' already present in ZooKeeper. Skipping upload."
else
  echo "Uploading config '${CONFIG_NAME}' to ZooKeeper..."
  /opt/solr/bin/solr zk upconfig \
    -n "${CONFIG_NAME}" \
    -d "${CONFIG_PATH}" \
    -z "${ZK_HOST}"
  echo "Config uploaded."
fi

# -------------------------------------------------------------------
# 4) Check if the 'contracting' collection exists, and create if not
# -------------------------------------------------------------------
COLLECTION_NAME="contracting"

echo "Checking if collection '${COLLECTION_NAME}' exists..."
LIST_OUTPUT="$(curl -s "http://localhost:8983/solr/admin/collections?action=LIST")"
if echo "${LIST_OUTPUT}" | grep -q "\"${COLLECTION_NAME}\""; then
  echo "Collection '${COLLECTION_NAME}' already exists. Skipping creation."
else
  echo "Creating collection '${COLLECTION_NAME}' with config '${CONFIG_NAME}'..."
  /opt/solr/bin/solr create \
    -c "${COLLECTION_NAME}" \
    -n "${CONFIG_NAME}" \
    -shards 1 \
    -replicationFactor 1
  echo "Collection created."
fi

# -------------------------------------------------------------------
# 5) Bring the Solr process (foreground) to the foreground of this script
# -------------------------------------------------------------------
wait "${SOLR_PID}"
