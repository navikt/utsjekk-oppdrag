FROM ghcr.io/navikt/baseimages/temurin:21

COPY target/utsjekk-oppdrag-simulering.jar "app.jar"

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"