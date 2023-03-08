# dagpenger-oppdrag
Generell proxy mot Oppdragsystemet (OS) for dagpenger

## Bygging
Bygg ved å kjøre `./gradlew clean build`. Dette vil også kjøre testene.  
Det er også mulig å kjøre `gradle clean build`, men da må man ha en riktig versjon av gradle installert (som støtter Java 17)

## Lokalkjøring
1. Start lokal instans av PostgreSQL database `docker-compose up -d`
2. Start appen `./gradlew runServerTest`

Swagger UI: http://localhost:8080/internal/swagger-ui/index.html

## Kontaktinfo
For NAV-interne kan henvendelser om appen rettes til #team-dagpenger på slack. Ellers kan man opprette et issue her på github.
