#!/bin/bash
#set -e

echo "====================== Starting Solr for upload and config...? ======================"
# Start Solr in background so we can do config commands
solr start -cloud -p 8983

COLLECTION_NAME="sanity-check"
CSV_FILE_PATH="/tmp/test-data.csv"

# -------------------------------------------------------------------
# Upload config set to ZooKeeper if not already uploaded
# -------------------------------------------------------------------
CONFIG_NAME="${COLLECTION_NAME}"
CONFIG_PATH="/opt/solr/server/solr/configsets/${CONFIG_NAME}/conf"

echo "====================== Checking if config '${CONFIG_NAME}' exists in ZooKeeper..."
# We'll parse the output of 'solr zk ls' to see if the config is uploaded
#if solr zk ls /configs -z "${ZK_HOST}" 2>&1 | grep -q "${CONFIG_NAME}"; then
#  echo "====================== Config '${CONFIG_NAME}' already present in ZooKeeper. Skipping upload."
#else
  echo "====================== Uploading config '${CONFIG_NAME}' to ZooKeeper..."
  solr zk upconfig \
    -n "${CONFIG_NAME}" \
    -d "${CONFIG_PATH}" \
    -z "${ZK_HOST}"
  echo "====================== Config (${CONFIG_NAME}) uploaded."
#fi

#sleep 5
# Wait for Solr to be available
echo "====================== Waiting for Solr to be available on port 8983 (collection: ${COLLECTION_NAME})..."
until curl -s "http://localhost:8983/solr/admin/info/system" > /dev/null; do
  sleep 3
  echo "           Still waiting for Solr..."
done
echo "====================== Solr is up!!"


# -------------------------------------------------------------------
# Check if the 'contracting' collection exists, and create if not
# -------------------------------------------------------------------
echo "Checking if collection '${COLLECTION_NAME}' exists..."
LIST_OUTPUT="$(curl -s "http://localhost:8983/solr/admin/collections?action=LIST")"
if echo "${LIST_OUTPUT}" | grep -q "\"${COLLECTION_NAME}\""; then
  echo "++++++++++++++++++++++ Collection '${COLLECTION_NAME}' already exists. Skipping creation."
else
  echo "===================== Creating collection '${COLLECTION_NAME}' with config '${CONFIG_NAME}'..."
  solr create \
    -c "${COLLECTION_NAME}" \
    -n "${CONFIG_NAME}" \
    -z "${ZK_HOST}"

  echo "...................... Collection created."
fi

echo "===================== POST sample data '${COLLECTION_NAME}' csv file:(${CSV_FILE_PATH})..."
solr post -c "${COLLECTION_NAME}" "${CSV_FILE_PATH}"

# todo -- make this better, more dockerified
echo "---------------------- Explicitly Stopping Solr (is there a better way?)..."
solr stop

#echo "Solr initialization completed successfully."
#echo "Bring Solr to foreground so container doesn't exit..."
#echo "should exit by default...?"
#solr-foreground

