# first draft dockerfile to include custom configet: contracting
FROM solr:9.8
# Copy in custom config
COPY solr-config/contracting  /opt/solr/server/solr/configsets/contracting

# Set entrypoint to your script
COPY scripts/solr-init.sh /solr-init.sh
USER root
RUN chmod +x /solr-init.sh
USER solr

# Create the init directory
RUN mkdir -p /docker-solr-initdb.d

# Use the default entrypoint from the Solr image
ENTRYPOINT ["docker-entrypoint.sh"]

# Run Solr in the foreground
CMD ["solr-foreground"]
