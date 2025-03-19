# first draft dockerfile to include custom configet: contracting
FROM solr:9.8
# Copy in custom config
#COPY solr-config/contracting  /opt/solr/server/solr/configsets/contracting
COPY solr-config/contracts  /opt/solr/server/solr/configsets/contracts

# Copy initialization script to the proper location
USER root
COPY scripts/solr-init.sh /docker-entrypoint-initdb.d/solr-init.sh
RUN chmod +x /docker-entrypoint-initdb.d/solr-init.sh && \
    chown solr:solr /docker-entrypoint-initdb.d/solr-init.sh && \
    mkdir -p /var/solr/data && \
    chown -R solr:solr /var/solr/data

USER solr

# Use the default entrypoint and CMD from the Solr image
#CMD ["solr-foreground"]
