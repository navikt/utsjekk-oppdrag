/*
 * For more details take a look at the 'Building Java & JVM projects' chapter in the Gradle
 * User Manual available at https://docs.gradle.org/7.6/userguide/building_java_projects.html
 */

project.setProperty("mainClassName", "dp.oppdrag.AppKt")

val ktorVersion = "2.2.3"

plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") version "1.8.0"
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
}

dependencies {
    implementation("com.github.navikt.tjenestespesifikasjoner:nav-virksomhet-oppdragsbehandling-v1-meldingsdefinisjon:2612.db4dc68")

    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-default-headers:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("no.nav.security:token-validation-ktor-v2:3.0.0")

    implementation("dev.forst:ktor-openapi-generator:0.6.1")

    implementation("ch.qos.logback:logback-classic:1.4.5")
    implementation("net.logstash.logback:logstash-logback-encoder:7.2")

    // Test
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("no.nav.security:mock-oauth2-server:0.5.8")
    // Use the Kotlin JUnit 5 integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    // Use the JUnit 5 integration.
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.2")
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
        systemProperties["AZURE_APP_WELL_KNOWN_URL"] = "https://login.microsoftonline.com/77678b69-1daf-47b6-9072-771d270ac800/v2.0/.well-known/openid-configuration"
        systemProperties["AZURE_APP_CLIENT_ID"] = "test"

        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set(project.property("mainClassName").toString())
    }
}
