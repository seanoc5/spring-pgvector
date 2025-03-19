#!/bin/bash
# copied from chatgpt -- upload custom configsets
set -e

echo "====================== Starting Solr for upload...? ======================"
# Start Solr in background so we can do config commands
solr start -cloud -p 8983

# Wait for Solr to come online (optional: do a curl health check loop)
sleep 10

# Check if config is present in ZK
CONFIG_EXISTS=$(solr zk ls /configs | grep contracting || true)
if [ -z "$CONFIG_EXISTS" ]; then
  echo "====================== Uploading 'contracting' config to ZK"
  solr zk upconfig -n contracting -d /opt/solr/server/solr/configsets/contracting
  echo "------------------ CREATE CONTRACTING Collection ------------------"
  solr create -c contracting -n contracting
fi

echo "Bring Solr to foreground so container doesn't exit..."
solr-foreground
