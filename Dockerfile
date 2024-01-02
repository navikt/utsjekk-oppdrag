FROM ghcr.io/navikt/baseimages/temurin:17

COPY init.sh /init-scripts/init.sh
COPY target/dp-oppdrag.jar "app.jar"

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"