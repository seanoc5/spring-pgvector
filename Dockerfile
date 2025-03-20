# first draft dockerfile to include custom configet: contracting
FROM solr:9.8
# Copy in custom config
# testing mount solr configset as volume rather than copy for better test/debug cycling
#COPY solr-config/contracts  /opt/solr/server/solr/configsets/contracts

# make a persistent data directory, chown to solr
# todo - is there a better approach?
USER root
RUN    mkdir -p /var/solr/data && \
    chown -R solr:solr /var/solr/data

USER solr

# Use the default entrypoint and CMD from the Solr image
#CMD ["solr-foreground"]
