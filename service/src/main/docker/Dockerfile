FROM docker.dbc.dk/payara5-micro:latest

LABEL \
  CACHE_TIMEOUT="Number of seconds to cache the response from profile-service (required)" \
  CACHE_TIMEOUT_ERROR="Number of seconds to cache an error from profile-service (required)" \
  MAX_COUNT="Max number of terms in one request (default: 100)" \
  PARALLEL_HITCOUNT_REQUESTS="Max number of inflight request to verify hitcount (default: 20)" \
  PROFILE_SERVICE_URL="url of profile-service (required)" \
  SOLR_URL="zookeeper/http url of solr collection (required)" \
  SOLR_APPID="Application ID pass to SOLR, for logging in SOLR (Required)" \
  SYSTEM_NAME="environment in which it runs for hazelcast naming (required)" \
  USER_AGENT="Name of application for http requests"

COPY target/${project.build.finalName}.war target/docker/app.json deployments/
