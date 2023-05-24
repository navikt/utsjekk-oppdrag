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

### Kjør lokalt med embedded MQ og database, og automatisk OK-kvittering på MQ
Hvis du vil kjøre opp dp-oppdrag fort og gæli med tom DB og kø, f.eks for debugging.

Kjør `LocalPsqlMqLauncher` under `test`. Da starter app'en opp på port 8087

Følgende miljøvariable må være satt:
* `AZURE_APP_CLIENT_ID`
* `AZURE_APP_CLIENT_SECRET`

De to siste får du tak i slik:
1. Endre kontekst til dev `kubectl config use-context dev-gcp`
2. Finne navn på secret ved å kjøre `kubectl -n teamdagpenger get secrets` og finne navnet på en secret som starter
   med `azure-dp-oppdrag-`. Kopier navnet på secreten.
3. Kjør `kubectl -n teamdagpenger get secret [NAVN PÅ SECRET FRA STEG 2] -o json | jq '.data | map_values(@base64d)'`

### Lokal MQ
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

### SwaggerUI
Swagger UI: http://localhost:8087/internal/swagger-ui/index.html

## Kontaktinfo

For NAV-interne kan henvendelser om appen rettes til #team-dagpenger på slack. Ellers kan man opprette et issue her på
github.
