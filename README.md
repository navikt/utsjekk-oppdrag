# dagpenger-oppdrag

Generell proxy mot Oppdragsystemet (OS) for dagpenger

## Bygging

Bygg ved å kjøre `./gradlew clean build`. Dette vil også kjøre testene.  
Det er også mulig å kjøre `gradle clean build`, men da må man ha en riktig versjon av gradle installert (som støtter
Java 17)

## Kubernetes secrets

Applikasjonen benytter én egendefinert Kubernetes secret i henholdsvis `dev-gcp` og `prod-gcp`.  
Secret inneholder et passord til IBM MQ. Passordet er representert gjennom miljøvariabelen `MQ_PASSWORD`

## Lokalkjøring

1. Start lokal instans av PostgreSQL database og IBM MQ `docker-compose up -d`
2. Start appen `./gradlew runServerTest`

Swagger UI: http://localhost:8080/internal/swagger-ui/index.html

For å sjekke køer gjennom IBM MQ Explorer:
Høyreklikk på Queue Managers > Add Remote Queue Manager

Queue Manager: QM1
Host: localhost
Port: 1414
Channel: DEV.ADMIN.SVRCONN
Userid: admin
Password: passw0rd

Det er mulig å installere og kjøre IBM MQ lokalt (ikke i Docker container):  
https://developer.ibm.com/tutorials/mq-connect-app-queue-manager-windows/

Husk å endre MQ_-variabler hvis noe ikke er gjort i henhold til instruksjonene i denne lenken

Det er også mulig å slå av MQ-funksjoner i appen (den skal ikke prøve å sende meldinger til IBM MQ).  
I build.gradle.kts endre  
`environment["MQ_ENABLED"] = "true"`  
til  
`environment["MQ_ENABLED"] = "false"`

## Kontaktinfo

For NAV-interne kan henvendelser om appen rettes til #team-dagpenger på slack. Ellers kan man opprette et issue her på
github.
