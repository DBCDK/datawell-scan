FROM docker.dbc.dk/payara5-micro:latest

LABEL \
  MAX_COUNT="Max number of terms in one request (default: 100)" \
  PARALLEL_HITCOUNT_REQUESTS="Max number of inflight request to verify hitcount (default: 20)" \
  PROFILE_SERVICE_URL="url of profile-service (required)" \
  SOLR_URL="zookeeper/http url of solr collection (required)" \
  USER_AGENT="Name of application for http requests"

COPY target/${project.build.finalName}.war target/docker/app.json deployments/
