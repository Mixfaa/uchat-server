plugins {
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.lombok") version "1.9.21"

    id("io.freefair.lombok") version "8.4"
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "ua.chat_socket"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.0")

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    runtimeOnly("org.postgresql:postgresql")

    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    implementation("io.arrow-kt:arrow-core:1.2.1")
}


tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}
