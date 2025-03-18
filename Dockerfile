
FROM solr:9

# Switch to root so we can create directories, copy files, and chmod
USER root

# (Optional) create a scripts directory if it doesn't already exist
RUN mkdir -p /scripts

# Copy in your script
#COPY scripts/solr-init.sh /scripts/solr-init.sh

# Make it executable
RUN #chmod +x /scripts/solr-init.sh

# Switch back to the 'solr' user to run Solr safely
USER solr

# Set entrypoint to your script
#ENTRYPOINT ["/scripts/solr-init.sh"]

# The default CMD in the official Solr image is "solr-foreground"
CMD ["solr-foreground"]
