FROM ghcr.io/navikt/baseimages/temurin:17
COPY target/dp-oppdrag.jar "app.jar"
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

