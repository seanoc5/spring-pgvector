FROM solr:9.8.1

USER root

# Copy our configuration files
COPY ./contracting/conf /opt/solr/server/solr/configsets/contracting_configs/conf/

# Create an initialization script
COPY ./init-solr.sh /opt/docker-solr/scripts/
RUN chmod +x /opt/docker-solr/scripts/init-solr.sh

USER solr

CMD ["solr-precreate", "contracting", "/opt/solr/server/solr/configsets/contracting_configs"]
