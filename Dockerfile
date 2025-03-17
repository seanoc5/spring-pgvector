FROM solr:9

# Copy our custom script into the container
COPY scripts/solr-init.sh /scripts/solr-init.sh

# Make it executable
RUN chmod +x /scripts/solr-init.sh

# Use this script as the container's entrypoint
ENTRYPOINT ["/scripts/solr-init.sh"]

# The official Solr image uses "solr-foreground" as CMD by default,
# so we keep that here (but you could override if needed)
CMD ["solr-foreground"]
