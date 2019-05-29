FROM docker.dbc.dk/payara5-micro:latest

LABEL \
  PROFILE_SERVICE="url of profile-service (required)"

COPY target/${project.build.finalName}.war target/docker/app.json deployments/
