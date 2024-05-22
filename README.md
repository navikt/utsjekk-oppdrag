# Dette repoet er arkivert

Utsjekk-oppdrag har blitt relansert i ny drakt som en Ktor-applikasjon. Dette repoet blir ikke lenger oppdatert og Spring Boot-applikasjonene kjører ikke lenger. 
Nytt repo finnes [her](https://github.com/navikt/helved-oppdrag). 

## utsjekk-oppdrag

Generell proxy mot Oppdragsystemet (OS) for Utsjekk, tjenesten som iverksetter utbetalinger for dagpenger, AAP, tilleggsstønader og tiltakspenger.

## Bygging

Bygg ved å kjøre `mvn clean install`. 

## Kubernetes secrets

Applikasjonen benytter én egendefinert Kubernetes secret i henholdsvis `dev-gcp` og `prod-gcp`.  
Secret inneholder et passord til IBM MQ. Passordet er representert gjennom miljøvariabelen `MQ_PASSWORD`

## Lokal kjøring

### Kjør lokalt med embedded MQ og database, og automatisk OK-kvittering på MQ
Hvis du vil kjøre opp utsjekk-oppdrag fort og gæli med tom DB og kø, f.eks for debugging.

Kjør `LocalPsqlMqLauncher` under `test`. Da starter app'en opp på port 8087

Følgende miljøvariable må være satt:
* `AZURE_APP_CLIENT_ID`
* `AZURE_APP_CLIENT_SECRET`

De to siste får du tak i slik:
1. Endre kontekst til dev `kubectl config use-context dev-gcp`
2. Finne navn på secret ved å kjøre `kubectl -n helved get secrets` og finne navnet på en secret som starter
   med `azure-utsjekk-oppdrag-`. Kopier navnet på secreten.
3. Kjør `kubectl -n helved get secret [NAVN PÅ SECRET FRA STEG 2] -o json | jq '.data | map_values(@base64d)'`

### Lokal MQ
Det er mulig å installere og kjøre IBM MQ lokalt (ikke i Docker container):  
https://developer.ibm.com/tutorials/mq-connect-app-queue-manager-windows/

For å sjekke køer gjennom IBM MQ Explorer:
Høyreklikk på Queue Managers > Add Remote Queue Manager

Queue Manager: QM1
Host: localhost
Port: 1414
Channel: DEV.ADMIN.SVRCONN
Userid: admin
Password: passw0rd

Husk å endre MQ_-variabler hvis noe ikke er gjort i henhold til instruksjonene i lenken over

### SwaggerUI
Swagger UI: http://localhost:8087/swagger-ui/index.html

## Kontaktinfo
For NAV-interne kan henvendelser om appen rettes til #team-hel-ved på slack. Ellers kan man opprette et issue her på
github.
