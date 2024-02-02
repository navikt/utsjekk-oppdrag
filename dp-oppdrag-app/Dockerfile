FROM ghcr.io/navikt/baseimages/temurin:21

COPY target/dp-oppdrag-app.jar "app.jar"

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"