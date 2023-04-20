/*
 * For more details take a look at the 'Building Java & JVM projects' chapter in the Gradle
 * User Manual available at https://docs.gradle.org/7.6/userguide/building_java_projects.html
 */

project.setProperty("mainClassName", "dp.oppdrag.AppKt")

val ktorVersion = "2.2.4"
val micrometerVersion = "1.10.5"
val jacksonVersion = "2.14.2"
val openApiGeneratorVersion = "0.6.1"
val tokenValidationVersion = "3.0.9"
val kotlinLoggerVersion = "3.0.5"
val logbackVersion = "1.4.6"
val logstashVersion = "7.3"
val postgresVersion = "42.6.0"
val hikariVersion = "5.0.1"
val flywayVersion = "9.16.3"
val navTjenesterVersion = "2612.db4dc68"
val navCommonVersion = "3.2023.04.18_10.07-0576b4e09008"
val ibmMqVersion = "9.3.2.0"
val mockOauth2Version = "0.5.8"
val jupiterVersion = "5.9.2"
val testcontainersVersion = "1.18.0"
val mockkVersion = "1.13.4"

plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") version "1.8.20"
    // Apply io.ktor.plugin to build a fat JAR
    id("io.ktor.plugin") version "2.2.4"

    // Apply the application plugin to add support for building a CLI application in Java.
    application
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://build.shibboleth.net/nexus/content/repositories/releases/")
}

dependencies {
    // Ktor
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-default-headers:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.micrometer:micrometer-registry-prometheus:$micrometerVersion")

    // Jackson
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-jaxb-annotations:$jacksonVersion")

    // OpenAPI / Swagger UI
    implementation("dev.forst:ktor-openapi-generator:$openApiGeneratorVersion")

    // Security
    implementation("no.nav.security:token-validation-ktor-v2:$tokenValidationVersion")

    // Log
    implementation("io.github.microutils:kotlin-logging:$kotlinLoggerVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashVersion")

    // DB
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("org.flywaydb:flyway-core:$flywayVersion")

    //
    implementation("com.github.navikt.tjenestespesifikasjoner:nav-virksomhet-oppdragsbehandling-v1-meldingsdefinisjon:$navTjenesterVersion")
    implementation("com.github.navikt.tjenestespesifikasjoner:avstemming-v1-tjenestespesifikasjon:$navTjenesterVersion")
    implementation("com.ibm.mq:com.ibm.mq.allclient:$ibmMqVersion")

    // Simulering
    implementation("com.github.navikt.tjenestespesifikasjoner:nav-system-os-simuler-fp-service-tjenestespesifikasjon:$navTjenesterVersion")
    implementation("com.github.navikt.common-java-modules:cxf:$navCommonVersion")
    implementation("org.opensaml:opensaml-saml-impl:4.2.0")
    implementation("org.opensaml:opensaml-xacml-impl:4.2.0")
    implementation("org.opensaml:opensaml-xacml-saml-impl:4.2.0")
    implementation("org.apache.cxf:cxf-core:4.0.0")
    implementation("org.apache.cxf:cxf-rt-frontend-jaxrs:4.0.0")
    implementation("org.apache.cxf:cxf-rt-transports-http:4.0.0")
    implementation("org.apache.cxf:cxf-rt-ws-security:4.0.0")
    implementation("org.apache.cxf:cxf-rt-features-logging:4.0.0")
    implementation("com.sun.xml.ws:jaxws-ri:4.0.1")
    implementation("javax.xml.bind:jaxb-api:2.3.1")

    // Test
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("no.nav.security:mock-oauth2-server:$mockOauth2Version")
    // Use the Kotlin JUnit 5 integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    // Use the JUnit 5 integration.
    testImplementation("org.junit.jupiter:junit-jupiter-engine:$jupiterVersion")
    // Testcontainers
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
    // MockK
    testImplementation("io.mockk:mockk:$mockkVersion")
}

application {
    // Define the main class for the application.
    mainClass.set(project.property("mainClassName").toString())
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
    // Set environmental variable so as isCurrentlyRunningOnNais returns true
    environment("NAIS_APP_NAME", "test")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = project.property("mainClassName").toString()
    }
}

tasks {
    register("runServerTest", JavaExec::class) {
        environment["AZURE_APP_WELL_KNOWN_URL"] = "https://login.microsoftonline.com/77678b69-1daf-47b6-9072-771d270ac800/v2.0/.well-known/openid-configuration"
        environment["AZURE_APP_CLIENT_ID"] = "test"

        environment["DB_HOST"] = "localhost"
        environment["DB_PORT"] = "5433"
        environment["DB_DATABASE"] = "dp-oppdrag"
        environment["DB_USERNAME"] = "dp-oppdrag-user"
        environment["DB_PASSWORD"] = "dp-oppdrag-password"

        environment["MQ_ENABLED"] = "true"
        environment["MQ_HOSTNAME"] = "localhost"
        environment["MQ_PORT"] = "1414"
        environment["MQ_CHANNEL"] = "DEV.APP.SVRCONN"
        environment["MQ_QUEUEMANAGER"] = "QM1"
        environment["MQ_USER"] = "app"
        environment["MQ_PASSWORD"] = "passw0rd"
        environment["MQ_OPPDRAG_QUEUE"] = "DEV.QUEUE.1"
        environment["MQ_KVITTERING_QUEUE"] = "DEV.QUEUE.2"
        environment["MQ_AVSTEMMING_QUEUE"] = "DEV.QUEUE.3"

        environment["OPPDRAG_SERVICE_URL"] = "https://cics-q1.adeo.no/oppdrag/simulerFpServiceWSBinding"
        environment["STS_URL"] = "https://sts-q1.preprod.local/SecurityTokenServiceProvider/"

        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set(project.property("mainClassName").toString())
    }
}
