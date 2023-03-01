FROM ghcr.io/navikt/baseimages/temurin:17

COPY build/libs/dp-oppdrag-all.jar /app/app.jar
EXPOSE 8080
