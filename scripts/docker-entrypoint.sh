#!/usr/bin/env bash
set -e

# Optional: Start Solr in the background first if you need Solr to be running
# in order for your init scripts to do things like "solr zk upconfig".
# For example, if you’re in SolrCloud mode you might do something like:
solr start -cloud -p 8983
# Then wait for it to be ready. (If you do that, you’d eventually do a "solr stop"
# before calling solr-foreground, or skip it if you can upload configs
# without Solr fully started.)

# Check if the directory for init scripts exists and is non-empty
if [ -d /docker-solr-initdb.d ] && [ "$(ls -A /docker-solr-initdb.d)" ]; then
  echo "Running scripts in /docker-solr-initdb.d ..."
  for f in /docker-solr-initdb.d/*.sh; do
    if [ -f "$f" ]; then
      echo "Executing $f"
      # We "source" the scripts (.) instead of executing them in a subshell
      # so that the environment persists, but either approach can work.
      . "$f"
    fi
  done
  echo "Finished running init scripts."
fi

# Finally, start Solr in the foreground so that the container doesn’t exit
exec solr-foreground
